package com.github.kitakkun.aspectk.test

import com.github.kitakkun.aspectk.annotations.After
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.DispatchReceiver
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.ValueParameter

@Aspect
class GreetingTracer {
    @Before
    @ClassName("Greeter")
    @MethodName("greet")
    fun beforeGreet(
        @DispatchReceiver greeter: Greeter,
        @ValueParameter(0) name: String,
    ) {
        println("[before] $greeter.greet($name)")
    }

    @After
    @ClassName("Greeter")
    @MethodName("greet")
    fun afterGreet(
        @ValueParameter(0) name: String,
    ) {
        println("[after] greeted $name")
    }
}
