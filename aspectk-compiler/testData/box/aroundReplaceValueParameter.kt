// FILE: AroundReplaceValueParameter.kt

import com.github.kitakkun.aspectk.annotations.Around
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.ValueParameter
import com.github.kitakkun.aspectk.core.interceptableAdvice

class Greeter {
    fun greet(name: String): String = "hello $name"
}

@Aspect
class Uppercaser {
    @Around
    @ClassName("Greeter")
    @MethodName("greet")
    fun aroundGreet(
        @ValueParameter(0) name: String,
    ): String = interceptableAdvice {
        replaceValueParameter(0, name.uppercase())
        proceed()
    }
}

fun box(): String {
    val result = Greeter().greet("world")
    if (result != "hello WORLD") return "FAIL: greet returned '$result'"
    return "OK"
}
