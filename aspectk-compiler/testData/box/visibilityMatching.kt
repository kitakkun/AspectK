// FILE: VisibilityMatching.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.Visibility

var publicHits = 0
var internalHits = 0

class Greeter {
    fun publicGreet() {}
    internal fun internalGreet() {}
}

@Aspect
class PublicOnlyAspect {
    @Before
    @ClassName("Greeter")
    @MethodName("*Greet")
    @Visibility(Visibility.Kind.PUBLIC)
    fun beforePublic() {
        publicHits++
    }
}

@Aspect
class InternalOnlyAspect {
    @Before
    @ClassName("Greeter")
    @MethodName("*Greet")
    @Visibility(Visibility.Kind.INTERNAL)
    fun beforeInternal() {
        internalHits++
    }
}

fun box(): String {
    val g = Greeter()
    g.publicGreet()
    g.internalGreet()
    if (publicHits != 1) return "FAIL: publicHits=$publicHits (expected 1)"
    if (internalHits != 1) return "FAIL: internalHits=$internalHits (expected 1)"
    return "OK"
}
