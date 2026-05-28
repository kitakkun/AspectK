package com.github.kitakkun.aspectk.annotations

/**
 * Binds the advice parameter to one of the matched target call's **value
 * parameters**. Specify exactly one of [index] or [name]:
 *
 * - `@ValueParameter(0) name: String` — bind to the first value parameter by index.
 * - `@ValueParameter(name = "user") user: User` — bind by parameter name.
 *
 * The binding parameter's declared type acts as a covariant assignability
 * constraint: the target's value parameter at that position/name must be
 * assignable to the binding parameter's type.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class ValueParameter(
    val index: Int = -1,
    val name: String = "",
)
