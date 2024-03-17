package com.github.kitakkun.aspectk.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class Before(val pointcut: String)

@Target(AnnotationTarget.FUNCTION)
annotation class After(val pointcut: String)

@Target(AnnotationTarget.FUNCTION)
annotation class Around(val pointcut: String)

@Target(AnnotationTarget.FUNCTION)
annotation class AfterReturning(val pointcut: String, val returning: String)

@Target(AnnotationTarget.FUNCTION)
annotation class AfterThrowing(val pointcut: String, val throwing: String)
