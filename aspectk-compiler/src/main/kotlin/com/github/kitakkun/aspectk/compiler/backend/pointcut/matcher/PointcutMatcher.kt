package com.github.kitakkun.aspectk.compiler.backend.pointcut.matcher

interface PointcutMatcher<T> {
    fun matches(target: T): Boolean
}
