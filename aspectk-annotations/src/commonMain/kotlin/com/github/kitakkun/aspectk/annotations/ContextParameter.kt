package com.github.kitakkun.aspectk.annotations

/**
 * Binds the advice parameter to one of the matched target call's **context
 * parameters**. Specify exactly one of [index] or [name]:
 *
 * - `@ContextParameter(0) tx: Transaction` — bind to the first context parameter
 *   by index.
 * - `@ContextParameter(name = "tx") tx: Transaction` — bind by parameter name.
 *
 * The parameter's declared type acts as a covariant assignability constraint.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class ContextParameter(
    val index: Int = -1,
    val name: String = "",
)
