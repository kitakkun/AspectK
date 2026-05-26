package com.github.kitakkun.aspectk.annotations

/**
 * Marks a function as advice that runs **before** every matched target call.
 *
 * The set of matched targets is described by the other AspectK annotations stacked
 * on the same function (`@Visibility`, `@MethodName`, `@Annotated`, … or a
 * `@Pointcut`-marked meta-annotation that bundles them).
 *
 * The advice function must be declared inside a class annotated with [Aspect].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Before

/**
 * Marks a function as advice that runs **after** every matched target call,
 * regardless of whether it returned normally or threw.
 *
 * The set of matched targets is described by the other AspectK annotations stacked
 * on the same function (`@Visibility`, `@MethodName`, `@Annotated`, … or a
 * `@Pointcut`-marked meta-annotation that bundles them).
 *
 * The advice function must be declared inside a class annotated with [Aspect].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class After

/**
 * Marks a function as advice that **wraps** every matched target call. The advice
 * receives a `ProceedingJoinPoint` and decides whether (and how) to invoke
 * `proceed()`.
 *
 * The set of matched targets is described by the other AspectK annotations stacked
 * on the same function (`@Visibility`, `@MethodName`, `@Annotated`, … or a
 * `@Pointcut`-marked meta-annotation that bundles them).
 *
 * The advice function must be declared inside a class annotated with [Aspect].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Around
