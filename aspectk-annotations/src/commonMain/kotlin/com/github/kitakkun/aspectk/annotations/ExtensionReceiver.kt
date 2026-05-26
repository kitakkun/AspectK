package com.github.kitakkun.aspectk.annotations

/**
 * Binds the advice parameter to the matched target call's **extension receiver**.
 * The parameter's declared type acts as a covariant subtype constraint on
 * matching.
 *
 * ```kotlin
 * @Before @MethodName("substring")
 * fun beforeSubstring(@ExtensionReceiver receiver: String) { ... }
 * ```
 *
 * For functions without an extension receiver, an advice declaring an
 * `@ExtensionReceiver` parameter does not match.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class ExtensionReceiver
