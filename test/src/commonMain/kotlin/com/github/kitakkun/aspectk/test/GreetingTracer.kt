package com.github.kitakkun.aspectk.test

import com.github.kitakkun.aspectk.annotations.Around
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.DispatchReceiver
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.ValueParameter
import com.github.kitakkun.aspectk.core.interceptableAdvice

@Aspect
class GreetingTracer {
    @Around
    @ClassName("Greeter")
    @MethodName("greet")
    fun aroundGreet(
        @DispatchReceiver greeter: Greeter,
        @ValueParameter(0) name: String,
    ): String = interceptableAdvice {
        println("[around-before] $greeter.greet($name)")
        replaceValueParameter(0, name.uppercase())
        val r = proceed()
        println("[around-after] returned $r")
        r
    }
}
