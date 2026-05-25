package com.github.kitakkun.aspectk.annotations

/**
 * Constrains an advice to target functions carrying every modifier listed in [values].
 *
 * Multiple modifiers within a single annotation are AND-combined (the function must
 * carry *all* of them):
 *
 * ```kotlin
 * @Modifiers(Modifiers.Kind.SUSPEND, Modifiers.Kind.INLINE)
 * ```
 *
 * matches functions that are both `suspend` *and* `inline`. For OR semantics (a
 * function carrying any of several modifiers), split the advice into multiple
 * advice functions or wrap the modifier sets in `@Any` (Phase 5).
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Modifiers(vararg val values: Kind) {
    enum class Kind { SUSPEND, INLINE, INFIX, OPERATOR, TAILREC, EXTERNAL }
}
