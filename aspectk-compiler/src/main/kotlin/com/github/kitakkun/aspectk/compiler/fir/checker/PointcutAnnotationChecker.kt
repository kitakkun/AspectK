package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.ClassId

/**
 * Validates the single `pattern: String` parameter on the v1 pointcut
 * annotations `@Package` / `@ClassName` / `@MethodName`. An empty pattern
 * is rejected at FIR time so the user gets a clear error rather than a
 * silently-non-matching advice at runtime.
 */
object PointcutAnnotationChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    private val annotationsToValidate = listOf(
        AspectKAnnotations.PACKAGE_CLASS_ID to "Package",
        AspectKAnnotations.CLASS_NAME_CLASS_ID to "ClassName",
        AspectKAnnotations.METHOD_NAME_CLASS_ID to "MethodName",
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        for ((classId, displayName) in annotationsToValidate) {
            validatePatternNonEmpty(declaration, classId, displayName)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun validatePatternNonEmpty(
        declaration: FirNamedFunction,
        annotationClassId: ClassId,
        annotationDisplayName: String,
    ) {
        val annotation: FirAnnotation =
            declaration.getAnnotationByClassId(annotationClassId, context.session) ?: return
        val pattern = annotation.getStringArgument(AspectKAnnotations.PATTERN, context.session)
        if (!pattern.isNullOrEmpty()) return

        val src = annotation.source ?: declaration.source ?: return
        reporter.reportOn(
            src,
            AspectKErrors.INVALID_POINTCUT_ANNOTATION,
            annotationDisplayName,
            "pattern must not be empty",
        )
    }
}
