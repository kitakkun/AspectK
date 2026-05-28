package com.github.kitakkun.aspectk.annotations

/**
 * Constrains an advice to target functions whose visibility is one of [values].
 *
 * Multiple values within a single annotation are OR-combined:
 *
 * ```kotlin
 * @Visibility(Visibility.Kind.PUBLIC, Visibility.Kind.INTERNAL)
 * ```
 *
 * matches public-or-internal functions.
 *
 * Stacking multiple `@Visibility` annotations on the same advice is not supported;
 * use a single annotation with the union of intended visibilities.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Visibility(vararg val values: Kind) {
    enum class Kind { PUBLIC, INTERNAL, PROTECTED, PRIVATE }
}
