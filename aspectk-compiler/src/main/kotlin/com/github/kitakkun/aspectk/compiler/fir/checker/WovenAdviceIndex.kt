package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirLazyValue
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Session-scoped cache of every advice the FIR session can see, with its
 * `@ClassName` / `@MethodName` patterns pre-resolved.
 *
 * [WovenCallSiteChecker] fires on every `FirFunctionCall`, which is a lot —
 * recomputing the advice list (and re-reading every annotation argument) per
 * call site adds up on big sources. The advice list is invariant across a
 * single compilation, so we resolve it once via [LookupPredicate.annotated]
 * on first access and reuse the result for the rest of the session.
 *
 * The cache uses [firCachesFactory] (specifically `createLazyValue`) so it
 * participates in the FIR pipeline's invalidation. A plain Kotlin `by lazy`
 * is technically equivalent in CLI compiles but doesn't get invalidated
 * when the session is recreated under IDE-driven file changes.
 *
 * Register through [com.github.kitakkun.aspectk.compiler.fir.AspectKFirExtensionRegistrar]
 * so the session-wide annotation index has `@Aspect` available.
 */
class WovenAdviceIndex(
    session: FirSession,
) : FirExtensionSessionComponent(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(ASPECT_LOOKUP)
    }

    /**
     * Pre-resolved snapshot of one advice function the checker may want to
     * report against.
     *
     * - [label] is the user-facing string baked into the diagnostic argument
     *   (e.g. `"GreetingTracer.aroundGreet (@Around)"`).
     * - [classNamePattern] / [methodNamePattern] are the literal pattern
     *   strings the user wrote; `null` means the corresponding annotation
     *   was absent (i.e. the constraint is open).
     */
    class Entry(
        val label: String,
        val classNamePattern: String?,
        val methodNamePattern: String?,
    )

    private val entriesCache: FirLazyValue<List<Entry>> =
        session.firCachesFactory.createLazyValue { computeEntries() }

    val entries: List<Entry> get() = entriesCache.getValue()

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun computeEntries(): List<Entry> {
        val aspectSymbols = session.predicateBasedProvider
            .getSymbolsByPredicate(ASPECT_LOOKUP)
            .filterIsInstance<FirRegularClassSymbol>()

        val result = mutableListOf<Entry>()
        for (aspect in aspectSymbols) {
            val aspectShortName = aspect.classId.relativeClassName.asString()
            for (memberSymbol in aspect.declarationSymbols) {
                if (memberSymbol !is FirNamedFunctionSymbol) continue
                val fir = memberSymbol.fir as? FirNamedFunction ?: continue
                val kindLabel = adviceKindLabel(fir, session) ?: continue
                result += Entry(
                    label = "$aspectShortName.${fir.name.asString()} ($kindLabel)",
                    classNamePattern = readPattern(fir, AspectKAnnotations.CLASS_NAME_CLASS_ID, session),
                    methodNamePattern = readPattern(fir, AspectKAnnotations.METHOD_NAME_CLASS_ID, session),
                )
            }
        }
        return result
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

    companion object {
        val ASPECT_LOOKUP: LookupPredicate = LookupPredicate.create {
            annotated(AspectKAnnotations.ASPECT_FQ_NAME)
        }
    }
}

val FirSession.wovenAdviceIndex: WovenAdviceIndex by FirSession.sessionComponentAccessor()
