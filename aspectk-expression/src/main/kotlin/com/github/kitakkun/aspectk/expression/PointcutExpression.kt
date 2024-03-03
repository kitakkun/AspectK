package com.github.kitakkun.aspectk.expression

import com.github.kitakkun.aspectk.expression.expressionparser.ArgMatchingExpression

sealed class PointcutExpression {
    data class And(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()
    data class Or(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()
    data class Not(val expression: PointcutExpression) : PointcutExpression()

    data class Execution(
        val modifiers: List<FunctionModifier>,
        val packageNames: List<NameExpression>,
        val classNames: List<NameExpression>,
        val functionName: NameExpression,
        val args: Args,
        val returnTypePackageNames: List<NameExpression>,
        val returnTypeClassNames: List<NameExpression>,
    ) : PointcutExpression()

    data class Args(
        val args: List<ArgMatchingExpression>,
        val lastIsVarArg: Boolean,
    ) : PointcutExpression()
}
