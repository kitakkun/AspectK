package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation

class AspectClassChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        with(context) {
            verifyContainingPointcutOrAdviceEntries(declaration, reporter)
        }
    }

    context(CheckerContext)
    private fun verifyContainingPointcutOrAdviceEntries(declaration: FirClass, reporter: DiagnosticReporter) {
        val isAspectClass = declaration.hasAnnotation(AspectKAnnotations.ASPECT_CLASS_ID, session)

        if (!isAspectClass) return

        val hasPointcutOrAdviceEntries = declaration.declarations.any {
            it.hasAnnotation(AspectKAnnotations.POINTCUT_CLASS_ID, session) ||
                it.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, session) ||
                it.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, session) ||
                it.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, session)
        }

        if (hasPointcutOrAdviceEntries) return

        reporter.reportOn(declaration.source, AspectKErrors.ASPECT_CLASS_WITH_NO_POINTCUT_OR_ADVICE_ENTRIES)
    }
}
