package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.TypeMatchingExpression

class TypeMatchingExpressionMatcher(private val expression: TypeMatchingExpression) {
    fun matches(packageName: String, className: String): Boolean {
        return when (expression) {
            is TypeMatchingExpression.Any -> true
            is TypeMatchingExpression.InPackage -> packageName == expression.packageName
            is TypeMatchingExpression.UnderPackage -> packageName.startsWith(expression.packageName)
            is TypeMatchingExpression.Class -> packageName == expression.packageName && className == expression.className
            is TypeMatchingExpression.ClassPattern -> packageName == expression.packageName && className.startsWith(expression.prefix) && className.endsWith(expression.suffix)
        }
    }
}

