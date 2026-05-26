package com.github.kitakkun.aspectk.compiler.backend.analyzer

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Walks the IR module fragment, finds every `@Aspect` class and the advice
 * functions it declares, and converts each advice's stacked pointcut annotations
 * into a [PointcutFilter].
 *
 * Phase 3 minimum scope: only `@ClassName` / `@MethodName` annotations are read.
 * Other v1 pointcut annotations (`@Visibility`, `@Package`, `@Annotated`, …) are
 * silently ignored here and will be added in Phase 3 follow-ups.
 */
internal class AspectAnalyzer {
    fun analyze(moduleFragment: IrModuleFragment): List<AspectMetadata> {
        val aspects = mutableListOf<AspectMetadata>()
        moduleFragment.acceptVoid(
            object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildren(this, null)
                }

                override fun visitClass(declaration: IrClass) {
                    if (declaration.hasAnnotation(AspectKAnnotations.ASPECT_FQ_NAME)) {
                        aspects.add(buildAspectMetadata(declaration))
                    }
                    declaration.acceptChildren(this, null)
                }
            },
        )
        return aspects
    }

    private fun buildAspectMetadata(aspectClass: IrClass): AspectMetadata {
        val advices = aspectClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .mapNotNull { fn ->
                val kind = adviceKindOf(fn) ?: return@mapNotNull null
                AdviceMetadata(
                    function = fn,
                    kind = kind,
                    pointcut = pointcutOf(fn),
                )
            }
        return AspectMetadata(aspectClass = aspectClass, advices = advices)
    }

    private fun adviceKindOf(fn: IrSimpleFunction): AdviceKind? =
        when {
            fn.hasAnnotation(AspectKAnnotations.BEFORE_FQ_NAME) -> AdviceKind.BEFORE
            fn.hasAnnotation(AspectKAnnotations.AFTER_FQ_NAME) -> AdviceKind.AFTER
            fn.hasAnnotation(AspectKAnnotations.AROUND_FQ_NAME) -> AdviceKind.AROUND
            else -> null
        }

    private fun pointcutOf(fn: IrSimpleFunction): PointcutFilter {
        val className = fn
            .getAnnotation(AspectKAnnotations.CLASS_NAME_CLASS_ID.asSingleFqName())
            ?.getStringConstArgument(0)
        val methodName = fn
            .getAnnotation(AspectKAnnotations.METHOD_NAME_CLASS_ID.asSingleFqName())
            ?.getStringConstArgument(0)
        return PointcutFilter(
            classNamePattern = className,
            methodNamePattern = methodName,
        )
    }
}

private fun IrElement.acceptVoid(visitor: IrVisitorVoid) {
    accept(visitor, null)
}
