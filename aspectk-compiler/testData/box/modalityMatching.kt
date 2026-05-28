// FILE: ModalityMatching.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.Modality

var openHits = 0
var finalHits = 0

open class OpenOwner {
    open fun touch() {}
}

class FinalOwner {
    fun touch() {}
}

@Aspect
class OpenOnlyAspect {
    @Before
    @MethodName("touch")
    @Modality(Modality.Kind.OPEN)
    fun beforeOpenTouch() {
        openHits++
    }
}

@Aspect
class FinalOnlyAspect {
    @Before
    @MethodName("touch")
    @Modality(Modality.Kind.FINAL)
    fun beforeFinalTouch() {
        finalHits++
    }
}

fun box(): String {
    OpenOwner().touch()
    FinalOwner().touch()
    if (openHits != 1) return "FAIL: openHits=$openHits (expected 1)"
    if (finalHits != 1) return "FAIL: finalHits=$finalHits (expected 1)"
    return "OK"
}
