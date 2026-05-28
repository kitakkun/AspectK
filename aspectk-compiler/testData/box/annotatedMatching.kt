// FILE: AnnotatedMatching.kt

import com.github.kitakkun.aspectk.annotations.Annotated
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName

@Retention(AnnotationRetention.RUNTIME)
annotation class Loggable

var loggableHits = 0

class Service {
    @Loggable
    fun loggableAction() {}
    fun silentAction() {}
}

@Aspect
class LoggingAspect {
    @Before
    @ClassName("Service")
    @MethodName("*Action")
    @Annotated(Loggable::class)
    fun beforeLoggable() {
        loggableHits++
    }
}

fun box(): String {
    val s = Service()
    s.loggableAction()
    s.silentAction()
    if (loggableHits != 1) return "FAIL: loggableHits=$loggableHits (expected 1, only @Loggable should match)"
    return "OK"
}
