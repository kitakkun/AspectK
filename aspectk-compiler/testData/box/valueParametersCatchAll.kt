// FILE: ValueParametersCatchAll.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.ValueParameters

var seen: List<Any?> = emptyList()

class Service {
    fun process(name: String, count: Int, flag: Boolean): String = "$name:$count:$flag"
}

@Aspect
class Auditor {
    @Before
    @ClassName("Service")
    @MethodName("process")
    fun beforeProcess(@ValueParameters args: List<Any?>) {
        seen = args
    }
}

fun box(): String {
    Service().process("alice", 42, true)
    if (seen != listOf<Any?>("alice", 42, true)) return "FAIL: seen=$seen"
    return "OK"
}
