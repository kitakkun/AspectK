// FILE: BindingAnnotationNegativeIndex.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.ContextParameter
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.ValueParameter

@Aspect
class NegativeIndexAspect {
    @Before
    @MethodName("greet")
    fun beforeNegativeIndex(
        <!INVALID_BINDING_ANNOTATION!>@ValueParameter(index = -1)<!> name: String,
    ) {}

    @Before
    @MethodName("greet")
    fun beforeNegativeIndexFar(
        <!INVALID_BINDING_ANNOTATION!>@ValueParameter(index = -5)<!> name: String,
    ) {}

    @Before
    @MethodName("greet")
    fun beforeNegativeContextIndex(
        <!INVALID_BINDING_ANNOTATION!>@ContextParameter(index = -1)<!> ctx: String,
    ) {}

    @Before
    @MethodName("greet")
    fun beforeOkIndex(
        @ValueParameter(index = 0) name: String,
    ) {}
}
