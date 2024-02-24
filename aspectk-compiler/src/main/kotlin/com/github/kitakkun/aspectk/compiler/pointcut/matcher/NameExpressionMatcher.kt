package com.github.kitakkun.aspectk.compiler.pointcut.matcher

import com.github.kitakkun.aspectk.compiler.pointcut.expression.NameExpression

class NameExpressionMatcher(val expression: NameExpression) {
    fun matches(name: String): Boolean {
        return when (expression) {
            is NameExpression.Normal -> name == expression.name
            is NameExpression.Prefixed -> name.startsWith(expression.prefix)
            is NameExpression.Suffixed -> name.endsWith(expression.suffix)
        }
    }
}
