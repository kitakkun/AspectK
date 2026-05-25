import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.core.JoinPoint

var capturedMethodName: String = ""
var capturedFirstArgName: String = ""
var capturedFirstArgValue: Any? = null

@Aspect
class CapturingAspect {
    @Before("execution(public Target.greet(*))")
    fun captureJoinPoint(jp: JoinPoint) {
        capturedMethodName = jp.methodName
        capturedFirstArgName = jp.valueArguments.firstOrNull()?.name ?: ""
        capturedFirstArgValue = jp.valueArguments.firstOrNull()?.value
    }
}

class Target {
    fun greet(name: String) {
        // body intentionally empty; return type Unit so the default-Unit pointcut matches
    }
}

fun box(): String {
    Target().greet("world")
    if (capturedMethodName != "greet") return "FAIL: methodName=$capturedMethodName"
    if (capturedFirstArgName != "name") return "FAIL: firstArgName=$capturedFirstArgName"
    if (capturedFirstArgValue != "world") return "FAIL: firstArgValue=$capturedFirstArgValue"
    return "OK"
}
