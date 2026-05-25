package com.github.kitakkun.aspectk.test

import com.github.kitakkun.aspectk.annotations.Annotated
import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.ClassName
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.Modality
import com.github.kitakkun.aspectk.annotations.Modifiers
import com.github.kitakkun.aspectk.annotations.Package
import com.github.kitakkun.aspectk.annotations.Pointcut
import com.github.kitakkun.aspectk.annotations.Visibility

// --- Phase 1 compile-only preview --------------------------------------------
//
// Phase 1 only defines the new pointcut annotation set. The FIR checker
// (Phase 2) and the IR transformer (Phase 3) do not look at any of this yet,
// so applying these annotations has no runtime effect. The point of this file
// is to verify that the annotation surface actually compiles from end-user code
// the way the design intends.

// A marker annotation used by [TransactionalRepoCall] below.
annotation class Transactional

// A reusable pointcut declared as a meta-annotation. Each constraint annotation
// listed here is part of the bundle; applying [TransactionalRepoCall] to an
// advice expands to all four constraints below.
@Pointcut
@Visibility(Visibility.Kind.PUBLIC)
@Modality(Modality.Kind.OPEN, Modality.Kind.FINAL)
@Modifiers(Modifiers.Kind.SUSPEND)
@Package("com.github.kitakkun.aspectk.test.**")
@ClassName("*Repo")
@MethodName("save*")
@Annotated(Transactional::class)
annotation class TransactionalRepoCall

@Aspect
class V1PreviewAspect {
    // Pulls in every constraint declared on [TransactionalRepoCall].
    @TransactionalRepoCall
    fun previewAdvice() {
        // Phase 3 will replace this body's execution path with an
        // advice-application IR transform.
    }
}
