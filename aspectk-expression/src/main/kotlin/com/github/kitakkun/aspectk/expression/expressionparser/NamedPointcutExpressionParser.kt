package com.github.kitakkun.aspectk.expression.expressionparser

import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.tokenparser.NamedPointcutToken
import com.github.kitakkun.aspectk.expression.tokenparser.NamedPointcutTokenType

class NamedPointcutExpressionParser(private val tokens: List<NamedPointcutToken>) {
    fun parse(): PointcutExpression.Named {
        val packageNames = tokens.filter { it.type == NamedPointcutTokenType.PACKAGE }.map { NameExpression.Normal(it.lexeme) }
        val classNames = tokens.filter { it.type == NamedPointcutTokenType.CLASS }.map { NameExpression.Normal(it.lexeme) }
        val functionName = NameExpression.Normal(tokens.last().lexeme)

        return PointcutExpression.Named(packageNames, classNames, functionName)
    }
}
