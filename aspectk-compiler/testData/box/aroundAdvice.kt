import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Around
import com.github.kitakkun.aspectk.core.ProceedingJoinPoint

val log = mutableListOf<String>()

@Aspect
class TraceAspect {
    @Around("execution(public target())")
    fun aroundTarget(pjp: ProceedingJoinPoint): Any? {
        log.add("around-before")
        val result = pjp.proceed()
        log.add("around-after")
        return result
    }
}

fun target() {
    log.add("body")
}

fun box(): String {
    target()
    return if (log == listOf("around-before", "body", "around-after")) "OK" else "FAIL: $log"
}
