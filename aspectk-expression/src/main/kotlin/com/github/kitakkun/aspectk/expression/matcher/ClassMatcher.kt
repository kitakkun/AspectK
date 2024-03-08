package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameExpression

class ClassMatcher(
    packageNameExpressions: List<NameExpression>,
    classNameExpressions: List<NameExpression>,
) {
    private val packageSequenceMatcher = NameExpressionSequenceMatcher(packageNameExpressions)
    private val classSequenceMatcher = NameExpressionSequenceMatcher(classNameExpressions)
    private val emptyPackageExpressions = packageNameExpressions.isEmpty()

    fun matches(
        packageName: String,
        className: String,
    ): Boolean {
        if (!classSequenceMatcher.matches(className.split(".").filter { it.isNotEmpty() })) return false

        if (packageName == "kotlin" && emptyPackageExpressions) {
            if (isKotlinPrimitive(className)) return true
        }

        return packageSequenceMatcher.matches(packageName.split(".", "/").filter { it.isNotEmpty() })
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
