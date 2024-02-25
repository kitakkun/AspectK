package com.github.kitakkun.aspectk.expression

sealed class PointcutExpression {
    data class And(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()
    data class Or(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()
    data class Not(val expression: PointcutExpression) : PointcutExpression()

    data class Execution(val functionMatchingExpression: FunctionMatchingExpression) : PointcutExpression()
    data class Args(val args: List<ArgumentMatchingExpression>) : PointcutExpression()
}
