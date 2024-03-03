package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameExpression

class ClassMatcher(
    private val packageNameExpressions: List<NameExpression>,
    private val classNameExpressions: List<NameExpression>
) {
    fun matches(packageName: String, className: String): Boolean {
        className.split(".").filterNot { it.isEmpty() }.forEachIndexed { index, name ->
            val nameExpression = classNameExpressions.getOrNull(index) ?: return false
            if (!NameExpressionMatcher(nameExpression).matches(name)) {
                return false
            }
            if (nameExpression is NameExpression.Recursive) {
                return true
            }
        }

        if (packageName == "kotlin" && packageNameExpressions.isEmpty()) {
            if (isKotlinPrimitive(className)) return true
        }

        packageName.split("/").filterNot { it.isEmpty() }.forEachIndexed { index, name ->
            val nameExpression = packageNameExpressions.getOrNull(index) ?: return false
            if (!NameExpressionMatcher(nameExpression).matches(name)) {
                return false
            }
            if (nameExpression is NameExpression.Recursive) {
                return true
            }
        }

        return true
    }

    private fun isKotlinPrimitive(className: String): Boolean {
        return when (className) {
            "Byte", "Short", "Int", "Long", "Float", "Double",
            "UByte", "UShort", "UInt", "ULong" -> true

            "Boolean" -> true

            "Char", "String" -> true

            "Array" -> true

            "Unit" -> true

            else -> false
        }
    }
}
