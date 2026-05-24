import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.Pointcut

class NotAnAspect {
    <!ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION!>@Before("execution(public com/example/Foo.bar())")
    fun beforeAdvice() {}<!>

    <!POINTCUT_FUNCTION_DECLARATION_SCOPE_VIOLATION!>@Pointcut("execution(public com/example/Foo.bar())")
    fun pointcutDecl() {}<!>
}
