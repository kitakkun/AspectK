package com.github.kitakkun.aspectk.annotations

/**
 * Constrains an advice to target functions whose name matches [pattern].
 *
 * Pattern syntax:
 * - A literal name (`"save"`) matches that method name exactly.
 * - `*` matches zero or more characters at that position.
 *   - `"save*"` matches `save`, `saveAll`, ….
 *   - `"*Async"` matches `loadAsync`, `runAsync`, ….
 *   - `"*find*"` matches anything that contains `find`.
 *   - `"*"` matches any method name.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class MethodName(val pattern: String)
