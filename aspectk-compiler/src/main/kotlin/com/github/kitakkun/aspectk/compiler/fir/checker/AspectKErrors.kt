package com.github.kitakkun.aspectk.compiler.fir.checker

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction

object AspectKErrors : KtDiagnosticsContainer() {
    /** `@Before` / `@After` / `@Around` used outside an `@Aspect` class. */
    val ADVICE_OUTSIDE_ASPECT_CLASS by error1<KtFunction, String>()

    /** A pointcut annotation's parameters violate the constraints documented on its KDoc. */
    val INVALID_POINTCUT_ANNOTATION by error2<KtAnnotationEntry, String, String>()

    /** `@Annotated(Foo::class)` where `Foo` is `@Retention(SOURCE)` (invisible at IR time). */
    val ANNOTATED_TARGETS_SOURCE_RETENTION by error1<KtAnnotationEntry, String>()

    /** `@ValueParameter` / `@ContextParameter` must specify exactly one of `index` / `name`. */
    val INVALID_BINDING_ANNOTATION by error2<KtAnnotationEntry, String, String>()

    /**
     * Advice (`@Before` / `@After` / `@Around`) declared without any matching
     * annotation (`@Package` / `@ClassName` / `@MethodName` / `@Visibility` /
     * `@Modality` / `@Modifiers` / `@Annotated`). Such advice would match an
     * unbounded set of targets — almost always a mistake — so we reject it at
     * FIR time rather than letting the IR weaver run unconstrained.
     */
    val ADVICE_HAS_NO_MATCHING_ANNOTATION by error1<KtFunction, String>()

    /**
     * A function call site is woven by an `@Around` / `@Before` / `@After`
     * advice. Surfaces as a weak compiler diagnostic so IntelliJ shows an
     * inline marker. The single argument is the woven advice's FQN + kind,
     * e.g. `"GreetingTracer.aroundGreet (@Around)"`.
     */
    val WOVEN_CALL_SITE by warning1<KtElement, String>()

    /**
     * An `@Around` advice declares a binding shape the around weaver doesn't
     * (yet) support — typically `@ValueParameters` / `@ContextParameters`.
     * The advice silently doesn't weave; this warning surfaces that so the
     * user isn't left wondering why their `@Around` stopped intercepting.
     */
    val AROUND_UNSUPPORTED_BINDING by warning1<KtElement, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = AspectKDefaultMessages
}

private object AspectKDefaultMessages : BaseDiagnosticRendererFactory() {
    @Suppress("ktlint:standard:property-naming")
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("AspectK") { map ->
        map.put(
            AspectKErrors.ADVICE_OUTSIDE_ASPECT_CLASS,
            "@{0} advice must be declared as a member of an @Aspect class.",
            CommonRenderers.STRING,
        )
        map.put(
            AspectKErrors.INVALID_POINTCUT_ANNOTATION,
            "@{0}: {1}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        map.put(
            AspectKErrors.ANNOTATED_TARGETS_SOURCE_RETENTION,
            "@Annotated cannot target ''{0}'' because it has @Retention(SOURCE); use BINARY or RUNTIME retention.",
            CommonRenderers.STRING,
        )
        map.put(
            AspectKErrors.INVALID_BINDING_ANNOTATION,
            "@{0}: {1}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        map.put(
            AspectKErrors.WOVEN_CALL_SITE,
            "intercepted by {0}",
            CommonRenderers.STRING,
        )
        map.put(
            AspectKErrors.AROUND_UNSUPPORTED_BINDING,
            "@Around advice not woven: {0}",
            CommonRenderers.STRING,
        )
        map.put(
            AspectKErrors.ADVICE_HAS_NO_MATCHING_ANNOTATION,
            "@{0} advice declares no matching annotation (@Package / @ClassName / @MethodName / @Visibility / @Modality / @Modifiers / @Annotated). Add at least one to bound the targets it intercepts.",
            CommonRenderers.STRING,
        )
    }
}
