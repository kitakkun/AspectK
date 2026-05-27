// FILE: BindingExtraction.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.DispatchReceiver
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.ValueParameter

var observedReceiverClass: String = ""
var observedName: String = ""

class Greeter {
    fun greet(name: String): String = "hello $name"
}

@Aspect
class GreetingObserver {
    @Before
    @ClassName("Greeter")
    @MethodName("greet")
    fun beforeGreet(
        @DispatchReceiver greeter: Greeter,
        @ValueParameter(0) name: String,
    ) {
        observedReceiverClass = greeter::class.java.simpleName
        observedName = name
    }
}

fun box(): String {
    Greeter().greet("world")
    if (observedReceiverClass != "Greeter") return "FAIL: receiver='$observedReceiverClass'"
    if (observedName != "world") return "FAIL: name='$observedName'"
    return "OK"
}
