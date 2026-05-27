// FILE: EmptyMethodNamePattern.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.MethodName

@Aspect
class EmptyPatternAspect {
    @Before
    <!INVALID_POINTCUT_ANNOTATION!>@MethodName("")<!>
    fun beforeNothing() {}
}
