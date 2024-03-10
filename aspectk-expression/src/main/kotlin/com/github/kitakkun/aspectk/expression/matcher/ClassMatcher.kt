package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameSequenceExpression

class ClassMatcher(
    packageNameExpressions: NameSequenceExpression,
    classNameExpressions: NameSequenceExpression,
) {
    private val packageSequenceMatcher = NameSequenceExpressionMatcher(packageNameExpressions)
    private val classSequenceMatcher = NameSequenceExpressionMatcher(classNameExpressions)
    private val emptyPackageExpressions = packageNameExpressions is NameSequenceExpression.Empty

    fun matches(
        packageName: String,
        className: String,
    ): Boolean {
        if (!classSequenceMatcher.matches(className)) return false

        if (packageName == "kotlin" && emptyPackageExpressions) {
            if (isKotlinPrimitive(className)) return true
        }

        return packageSequenceMatcher.matches(packageName)
    }

    private fun isKotlinPrimitive(className: String): Boolean {
        return when (className) {
            "Byte", "Short", "Int", "Long", "Float", "Double",
            "UByte", "UShort", "UInt", "ULong",
            -> true

            "Boolean" -> true

            "Char", "String" -> true

            "Array" -> true

            "Unit" -> true

            else -> false
        }
    }
}
