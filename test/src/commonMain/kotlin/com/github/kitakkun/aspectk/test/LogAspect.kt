package com.github.kitakkun.aspectk.test

import com.github.kitakkun.aspectk.annotations.After
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.Pointcut

@Aspect
class LogAspect {
    @Pointcut("execution(public *(..))")
    fun allFunctions() {
    }

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
