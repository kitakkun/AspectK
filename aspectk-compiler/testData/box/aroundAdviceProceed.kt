// FILE: AroundAdviceProceed.kt

import com.github.kitakkun.aspectk.annotations.Around
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.core.interceptableAdvice

var trace: String = ""

class Greeter {
    fun greet(name: String): String = "hello $name"
}

@Aspect
class GreetingTracer {
    @Around
    @ClassName("Greeter")
    @MethodName("greet")
    fun aroundGreet(): String = interceptableAdvice {
        trace += "[before]"
        val r = proceed()
        trace += "[after:$r]"
        r
    }
}

fun box(): String {
    val result = Greeter().greet("world")
    if (result != "hello world") return "FAIL: greet returned '$result'"
    if (trace != "[before][after:hello world]") return "FAIL: trace was '$trace'"
    return "OK"
}
