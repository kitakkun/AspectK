package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.backend.matching.NamePattern
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Emits a weak [AspectKErrors.WOVEN_CALL_SITE] diagnostic at each function
 * call whose callee is matched by at least one advice in the current session.
 *
 * Phase 3.5 over-approximates: only `@ClassName` / `@MethodName` patterns
 * are honoured at FIR time. IR remains the authoritative weaver; an
 * over-fired marker is acceptable for editor feedback.
 *
 * Aspect discovery uses the FIR predicate system (`LookupPredicate.annotated
 * (@Aspect)`); the registrar registers the predicate so the session-wide
 * annotation index includes `@Aspect`.
 *
 * Calls whose callee lives inside an `@Aspect` class are skipped — the IR
 * weaver never targets those either.
 */
object WovenCallSiteChecker : FirExpressionChecker<FirFunctionCall>(MppCheckerKind.Common) {
    private val ASPECT_LOOKUP: LookupPredicate = LookupPredicate.create {
        annotated(AspectKAnnotations.ASPECT_FQ_NAME)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val session = context.session
        val calleeSymbol = expression.calleeReference.toResolvedNamedFunctionSymbol() ?: return

        // Skip calls into @Aspect classes (advice-internal calls) and skip
        // calls to advice functions themselves.
        if (isInsideAspectClass(calleeSymbol, session)) return
        if (isAdvice(calleeSymbol, session)) return

        val containingClassName = calleeSymbol.callableId.classId
            ?.shortClassName
            ?.asString()
        val methodName = calleeSymbol.callableId.callableName.asString()

        val matches = collectMatchingAdvices(session, containingClassName, methodName)
        if (matches.isEmpty()) return
        val src = expression.calleeReference.source ?: expression.source ?: return
        for (advice in matches) {
            reporter.reportOn(src, AspectKErrors.WOVEN_CALL_SITE, advice)
        }
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun collectMatchingAdvices(
        session: FirSession,
        containingClassName: String?,
        methodName: String,
    ): List<String> {
        val aspectSymbols = session.predicateBasedProvider
            .getSymbolsByPredicate(ASPECT_LOOKUP)
            .filterIsInstance<FirRegularClassSymbol>()

        val matched = mutableListOf<String>()
        for (aspect in aspectSymbols) {
            val aspectShortName = aspect.classId.relativeClassName.asString()
            for (memberSymbol in aspect.declarationSymbols) {
                if (memberSymbol !is FirNamedFunctionSymbol) continue
                val fir = memberSymbol.fir as? FirNamedFunction ?: continue
                val kindLabel = adviceKindLabel(fir, session) ?: continue
                val classNamePattern = readPattern(fir, AspectKAnnotations.CLASS_NAME_CLASS_ID, session)
                val methodNamePattern = readPattern(fir, AspectKAnnotations.METHOD_NAME_CLASS_ID, session)
                if (!filterMatches(classNamePattern, methodNamePattern, containingClassName, methodName)) continue
                matched += "$aspectShortName.${fir.name.asString()} ($kindLabel)"
            }
        }
        return matched
    }

    private fun filterMatches(
        classNamePattern: String?,
        methodNamePattern: String?,
        containingClassName: String?,
        methodName: String,
    ): Boolean {
        classNamePattern?.let { pattern ->
            val target = containingClassName ?: return false
            if (!NamePattern.matches(pattern, target)) return false
        }
        methodNamePattern?.let { pattern ->
            if (!NamePattern.matches(pattern, methodName)) return false
        }
        return true
    }

    private fun adviceKindLabel(
        fn: FirNamedFunction,
        session: FirSession,
    ): String? =
        when {
            fn.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, session) -> "@Before"
            fn.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, session) -> "@After"
            fn.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, session) -> "@Around"
            else -> null
        }

    private fun readPattern(
        fn: FirNamedFunction,
        annotationClassId: ClassId,
        session: FirSession,
    ): String? =
        fn
            .getAnnotationByClassId(annotationClassId, session)
            ?.getStringArgument(AspectKAnnotations.PATTERN, session)

    private fun isInsideAspectClass(
        symbol: FirNamedFunctionSymbol,
        session: FirSession,
    ): Boolean {
        val classId = symbol.callableId.classId ?: return false
        val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return false
        return classSymbol.hasAnnotation(AspectKAnnotations.ASPECT_CLASS_ID, session)
    }

    private fun isAdvice(
        symbol: FirNamedFunctionSymbol,
        session: FirSession,
    ): Boolean =
        symbol.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, session) ||
            symbol.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, session) ||
            symbol.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, session)
}
