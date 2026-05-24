import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.After

val log = mutableListOf<String>()

@Aspect
class TraceAspect {
    @After("execution(public target())")
    fun afterTarget() {
        log.add("after")
    }
}

fun target() {
    log.add("body")
}

fun box(): String {
    target()
    return if (log == listOf("body", "after")) "OK" else "FAIL: $log"
}
