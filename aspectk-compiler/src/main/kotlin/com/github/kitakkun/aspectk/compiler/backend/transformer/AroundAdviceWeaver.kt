package com.github.kitakkun.aspectk.compiler.backend.transformer

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceMetadata
import com.github.kitakkun.aspectk.compiler.backend.analyzer.Binding
import com.github.kitakkun.aspectk.compiler.fir.checker.AspectKErrors
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Phase 3.3 `@Around` advice weaver.
 *
 * For each `(target, around-advice)` match, this weaver:
 *
 * 1. Extracts the lambda body from the advice's `interceptableAdvice<R> { ... }`
 *    invocation.
 * 2. Declares one mutable local variable per advice binding (`__<slotName>`),
 *    each initialised from the target's matching parameter slot.
 * 3. Deep-clones the lambda body so each target site gets an independent copy.
 * 4. Inside the cloned body, rewrites:
 *    - `IrGetValue` reads of the advice binding parameters into reads of the
 *      corresponding local variable.
 *    - `AroundScope.replace*(…, value)` calls into `IrSetValue` assignments
 *      against the local variable for the targeted slot.
 *    - `AroundScope.proceed()` calls into a fresh clone of the target's
 *      original return-value expression, with reads of the target's
 *      parameter slots remapped to reads of the local variables (so the
 *      current override state flows into the call).
 *    - `IrReturn`s that targeted the lambda into `IrReturn`s targeting the
 *      target function (so the value escapes both the lambda and the target).
 * 5. Replaces the target's body with the substituted block.
 *
 * Constraints carried by this minimum-viable cut:
 *
 * - The target's body must be a single `return <expr>` (`IrBlockBody` whose
 *   sole statement is an `IrReturn`). Multi-statement bodies and
 *   `IrExpressionBody` are deferred.
 * - The aspect class must have a no-arg primary constructor.
 * - `replaceValueParameter` / `replaceContextParameter` need a *constant*
 *   first argument (index `IrConst<Int>` or name `IrConst<String>`); a
 *   non-const slot identifier is left untransformed and would throw at
 *   runtime via the `aspectk-core` stub.
 * - The bind-first rule (replace targets a slot that must also be bound) is
 *   enforced indirectly: only slots with declared bindings get a local
 *   variable, so an unbound `replace*` call has no local to target and is
 *   silently left as the runtime-throwing stub call. A dedicated FIR
 *   checker arrives in a follow-up.
 * - Combining `@Around` with `@Before` / `@After` on the same target is
 *   undefined — `@Around` replaces the body.
 *
 * The advice function itself is left untouched — the lambda body is cloned
 * out of it and used as a template.
 */
internal class AroundAdviceWeaver(
    private val pluginContext: IrPluginContext,
) {
    fun weave(
        target: IrSimpleFunction,
        aspectClass: IrClass,
        advice: AdviceMetadata,
    ): Boolean {
        if (!canInstantiateAspect(aspectClass)) return false
        // Catch-all bindings (`@ValueParameters` / `@ContextParameters`) require
        // synthesising a `listOf<Any?>(…)` at the call site; integrating that
        // with the around weaver's local-var substitution path isn't done yet.
        val catchAll = advice.bindings.firstOrNull {
            it is Binding.ValueParameters || it is Binding.ContextParameters
        }
        if (catchAll != null) {
            val annotationName = when (catchAll) {
                is Binding.ValueParameters -> "@ValueParameters"
                is Binding.ContextParameters -> "@ContextParameters"
                else -> "@?"
            }
            pluginContext.diagnosticReporter
                .at(advice.function)
                .report(
                    AspectKErrors.AROUND_UNSUPPORTED_BINDING,
                    "$annotationName binding is not yet supported by the @Around weaver",
                )
            return false
        }

        val adviceFn = advice.function
        val lambdaFn = findAdviceLambda(adviceFn) ?: return false
        val adviceLambdaBody = lambdaFn.body as? IrBlockBody ?: return false

        val originalReturnValue = singleReturnValue(target) ?: return false

        val builder = DeclarationIrBuilder(
            pluginContext,
            target.symbol,
            target.startOffset,
            target.endOffset,
        )

        target.body = builder.irBlockBody {
            // 1. Declare one mutable local per bound slot, initialised from
            //    the matching target parameter. `irTemporary` is an
            //    `IrStatementsBuilder` extension, so locals are appended to
            //    this block in declaration order.
            val slotToLocal = linkedMapOf<IrValueParameter, IrVariable>()
            for (binding in advice.bindings) {
                val slot = findTargetSlot(binding, target) ?: continue
                slotToLocal.getOrPut(slot) {
                    irTemporary(
                        value = irGet(slot),
                        nameHint = "__${slot.name.asString()}",
                        isMutable = true,
                    )
                }
            }

            // 2. Map each advice binding parameter to its local.
            val paramToLocal = mutableMapOf<IrValueParameter, IrVariable>()
            for (binding in advice.bindings) {
                val slot = findTargetSlot(binding, target) ?: continue
                val local = slotToLocal[slot] ?: continue
                paramToLocal[binding.adviceParameter] = local
            }

            // 3. Clone the advice's lambda body and rewrite parameter reads,
            //    replace* calls, proceed() calls, and lambda-targeted returns.
            val clonedBody = adviceLambdaBody.deepCopyWithSymbols(initialParent = target)
            val substitution = AroundSubstitutionTransformer(
                builder = builder,
                paramToLocal = paramToLocal,
                slotToLocal = slotToLocal,
                target = target,
                originalReturnValue = originalReturnValue,
                lambdaFunction = lambdaFn,
            )
            clonedBody.transformChildren(substitution, null)

            for (stmt in clonedBody.statements) +stmt
        }
        return true
    }

    private fun canInstantiateAspect(aspectClass: IrClass): Boolean =
        AspectInstanceSupport.canInstantiate(aspectClass)

    private fun findAdviceLambda(adviceFn: IrSimpleFunction): IrFunction? {
        val body = adviceFn.body as? IrBlockBody ?: return null
        val ret = body.statements.singleOrNull() as? IrReturn ?: return null
        val call = ret.value as? IrCall ?: return null
        val callee = call.symbol.owner
        if (callee.parentClassOrNull != null) return null
        if (callee.kotlinFqName != AspectKAnnotations.INTERCEPTABLE_ADVICE_FQ_NAME) return null
        val lambdaArg = call.arguments.firstOrNull { it is IrFunctionExpression } as? IrFunctionExpression
            ?: return null
        return lambdaArg.function
    }

    private fun singleReturnValue(target: IrSimpleFunction): IrExpression? {
        val body = target.body as? IrBlockBody ?: return null
        val ret = body.statements.singleOrNull() as? IrReturn ?: return null
        return ret.value
    }

    private fun findTargetSlot(
        binding: Binding,
        target: IrSimpleFunction,
    ): IrValueParameter? =
        when (binding) {
            is Binding.DispatchReceiver ->
                target.parameters.firstOrNull { it.kind == IrParameterKind.DispatchReceiver }
            is Binding.ExtensionReceiver ->
                target.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
            is Binding.ValueParameter -> {
                val regulars = target.parameters.filter { it.kind == IrParameterKind.Regular }
                when {
                    binding.index != null -> regulars.getOrNull(binding.index)
                    binding.name != null -> regulars.firstOrNull { it.name.asString() == binding.name }
                    else -> null
                }
            }
            is Binding.ContextParameter -> {
                val contexts = target.parameters.filter { it.kind == IrParameterKind.Context }
                when {
                    binding.index != null -> contexts.getOrNull(binding.index)
                    binding.name != null -> contexts.firstOrNull { it.name.asString() == binding.name }
                    else -> null
                }
            }
            // Catch-all bindings don't map to a single target slot. They are
            // intentionally not supported inside `@Around` advice in this PR —
            // returning null leaves them out of the substitution map, and the
            // weaver bails for any advice that declares one.
            is Binding.ValueParameters, is Binding.ContextParameters -> null
        }
}

private class AroundSubstitutionTransformer(
    private val builder: DeclarationIrBuilder,
    private val paramToLocal: Map<IrValueParameter, IrVariable>,
    private val slotToLocal: Map<IrValueParameter, IrVariable>,
    private val target: IrSimpleFunction,
    private val originalReturnValue: IrExpression,
    private val lambdaFunction: IrFunction,
) : IrTransformer<Nothing?>() {
    override fun visitElement(
        element: IrElement,
        data: Nothing?,
    ): IrElement {
        element.transformChildren(this, null)
        return element
    }

    override fun visitGetValue(
        expression: IrGetValue,
        data: Nothing?,
    ): IrExpression {
        val local = paramToLocal[expression.symbol.owner]
        return if (local != null) builder.irGet(local) else expression
    }

    override fun visitCall(
        expression: IrCall,
        data: Nothing?,
    ): IrElement {
        if (isProceedCall(expression)) return buildProceedReplacement()
        replaceCallTargetSlot(expression)?.let { slot ->
            val local = slotToLocal[slot] ?: return super.visitCall(expression, data)
            val valueArg = replaceCallValue(expression)
                ?.transform(this, null) as? IrExpression
                ?: return super.visitCall(expression, data)
            return builder.irSet(local.symbol, valueArg)
        }
        return super.visitCall(expression, data)
    }

    override fun visitReturn(
        expression: IrReturn,
        data: Nothing?,
    ): IrExpression {
        if (expression.returnTargetSymbol == lambdaFunction.symbol) {
            val newValue = expression.value.transform(this, null)
            return builder.irReturn(newValue)
        }
        return super.visitReturn(expression, data)
    }

    /**
     * Builds a fresh clone of the target's original return-value expression
     * with target-parameter reads rebound to the matching local variables,
     * so any `replace*`-induced override state flows in.
     */
    private fun buildProceedReplacement(): IrExpression {
        val clone = originalReturnValue.deepCopyWithSymbols(initialParent = target)
        clone.transformChildren(
            SlotRefToLocalTransformer(builder, slotToLocal),
            null,
        )
        return clone
    }

    private fun isProceedCall(call: IrCall): Boolean {
        val callee = call.symbol.owner
        if (callee.name != AspectKAnnotations.PROCEED_NAME) return false
        val owner = callee.parentClassOrNull ?: return false
        return owner.kotlinFqName == AspectKAnnotations.AROUND_SCOPE_FQ_NAME
    }

    /**
     * If [call] is a recognised `AroundScope.replace*` invocation with a
     * constant slot identifier that resolves to a target parameter, returns
     * that target parameter. Returns `null` otherwise.
     *
     * Caveat: returning null here can mean either "not a `replace*` call at
     * all" or "a `replace*` call whose slot key isn't a compile-time
     * constant". The visitCall caller treats both as fall-through; the
     * latter case leaves the original `AroundScope.replace*` call in place,
     * which would crash at runtime via the `aspectk-core` stub. A dedicated
     * UNRESOLVED_REPLACE_SLOT diagnostic is tracked under task #26 (FIR
     * pointcut-completeness check), where the necessary diagnostic-factory
     * plumbing already lives.
     */
    private fun replaceCallTargetSlot(call: IrCall): IrValueParameter? {
        val callee = call.symbol.owner
        val owner = callee.parentClassOrNull ?: return null
        if (owner.kotlinFqName != AspectKAnnotations.AROUND_SCOPE_FQ_NAME) return null
        return when (callee.name.asString()) {
            "replaceDispatchReceiver" ->
                target.parameters.firstOrNull { it.kind == IrParameterKind.DispatchReceiver }
            "replaceExtensionReceiver" ->
                target.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
            "replaceValueParameter" ->
                resolveSlotByConstArg(call, target.parameters.filter { it.kind == IrParameterKind.Regular })
            "replaceContextParameter" ->
                resolveSlotByConstArg(call, target.parameters.filter { it.kind == IrParameterKind.Context })
            else -> null
        }
    }

    private fun resolveSlotByConstArg(
        call: IrCall,
        candidates: List<IrValueParameter>,
    ): IrValueParameter? {
        // arguments layout: [dispatchReceiver, slotKey, value]
        val keyArg = call.arguments.getOrNull(1) as? IrConst ?: return null
        return when (val key = keyArg.value) {
            is Int -> candidates.getOrNull(key)
            is String -> candidates.firstOrNull { it.name.asString() == key }
            else -> null
        }
    }

    private fun replaceCallValue(call: IrCall): IrExpression? = call.arguments.getOrNull(2)
}

/**
 * Rewrites `IrGetValue` reads of target parameter slots into reads of their
 * corresponding local variables. Used inside the cloned original return-value
 * expression so `proceed()` picks up the override state.
 */
private class SlotRefToLocalTransformer(
    private val builder: DeclarationIrBuilder,
    private val slotToLocal: Map<IrValueParameter, IrVariable>,
) : IrTransformer<Nothing?>() {
    override fun visitElement(
        element: IrElement,
        data: Nothing?,
    ): IrElement {
        element.transformChildren(this, null)
        return element
    }

    override fun visitGetValue(
        expression: IrGetValue,
        data: Nothing?,
    ): IrExpression {
        val local = slotToLocal[expression.symbol.owner]
        return if (local != null) builder.irGet(local) else expression
    }
}
