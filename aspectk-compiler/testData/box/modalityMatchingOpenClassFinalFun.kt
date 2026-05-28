// FILE: ModalityMatchingOpenClassFinalFun.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.Modality

// Verifies the "enclosing class OR function-own" semantic for @Modality:
// touch() is FINAL (default member fun), but its enclosing class is OPEN.
// Per Modality.kt KDoc ("OPEN matches open class and open fun parents"),
// both @Modality(OPEN) and @Modality(FINAL) must hit this target.

var openHits = 0
var finalHits = 0

open class Mixed {
    fun touch() {}
}

@Aspect
class OpenAspect {
    @Before
    @MethodName("touch")
    @Modality(Modality.Kind.OPEN)
    fun beforeOpen() {
        openHits++
    }
}

@Aspect
class FinalAspect {
    @Before
    @MethodName("touch")
    @Modality(Modality.Kind.FINAL)
    fun beforeFinal() {
        finalHits++
    }
}

fun box(): String {
    Mixed().touch()
    if (openHits != 1) return "FAIL: openHits=$openHits (expected 1 — open class should match OPEN)"
    if (finalHits != 1) return "FAIL: finalHits=$finalHits (expected 1 — final fun should match FINAL)"
    return "OK"
}
