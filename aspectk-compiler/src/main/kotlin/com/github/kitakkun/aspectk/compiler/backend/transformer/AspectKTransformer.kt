package com.github.kitakkun.aspectk.compiler.backend.transformer

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceKind
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceMetadata
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectMetadata
import com.github.kitakkun.aspectk.compiler.backend.analyzer.Binding
import com.github.kitakkun.aspectk.compiler.backend.matching.PointcutMatcher
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Phase 3.2 advice applier.
 *
 * Walks every `IrSimpleFunction` in the module fragment. For each function that
 * is **not** itself part of an `@Aspect` class, asks [PointcutMatcher] which
 * advices match, and weaves each matching advice into the target's body.
 *
 * - `@Before` advice is **prepended** to the target's body so it runs before
 *   the target's first statement.
 * - `@After` advice wraps the target's body in `try { … } finally { advice() }`
 *   so it runs after both normal returns and exception propagation.
 * - `@Around` is recognised but no-op'd here; it arrives in Phase 3.3.
 *
 * In all cases, binding values (`@DispatchReceiver`, `@ExtensionReceiver`,
 * `@ContextParameter`, `@ValueParameter`) are extracted from the target's
 * parameters and forwarded into the advice's matching slot.
 */
internal class AspectKTransformer(
    private val pluginContext: IrPluginContext,
    private val aspects: List<AspectMetadata>,
) : IrTransformer<Nothing?>() {
    private val aroundWeaver = AroundAdviceWeaver(pluginContext)

    override fun visitElement(
        element: IrElement,
        data: Nothing?,
    ): IrElement {
        element.transformChildren(this, null)
        return element
    }

    override fun visitSimpleFunction(
        declaration: IrSimpleFunction,
        data: Nothing?,
    ): IrSimpleFunction {
        if (declaration.isInsideAspectClass()) return declaration

        for (aspect in aspects) {
            for (advice in aspect.advices) {
                if (!PointcutMatcher.matches(advice, declaration)) continue
                applyAdvice(declaration, aspect.aspectClass, advice)
            }
        }
        return declaration
    }

    private fun IrSimpleFunction.isInsideAspectClass(): Boolean = parentClassOrNull?.hasAnnotation(AspectKAnnotations.ASPECT_FQ_NAME) == true

    private fun applyAdvice(
        target: IrSimpleFunction,
        aspectClass: IrClass,
        advice: AdviceMetadata,
    ) {
        when (advice.kind) {
            AdviceKind.BEFORE -> applyBefore(target, aspectClass, advice)
            AdviceKind.AFTER -> applyAfter(target, aspectClass, advice)
            AdviceKind.AROUND -> aroundWeaver.weave(target, aspectClass, advice)
        }
    }

    private fun applyBefore(
        target: IrSimpleFunction,
        aspectClass: IrClass,
        advice: AdviceMetadata,
    ) {
        val body = target.body as? IrBlockBody ?: return
        val call = buildAdviceCall(target, aspectClass, advice) ?: return
        body.statements.add(0, call)
    }

    private fun applyAfter(
        target: IrSimpleFunction,
        aspectClass: IrClass,
        advice: AdviceMetadata,
    ) {
        val body = target.body as? IrBlockBody ?: return
        val call = buildAdviceCall(target, aspectClass, advice) ?: return

        // Move the current statements into a fresh `try { ... }` block and add
        // the advice invocation as `finally { ... }`. The result type matches
        // the target's return type so the original return value flows through.
        val builder = DeclarationIrBuilder(
            pluginContext,
            target.symbol,
            target.startOffset,
            target.endOffset,
        )
        val originalStatements = body.statements.toList()
        body.statements.clear()
        val tryBlock = builder.irBlock(resultType = target.returnType) {
            for (stmt in originalStatements) +stmt
        }
        val tryExpr = IrTryImpl(
            startOffset = target.startOffset,
            endOffset = target.endOffset,
            type = target.returnType,
        ).apply {
            tryResult = tryBlock
            finallyExpression = call
        }
        body.statements.add(tryExpr)
    }

    /**
     * Builds an `IrCall` invoking [advice]. The advice's dispatch receiver is:
     *
     * - For an ``object`` aspect: the singleton `INSTANCE`, read via
     *   `IrGetObjectValue`. Each weave site shares the same instance.
     * - For a regular ``class`` aspect with a no-arg primary constructor: a
     *   freshly constructed instance per weave site (the v0.x behaviour). User
     *   opts into caching by declaring the aspect as ``object``.
     *
     * Returns `null` if neither shape applies (e.g. ``class`` with required
     * constructor parameters).
     */
    private fun buildAdviceCall(
        target: IrSimpleFunction,
        aspectClass: IrClass,
        advice: AdviceMetadata,
    ): IrCall? {
        val adviceFn = advice.function
        val builder = DeclarationIrBuilder(
            pluginContext,
            target.symbol,
            target.startOffset,
            target.endOffset,
        )
        val aspectInstance = aspectInstance(aspectClass, builder) ?: return null
        return builder.irCall(adviceFn.symbol).apply {
            // arguments[0] is the advice's dispatch receiver (the aspect instance).
            arguments[0] = aspectInstance
            for (binding in advice.bindings) {
                val adviceSlotIndex = adviceFn.parameters.indexOf(binding.adviceParameter)
                if (adviceSlotIndex < 0) continue
                val value = extractBindingValue(binding, target, builder) ?: continue
                arguments[adviceSlotIndex] = value
            }
        }
    }

    private fun aspectInstance(
        aspectClass: IrClass,
        builder: DeclarationIrBuilder,
    ): IrExpression? = AspectInstanceSupport.instanceExpression(aspectClass, builder)

    private fun extractBindingValue(
        binding: Binding,
        target: IrSimpleFunction,
        builder: DeclarationIrBuilder,
    ): IrExpression? {
        return when (binding) {
            is Binding.DispatchReceiver -> target.parameters
                .firstOrNull { it.kind == IrParameterKind.DispatchReceiver }
                ?.let(builder::irGet)
            is Binding.ExtensionReceiver -> target.parameters
                .firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
                ?.let(builder::irGet)
            is Binding.ValueParameter -> findRegular(target, binding.index, binding.name)
                ?.let(builder::irGet)
            is Binding.ContextParameter -> findContext(target, binding.index, binding.name)
                ?.let(builder::irGet)
            is Binding.ValueParameters -> buildAnyNullableList(
                target.parameters.filter { it.kind == IrParameterKind.Regular },
                builder,
            )
            is Binding.ContextParameters -> buildAnyNullableList(
                target.parameters.filter { it.kind == IrParameterKind.Context },
                builder,
            )
        }
    }

    /**
     * Builds a `listOf<Any?>(p0, p1, …)` expression that materialises [params]
     * (as `IrGetValue` reads) into a `List<Any?>` at the woven call site.
     * Returns `null` if `kotlin.collections.listOf` can't be resolved.
     */
    private fun buildAnyNullableList(
        params: List<IrValueParameter>,
        builder: DeclarationIrBuilder,
    ): IrExpression? {
        val anyNType = pluginContext.irBuiltIns.anyNType
        val listOfSymbol = pluginContext.referenceFunctions(
            org.jetbrains.kotlin.name.CallableId(
                org.jetbrains.kotlin.name.FqName("kotlin.collections"),
                org.jetbrains.kotlin.name.Name.identifier("listOf"),
            ),
        ).firstOrNull { fn ->
            val regular = fn.owner.parameters.filter { it.kind == IrParameterKind.Regular }
            regular.size == 1 && regular[0].varargElementType != null
        } ?: return null

        val arrayType = pluginContext.irBuiltIns.arrayClass.typeWith(anyNType)
        val vararg = org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl(
            startOffset = builder.startOffset,
            endOffset = builder.endOffset,
            type = arrayType,
            varargElementType = anyNType,
            elements = params.map { builder.irGet(it) },
        )
        return builder.irCall(listOfSymbol).apply {
            typeArguments[0] = anyNType
            arguments[0] = vararg
        }
    }

    private fun findRegular(
        target: IrSimpleFunction,
        index: Int?,
        name: String?,
    ): IrValueParameter? {
        val regulars = target.parameters.filter { it.kind == IrParameterKind.Regular }
        return when {
            index != null -> regulars.getOrNull(index)
            name != null -> regulars.firstOrNull { it.name.asString() == name }
            else -> null
        }
    }

    private fun findContext(
        target: IrSimpleFunction,
        index: Int?,
        name: String?,
    ): IrValueParameter? {
        val contexts = target.parameters.filter { it.kind == IrParameterKind.Context }
        return when {
            index != null -> contexts.getOrNull(index)
            name != null -> contexts.firstOrNull { it.name.asString() == name }
            else -> null
        }
    }
}
