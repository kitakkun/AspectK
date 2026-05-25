import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before

@Aspect
class MyAspect {
    <!INVALID_POINTCUT_EXPRESSION!>@Before("garbage(")
    fun before() {}<!>
}
