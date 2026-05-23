package com.github.kitakkun.aspectk.test

import com.github.kitakkun.aspectk.annotations.After
import com.github.kitakkun.aspectk.annotations.Around
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.core.JoinPoint
import com.github.kitakkun.aspectk.core.ProceedingJoinPoint

@Aspect
class AspectExample {
    init {
        println("AspectExample init")
    }

    @Before("execution(public com/github/kitakkun/aspectk/test/ExampleClass.*())")
    fun beforeExampleClassMethod() {
        println("Before ExampleClass method call")
    }

    @After("execution(public com/github/kitakkun/aspectk/test/ExampleClass.*(..))")
    fun afterExampleClassMethod(joinPoint: JoinPoint) {
        println("After ExampleClass method call")
        println(joinPoint)
    }

    @Around("execution(public com/github/kitakkun/aspectk/test/ExampleClass.*())")
    fun aroundExampleClassMethod(joinPoint: ProceedingJoinPoint): Any? {
        println("Around: before proceed for ${joinPoint.signature}")
        val result = joinPoint.proceed()
        println("Around: after proceed for ${joinPoint.signature} (result=$result)")
        return result
    }
}
