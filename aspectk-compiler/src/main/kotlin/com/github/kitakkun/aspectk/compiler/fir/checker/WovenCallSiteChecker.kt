package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.backend.matching.NamePattern
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol

/**
 * Emits a weak [AspectKErrors.WOVEN_CALL_SITE] diagnostic at each function
 * call whose callee is matched by at least one advice in the current session.
 *
 * Phase 3.5 over-approximates: only `@ClassName` / `@MethodName` patterns
 * are honoured at FIR time. IR remains the authoritative weaver; an
 * over-fired marker is acceptable for editor feedback.
 *
 * The advice list comes from [WovenAdviceIndex], a session component that
 * resolves every `@Aspect`-class's advices once per session and caches the
 * pre-resolved `(classNamePattern, methodNamePattern, label)` tuples. This
 * checker fires on every `FirFunctionCall` in the source, so resolving the
 * list inline would compound across call sites.
 *
 * Calls whose callee lives inside an `@Aspect` class are skipped — the IR
 * weaver never targets those either.
 */
object WovenCallSiteChecker : FirExpressionChecker<FirFunctionCall>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val session = context.session
        // Short-circuit when the session has no advices at all — most calls
        // in most files. Avoids per-call-site annotation reads below.
        val entries = session.wovenAdviceIndex.entries
        if (entries.isEmpty()) return

        val calleeSymbol = expression.calleeReference.toResolvedNamedFunctionSymbol() ?: return

        // Skip calls into @Aspect classes (advice-internal calls) and skip
        // calls to advice functions themselves.
        if (isInsideAspectClass(calleeSymbol, session)) return
        if (isAdvice(calleeSymbol, session)) return

        val containingClassName = calleeSymbol.callableId.classId
            ?.shortClassName
            ?.asString()
        val methodName = calleeSymbol.callableId.callableName.asString()

        val matches = entries.filter { entry ->
            filterMatches(entry.classNamePattern, entry.methodNamePattern, containingClassName, methodName)
        }
        if (matches.isEmpty()) return
        val src = expression.calleeReference.source ?: expression.source ?: return
        for (advice in matches) {
            reporter.reportOn(src, AspectKErrors.WOVEN_CALL_SITE, advice.label)
        }
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
