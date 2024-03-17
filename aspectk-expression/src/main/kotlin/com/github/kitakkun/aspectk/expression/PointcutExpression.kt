package com.github.kitakkun.aspectk.expression

import com.github.kitakkun.aspectk.expression.expressionparser.ArgMatchingExpression

sealed class PointcutExpression {
    data object Empty : PointcutExpression()

    data class And(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()

    data class Or(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()

    data class Not(val expression: PointcutExpression) : PointcutExpression()

    sealed class Execution : PointcutExpression() {
        abstract val modifiers: List<FunctionModifier>
        abstract val functionName: NameExpression
        abstract val args: Args
        abstract val returnType: ClassSignatureExpression

        data class TopLevelFunction(
            override val modifiers: List<FunctionModifier>,
            val packageNames: NameSequenceExpression,
            override val functionName: NameExpression,
            override val args: Args,
            override val returnType: ClassSignatureExpression,
        ) : Execution()

        data class MemberFunction(
            override val modifiers: List<FunctionModifier>,
            val classSignature: ClassSignatureExpression,
            override val functionName: NameExpression,
            override val args: Args,
            override val returnType: ClassSignatureExpression,
        ) : Execution()
    }

    data class Args(
        val args: List<ArgMatchingExpression>,
        val lastIsVarArg: Boolean,
    ) : PointcutExpression()

    data class Named(
        val packageNames: List<NameExpression.Normal>,
        val classNames: List<NameExpression.Normal>,
        val functionName: NameExpression.Normal,
    ) : PointcutExpression()
}
