// FILE: AdviceOutsideAspect.kt

import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.MethodName

class NotAnAspect {
    <!ADVICE_OUTSIDE_ASPECT_CLASS!>@Before
    @MethodName("foo")
    fun beforeFoo() {}<!>
}
