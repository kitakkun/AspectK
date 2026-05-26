package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId

/**
 * Errors when `@Before` / `@After` / `@Around` is applied to a function that is
 * not a member of an `@Aspect`-annotated class.
 */
object AdviceScopeChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        val adviceClassId = adviceClassIdOn(declaration) ?: return
        if (declaration.isInsideAspectClass()) return

        val src = declaration.source ?: return
        reporter.reportOn(
            src,
            AspectKErrors.ADVICE_OUTSIDE_ASPECT_CLASS,
            adviceClassId.shortClassName.asString(),
        )
    }

    context(context: CheckerContext)
    private fun adviceClassIdOn(declaration: FirNamedFunction): ClassId? {
        for (classId in AspectKAnnotations.ADVICE_CLASS_IDS) {
            if (declaration.hasAnnotation(classId, context.session)) return classId
        }
        return null
    }

    context(context: CheckerContext)
    private fun FirNamedFunction.isInsideAspectClass(): Boolean {
        val classId = dispatchReceiverType?.classId ?: return false
        val classSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
            as? FirRegularClassSymbol
            ?: return false
        return classSymbol.hasAnnotation(AspectKAnnotations.ASPECT_CLASS_ID, context.session)
    }
}
