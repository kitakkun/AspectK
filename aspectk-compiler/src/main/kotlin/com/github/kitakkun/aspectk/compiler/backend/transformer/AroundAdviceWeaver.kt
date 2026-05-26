package com.github.kitakkun.aspectk.compiler.backend.transformer

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceMetadata
import com.github.kitakkun.aspectk.compiler.backend.analyzer.Binding
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Phase 3.3a `@Around` advice weaver (minimum-viable scope).
 *
 * For each `(target, around-advice)` match, this weaver:
 *
 * 1. Extracts the lambda body from the advice's `interceptableAdvice<R> { ... }`
 *    invocation.
 * 2. Deep-clones the lambda body so each target site gets an independent copy.
 * 3. Inside the cloned body, rewrites:
 *    - References to advice binding parameters into `IrGetValue` reads of the
 *      target's matching slot.
 *    - Calls to `AroundScope.proceed()` into a fresh clone of the target's
 *      original return-value expression.
 *    - `IrReturn`s that targeted the lambda into `IrReturn`s targeting the
 *      target function (so the value escapes both the lambda and the target).
 * 4. Replaces the target's body with the substituted block.
 *
 * Constraints carried by this minimum-viable cut:
 *
 * - The target's body must be a single `return <expr>` (`IrBlockBody` whose
 *   sole statement is an `IrReturn`). Multi-statement bodies and
 *   `IrExpressionBody` are deferred.
 * - The aspect class must have a no-arg primary constructor.
 * - The `replace*` family on `AroundScope` is left untransformed; calling any
 *   of them at runtime throws via the stub in `aspectk-core`. Phase 3.3b
 *   wires them up.
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
        val ctor = aspectClass.primaryConstructor ?: return false
        if (ctor.parameters.isNotEmpty()) return false

        val adviceFn = advice.function
        val lambdaFn = findAdviceLambda(adviceFn) ?: return false
        val adviceLambdaBody = lambdaFn.body as? IrBlockBody ?: return false

        val originalReturnValue = singleReturnValue(target) ?: return false

        val paramSubst = buildParameterSubstitution(advice.bindings, target)

        val clonedBody = adviceLambdaBody.deepCopyWithSymbols(initialParent = target)
        val builder = DeclarationIrBuilder(
            pluginContext,
            target.symbol,
            target.startOffset,
            target.endOffset,
        )
        val substitution = AroundSubstitutionTransformer(
            builder = builder,
            paramSubst = paramSubst,
            proceedReplacement = { originalReturnValue.deepCopyWithSymbols(initialParent = target) },
            lambdaFunction = lambdaFn,
            target = target,
        )
        clonedBody.transformChildren(substitution, null)

        target.body = builder.irBlockBody {
            for (stmt in clonedBody.statements) +stmt
        }
        return true
    }

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

    private fun buildParameterSubstitution(
        bindings: List<Binding>,
        target: IrSimpleFunction,
    ): Map<IrValueParameter, IrValueParameter> {
        val map = mutableMapOf<IrValueParameter, IrValueParameter>()
        for (binding in bindings) {
            val targetSlot = findTargetSlot(binding, target) ?: continue
            map[binding.adviceParameter] = targetSlot
        }
        return map
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
        }
}

private class AroundSubstitutionTransformer(
    private val builder: DeclarationIrBuilder,
    private val paramSubst: Map<IrValueParameter, IrValueParameter>,
    private val proceedReplacement: () -> IrExpression,
    private val lambdaFunction: IrFunction,
    private val target: IrSimpleFunction,
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
        val targetSlot = paramSubst[expression.symbol.owner]
        return if (targetSlot != null) builder.irGet(targetSlot) else expression
    }

    override fun visitCall(
        expression: IrCall,
        data: Nothing?,
    ): IrElement {
        if (isProceedCall(expression)) return proceedReplacement()
        return super.visitCall(expression, data)
    }

    override fun visitReturn(
        expression: IrReturn,
        data: Nothing?,
    ): IrExpression {
        // Returns that target the original lambda are retargeted to the target
        // function so their value escapes both the lambda and the target.
        if (expression.returnTargetSymbol == lambdaFunction.symbol) {
            val newValue = expression.value.transform(this, null)
            return builder.irReturn(newValue)
        }
        return super.visitReturn(expression, data)
    }

    private fun isProceedCall(call: IrCall): Boolean {
        val callee = call.symbol.owner
        if (callee.name != AspectKAnnotations.PROCEED_NAME) return false
        val owner = callee.parentClassOrNull ?: return false
        return owner.kotlinFqName == AspectKAnnotations.AROUND_SCOPE_FQ_NAME
    }
}
