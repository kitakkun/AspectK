package com.github.kitakkun.aspectk.annotations

/**
 * Marks an annotation class as a *reusable pointcut*.
 *
 * A pointcut annotation bundles a set of constraint annotations (e.g.
 * [Visibility], [Package], [Annotated]) so they can be applied as one tag to
 * many advice functions:
 *
 * ```kotlin
 * @Pointcut
 * @Visibility(Visibility.Kind.PUBLIC)
 * @Package(prefix = "com.example.repo")
 * @Annotated(Transactional::class)
 * annotation class PublicTransactionalRepoCall
 *
 * @Aspect
 * class TxAspect {
 *     @Around
 *     @PublicTransactionalRepoCall              // pulls in the three constraints above
 *     @MethodName(startsWith = "save")          // can narrow further at the call site
 *     fun BaseRepo.tx(user: User): Unit { … }
 * }
 * ```
 *
 * `@Pointcut`-marked annotations are expanded transitively, so a pointcut
 * annotation can reference other pointcut annotations.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Pointcut
