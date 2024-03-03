package com.github.kitakkun.aspectk.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class Before(val pointcut: String)

@Target(AnnotationTarget.FUNCTION)
annotation class After(val pointcut: String)

@Target(AnnotationTarget.FUNCTION)
annotation class Around(val pointcut: String)
