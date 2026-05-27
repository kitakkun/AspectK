// FILE: ObjectAspectSingleton.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName

class Greeter {
    fun greet() {}
}

@Aspect
object SingletonTracer {
    var seenInstances: MutableSet<Int> = mutableSetOf()

    @Before
    @ClassName("Greeter")
    @MethodName("greet")
    fun beforeGreet() {
        // System.identityHashCode would distinguish multiple instances; for a
        // singleton, every invocation observes the same object identity.
        seenInstances.add(System.identityHashCode(this))
    }
}

fun box(): String {
    val g = Greeter()
    repeat(3) { g.greet() }
    if (SingletonTracer.seenInstances.size != 1) {
        return "FAIL: expected 1 distinct aspect instance across 3 calls, got ${SingletonTracer.seenInstances.size}"
    }
    return "OK"
}
