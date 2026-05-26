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
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Phase 3.1 advice applier.
 *
 * Walks every `IrSimpleFunction` in the module fragment. For each function that
 * is **not** itself part of an `@Aspect` class, asks [PointcutMatcher] which
 * advices match, and prepends each matching advice's invocation to the target's
 * body. Binding values (`@DispatchReceiver`, `@ExtensionReceiver`,
 * `@ContextParameter`, `@ValueParameter`) are extracted from the target's
 * parameters and forwarded into the advice's matching slot.
 *
 * Only `@Before` advice is wired up here; `@After` / `@Around` are recognised
 * but no-op'd and arrive in Phase 3.2 / 3.3.
 */
internal class AspectKTransformer(
    private val pluginContext: IrPluginContext,
    private val aspects: List<AspectMetadata>,
) : IrTransformer<Nothing?>() {
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
            AdviceKind.AFTER -> Unit // Phase 3.2
            AdviceKind.AROUND -> Unit // Phase 3.3
        }
    }

    private fun applyBefore(
        target: IrSimpleFunction,
        aspectClass: IrClass,
        advice: AdviceMetadata,
    ) {
        val body = target.body as? IrBlockBody ?: return
        val ctor = aspectClass.primaryConstructor
            ?: return // No primary constructor — nothing safe to do
        if (ctor.parameters.isNotEmpty()) return

        val adviceFn = advice.function
        val builder = DeclarationIrBuilder(
            pluginContext,
            target.symbol,
            target.startOffset,
            target.endOffset,
        )
        val call = builder.irCall(adviceFn.symbol).apply {
            // arguments[0] is the advice's dispatch receiver (the aspect instance).
            arguments[0] = builder.irCallConstructor(ctor.symbol, emptyList())
            // Forward each binding's extracted value into the matching advice slot.
            for (binding in advice.bindings) {
                val adviceSlotIndex = adviceFn.parameters.indexOf(binding.adviceParameter)
                if (adviceSlotIndex < 0) continue
                val value = extractBindingValue(binding, target, builder) ?: continue
                arguments[adviceSlotIndex] = value
            }
        }

        body.statements.add(0, call)
    }

    private fun extractBindingValue(
        binding: Binding,
        target: IrSimpleFunction,
        builder: DeclarationIrBuilder,
    ): IrExpression? {
        val sourceParam = when (binding) {
            is Binding.DispatchReceiver -> target.parameters.firstOrNull { it.kind == IrParameterKind.DispatchReceiver }
            is Binding.ExtensionReceiver -> target.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
            is Binding.ValueParameter -> findRegular(target, binding.index, binding.name)
            is Binding.ContextParameter -> findContext(target, binding.index, binding.name)
        } ?: return null
        return builder.irGet(sourceParam)
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
