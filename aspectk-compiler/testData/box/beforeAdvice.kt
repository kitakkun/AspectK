import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before

val log = mutableListOf<String>()

@Aspect
class TraceAspect {
    @Before("execution(public target())")
    fun beforeTarget() {
        log.add("before")
    }
}

fun target() {
    log.add("body")
}

fun box(): String {
    target()
    return if (log == listOf("before", "body")) "OK" else "FAIL: $log"
}
