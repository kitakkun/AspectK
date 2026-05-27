// FILE: AfterAdvice.kt

import com.github.kitakkun.aspectk.annotations.After
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName

var trace: String = ""

class Greeter {
    fun greet(name: String): String {
        trace += "[body]"
        return "hello $name"
    }
}

@Aspect
class GreetingTracer {
    @After
    @ClassName("Greeter")
    @MethodName("greet")
    fun afterGreet() {
        trace += "[after]"
    }
}

fun box(): String {
    val result = Greeter().greet("world")
    if (result != "hello world") return "FAIL: greet returned '$result'"
    if (trace != "[body][after]") return "FAIL: trace was '$trace'"
    return "OK"
}
