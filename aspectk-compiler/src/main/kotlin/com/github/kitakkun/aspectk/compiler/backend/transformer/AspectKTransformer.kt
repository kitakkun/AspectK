package com.github.kitakkun.aspectk.compiler.backend.transformer

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceKind
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceMetadata
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectMetadata
import com.github.kitakkun.aspectk.compiler.backend.matching.PointcutMatcher
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Phase 3 (minimum-viable) advice applier.
 *
 * Walks every `IrSimpleFunction` in the module fragment. For each function that
 * is **not** itself part of an `@Aspect` class, the transformer checks every
 * advice's [PointcutMatcher] and — when matched — prepends an advice invocation
 * to the target's body.
 *
 * Currently only `@Before` advice is wired up; `@After` / `@Around` are no-ops
 * and arrive in Phase 3 follow-up PRs. Advice functions are expected to be
 * parameterless (no `JoinPoint` injection in this PR).
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
                if (!PointcutMatcher.matches(advice.pointcut, declaration)) continue
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
            AdviceKind.BEFORE -> applyBefore(target, aspectClass, advice.function)
            AdviceKind.AFTER -> Unit // Phase 3 follow-up
            AdviceKind.AROUND -> Unit // Phase 3 follow-up
        }
    }

    private fun applyBefore(
        target: IrSimpleFunction,
        aspectClass: IrClass,
        adviceFn: IrSimpleFunction,
    ) {
        val body = target.body as? IrBlockBody ?: return
        val ctor = aspectClass.primaryConstructor
            ?: return // No no-arg primary constructor — nothing safe to do
        if (ctor.parameters.isNotEmpty()) return

        val builder = DeclarationIrBuilder(
            pluginContext,
            target.symbol,
            target.startOffset,
            target.endOffset,
        )
        // Construct a fresh aspect instance inline at each call site (no caching).
        // arguments[0] of the advice call is the dispatch receiver slot.
        val call = builder.irCall(adviceFn.symbol).apply {
            arguments[0] = builder.irCallConstructor(ctor.symbol, emptyList())
        }

        body.statements.add(0, call)
    }
}
