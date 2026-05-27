package com.github.kitakkun.aspectk.annotations

/**
 * Catch-all binding for the matched target call's **value parameters**: every
 * value argument is collected into a `List<Any?>` in declaration order and
 * passed to the bound parameter.
 *
 * ```kotlin
 * @Before @MethodName("*")
 * fun logAll(@ValueParameters args: List<Any?>) {
 *     println("called with $args")
 * }
 * ```
 *
 * Complementary to [ValueParameter] (singular), which binds one specific slot
 * by index or name. Use [ValueParameters] when you don't know or don't care
 * which arguments the target declares — typical for generic logging or
 * auditing aspects.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class ValueParameters
