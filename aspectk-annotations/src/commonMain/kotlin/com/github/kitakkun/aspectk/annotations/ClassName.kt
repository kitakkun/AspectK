package com.github.kitakkun.aspectk.annotations

/**
 * Constrains an advice to target functions whose declaring class's simple name
 * matches [pattern].
 *
 * Pattern syntax:
 * - A literal name (`"UserRepository"`) matches that simple name exactly.
 * - `*` matches zero or more characters at that position.
 *   - `"*Repository"` matches `UserRepository`, `OrderRepository`, ….
 *   - `"Abstract*"` matches `AbstractRepo`, `AbstractService`, ….
 *   - `"*Repo*"` matches anything that contains `Repo`.
 *   - `"*"` matches any class name (use to constrain via [Package] only).
 *
 * For top-level functions with no enclosing class, omit this annotation.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ClassName(val pattern: String)
