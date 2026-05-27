// FILE: BindingAnnotationMissingIndexAndName.kt

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.ValueParameter

@Aspect
class Aspect {
    @Before
    @MethodName("greet")
    fun beforeGreet(
        <!INVALID_BINDING_ANNOTATION!>@ValueParameter<!> name: String,
    ) {}
}
