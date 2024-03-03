package com.github.kitakkun.aspectk.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class Pointcut(val expression: String)
