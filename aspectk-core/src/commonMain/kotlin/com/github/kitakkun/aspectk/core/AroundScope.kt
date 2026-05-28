package com.github.kitakkun.aspectk.core

/**
 * Scope receiver for the body of an `@Around` advice.
 *
 * Inside the lambda passed to [interceptableAdvice], the AspectK compiler
 * plugin rewrites calls to [proceed] into an invocation of the matched
 * target method. The class itself is never instantiated at runtime — the
 * member functions all throw if the compiler plugin is not on the classpath.
 */
@Suppress("UNUSED_PARAMETER")
class AroundScope<R> internal constructor() {
    /**
     * Invokes the matched target method with the currently-set bindings and
     * returns its result.
     *
     * Rewritten by the AspectK compiler plugin; calling this outside an
     * `@Around` advice body is an error.
     */
    fun proceed(): R = throwOutsidePluginContext()

    /** Replaces the N-th value parameter for the next [proceed] call. */
    fun replaceValueParameter(index: Int, value: Any?): Unit = throwOutsidePluginContext()

    /** Replaces the named value parameter for the next [proceed] call. */
    fun replaceValueParameter(name: String, value: Any?): Unit = throwOutsidePluginContext()

    /** Replaces the dispatch receiver for the next [proceed] call. */
    fun replaceDispatchReceiver(value: Any?): Unit = throwOutsidePluginContext()

    /** Replaces the extension receiver for the next [proceed] call. */
    fun replaceExtensionReceiver(value: Any?): Unit = throwOutsidePluginContext()

    /** Replaces the N-th context parameter for the next [proceed] call. */
    fun replaceContextParameter(index: Int, value: Any?): Unit = throwOutsidePluginContext()

    /** Replaces the named context parameter for the next [proceed] call. */
    fun replaceContextParameter(name: String, value: Any?): Unit = throwOutsidePluginContext()

    private fun throwOutsidePluginContext(): Nothing =
        error("AspectK: this function only has meaning inside an @Around advice's interceptableAdvice { ... } block.")
}

/**
 * Marker DSL builder for the body of an `@Around` advice.
 *
 * The lambda's receiver exposes `proceed()` and the `replace*` family of
 * override functions. The AspectK compiler plugin rewrites every advice site
 * so that this call is replaced with the lambda body inlined into the
 * matched target, with `proceed()` standing in for the original target body.
 *
 * Outside of an `@Around` advice this function throws — it should never run
 * unaltered.
 */
@Suppress("UNUSED_PARAMETER")
fun <R> interceptableAdvice(block: AroundScope<R>.() -> R): R =
    error("AspectK: interceptableAdvice { ... } only has meaning inside an @Around advice body.")
