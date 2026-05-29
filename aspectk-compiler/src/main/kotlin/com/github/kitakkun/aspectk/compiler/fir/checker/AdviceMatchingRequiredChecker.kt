package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.ClassId

/**
 * An advice function (`@Before` / `@After` / `@Around`) must declare at least
 * one matching annotation that bounds the set of targets it intercepts. With
 * none, the advice would either match nothing (silently) or every function
 * AspectK sees, depending on how the matcher treats missing pointcuts —
 * neither is what the user likely wants.
 *
 * Matching annotations are `@Package`, `@ClassName`, `@MethodName`,
 * `@Visibility`, `@Modality`, `@Modifiers`, `@Annotated`.
 *
 * This checker complements [PointcutAnnotationChecker]'s pattern-string
 * validation: that checker rejects empty patterns on individual annotations;
 * this one rejects the case where no matching annotation is present at all.
 */
object AdviceMatchingRequiredChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    private val MATCHING_ANNOTATIONS: List<ClassId> = listOf(
        AspectKAnnotations.PACKAGE_CLASS_ID,
        AspectKAnnotations.CLASS_NAME_CLASS_ID,
        AspectKAnnotations.METHOD_NAME_CLASS_ID,
        AspectKAnnotations.VISIBILITY_CLASS_ID,
        AspectKAnnotations.MODALITY_CLASS_ID,
        AspectKAnnotations.MODIFIERS_CLASS_ID,
        AspectKAnnotations.ANNOTATED_CLASS_ID,
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        val adviceKind = adviceKindOn(declaration) ?: return
        if (MATCHING_ANNOTATIONS.any { declaration.hasAnnotation(it, context.session) }) return

        val src = declaration.source ?: return
        reporter.reportOn(
            src,
            AspectKErrors.ADVICE_HAS_NO_MATCHING_ANNOTATION,
            adviceKind,
        )
    }

    context(context: CheckerContext)
    private fun adviceKindOn(declaration: FirNamedFunction): String? =
        when {
            declaration.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, context.session) -> "Before"
            declaration.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, context.session) -> "After"
            declaration.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, context.session) -> "Around"
            else -> null
        }
}
