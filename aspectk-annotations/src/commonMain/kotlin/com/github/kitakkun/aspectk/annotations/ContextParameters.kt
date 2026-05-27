package com.github.kitakkun.aspectk.annotations

/**
 * Catch-all binding for the matched target call's **context parameters**:
 * every context argument is collected into a `List<Any?>` in declaration
 * order and passed to the bound parameter.
 *
 * Complementary to [ContextParameter] (singular).
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class ContextParameters
