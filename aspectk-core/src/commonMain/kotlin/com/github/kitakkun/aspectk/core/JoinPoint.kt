package com.github.kitakkun.aspectk.core

data class JoinPoint(
    val parentClass: Any?,
    val args: List<Any?>,
)
