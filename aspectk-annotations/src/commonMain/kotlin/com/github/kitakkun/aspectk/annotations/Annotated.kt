package com.github.kitakkun.aspectk.annotations

import kotlin.reflect.KClass

/**
 * Constrains an advice to target functions carrying at least one annotation whose
 * class is listed in [values].
 *
 * Multiple [values] within a single annotation are OR-combined:
 *
 * ```kotlin
 * @Annotated(Transactional::class, Cached::class)
 * ```
 *
 * matches functions annotated with `@Transactional` or `@Cached`.
 *
 * Target annotations must have at least `BINARY` retention; `SOURCE`-retained
 * annotations are invisible to the compiler plugin at IR time and will be flagged
 * by a FIR diagnostic.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Annotated(vararg val values: KClass<out Annotation>)
