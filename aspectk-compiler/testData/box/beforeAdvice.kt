// FILE: BeforeAdvice.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName

var trace: String = ""

class Greeter {
    fun greet(name: String): String = "hello $name"
}

@Aspect
class GreetingTracer {
    @Before
    @ClassName("Greeter")
    @MethodName("greet")
    fun beforeGreet() {
        trace += "[before]"
    }
}

fun box(): String {
    val result = Greeter().greet("world")
    if (result != "hello world") return "FAIL: greet returned '$result'"
    if (trace != "[before]") return "FAIL: trace was '$trace'"
    return "OK"
}
