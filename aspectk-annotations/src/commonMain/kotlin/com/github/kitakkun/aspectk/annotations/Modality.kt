package com.github.kitakkun.aspectk.annotations

/**
 * Constrains an advice to target functions whose enclosing class modality is one
 * of [values].
 *
 * `FINAL` matches classes without `open` / `abstract` / `sealed`. `OPEN` matches
 * `open class` and `open fun` parents. `ABSTRACT` matches `abstract class` and
 * `abstract fun`. `SEALED` matches `sealed class` and `sealed interface`.
 *
 * For top-level functions (no enclosing class) the annotation matches nothing.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Modality(vararg val values: Kind) {
    enum class Kind { FINAL, OPEN, ABSTRACT, SEALED }
}
