package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameExpression

class NameExpressionMatcher(val expression: NameExpression) {
    fun matches(name: String): Boolean {
        return when (expression) {
            is NameExpression.Any, NameExpression.Recursive -> true
            is NameExpression.Normal -> name == expression.name
            is NameExpression.Prefixed -> name.startsWith(expression.prefix)
            is NameExpression.Suffixed -> name.endsWith(expression.suffix)
        }
    }
}
