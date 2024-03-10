package com.github.kitakkun.aspectk.expression

sealed class ClassSignatureExpression {
    data class Normal(
        val packageNames: NameSequenceExpression,
        val classNames: NameSequenceExpression,
    ) : ClassSignatureExpression()

    data class IncludingSubClass(
        val packageNames: NameSequenceExpression,
        val classNames: NameSequenceExpression,
    ) : ClassSignatureExpression()

    data object Any : ClassSignatureExpression()
}
