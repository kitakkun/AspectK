package com.github.kitakkun.aspectk.test

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName

@Aspect
class GreetingTracer {
    @Before
    @ClassName("Greeter")
    @MethodName("greet")
    fun beforeGreet() {
        println("[before] Greeter.greet")
    }
}
