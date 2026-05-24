import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.After

@Aspect
class MyAspect {
    <!ADVICE_INVALID_SIGNATURE!>@Before("args(String)")
    fun beforeOneStringArg(text: String) {}<!>

    <!ADVICE_INVALID_SIGNATURE!>@After("args(Int, String)")
    fun afterTwoArgs(n: Int, text: String) {}<!>
}
