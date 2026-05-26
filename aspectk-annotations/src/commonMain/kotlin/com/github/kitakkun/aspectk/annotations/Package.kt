package com.github.kitakkun.aspectk.annotations

/**
 * Constrains an advice to target functions whose declaring package matches
 * [pattern].
 *
 * Pattern syntax:
 * - A literal name (`"com.example.repo"`) matches that package exactly.
 * - `*` matches one whole segment (anything between two dots).
 *   - `"com.example.*"` matches `com.example.foo`, `com.example.bar`, but **not**
 *     `com.example` itself or `com.example.foo.bar`.
 * - `**` matches zero or more whole segments.
 *   - `"com.example.**"` matches `com.example`, `com.example.foo`, and
 *     `com.example.foo.bar`.
 *   - `"**.repo"` matches any package whose last segment is `repo`.
 *
 * For top-level functions with no package (the root package), use [pattern] = `""`.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Package(val pattern: String)
