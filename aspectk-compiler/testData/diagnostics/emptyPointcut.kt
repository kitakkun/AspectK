import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before

@Aspect
class MyAspect {
    <!EMPTY_POINTCUT_EXPRESSION!>@Before("")
    fun before() {}<!>
}
