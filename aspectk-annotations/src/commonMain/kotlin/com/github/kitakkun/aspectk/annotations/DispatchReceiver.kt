package com.github.kitakkun.aspectk.annotations

/**
 * Binds the advice parameter to the matched target call's **dispatch receiver**
 * (`this` of the method's declaring class). The parameter's declared type acts
 * as a covariant subtype constraint: the target's dispatch receiver type must
 * be a subtype of the binding parameter's type.
 *
 * ```kotlin
 * @Before @MethodName("greet")
 * fun beforeGreet(@DispatchReceiver greeter: Greeter) { ... }
 * ```
 *
 * For top-level functions (no dispatch receiver), an advice declaring a
 * `@DispatchReceiver` parameter does not match.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class DispatchReceiver
