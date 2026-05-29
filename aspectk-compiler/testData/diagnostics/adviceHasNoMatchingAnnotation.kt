// FILE: AdviceHasNoMatchingAnnotation.kt

import com.github.kitakkun.aspectk.annotations.After
import com.github.kitakkun.aspectk.annotations.Around
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName

@Aspect
class Unbounded {
    // @Before with no matching annotation — flagged.
    <!ADVICE_HAS_NO_MATCHING_ANNOTATION!>@Before
    fun beforeAll() {}<!>

    // @After likewise.
    <!ADVICE_HAS_NO_MATCHING_ANNOTATION!>@After
    fun afterAll() {}<!>

    // @Around likewise. (body is empty to keep the test focused on the
    // missing-matching-annotation diagnostic — IR-level @Around requires
    // an `interceptableAdvice { ... }` body, but this test only exercises
    // the FIR pipeline.)
    <!ADVICE_HAS_NO_MATCHING_ANNOTATION!>@Around
    fun aroundAll() {}<!>

    // A single matching annotation is enough — not flagged.
    @Before
    @MethodName("greet")
    fun beforeNamed() {}

    // @ClassName-only is sufficient.
    @Before
    @ClassName("Greeter")
    fun beforeAnyMethodInGreeter() {}
}
