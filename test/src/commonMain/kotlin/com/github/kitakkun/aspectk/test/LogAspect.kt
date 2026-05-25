package com.github.kitakkun.aspectk.test

import com.github.kitakkun.aspectk.annotations.After
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before

@Aspect
class LogAspect {
    // The v0.x `@Pointcut("execution(public *(..))") fun allFunctions()` declaration
    // was removed when `@Pointcut` was repurposed as a meta-annotation marker for v1.
    // Reusable pointcuts are now expressed as `@Pointcut`-marked annotation classes.

    @Before("args(String)")
    fun logString() {
        println("Before method call")
    }

    @After("args(Int)")
    fun logInt() {
        println("Before method call 2")
    }
}

fun hoge(a: Int) {
}
