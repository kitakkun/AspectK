// FILE: ModifiersMatching.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.Modifiers

var inlineHits = 0
var nonInlineHits = 0

class Util {
    fun normalDo() {}
    inline fun inlineDo() {}
}

@Aspect
class InlineOnlyAspect {
    @Before
    @ClassName("Util")
    @MethodName("*Do")
    @Modifiers(Modifiers.Kind.INLINE)
    fun beforeInline() {
        inlineHits++
    }
}

@Aspect
class CatchAllAspect {
    @Before
    @ClassName("Util")
    @MethodName("*Do")
    fun beforeAny() {
        nonInlineHits++
    }
}

fun box(): String {
    val u = Util()
    u.normalDo()
    u.inlineDo()
    // CatchAllAspect should match both, InlineOnlyAspect only inlineDo.
    if (inlineHits != 1) return "FAIL: inlineHits=$inlineHits (expected 1)"
    if (nonInlineHits != 2) return "FAIL: nonInlineHits=$nonInlineHits (expected 2)"
    return "OK"
}
