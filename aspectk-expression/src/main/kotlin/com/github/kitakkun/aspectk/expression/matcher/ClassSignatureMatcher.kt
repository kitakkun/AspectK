package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.ClassSignatureExpression
import com.github.kitakkun.aspectk.expression.NameSequenceExpression
import com.github.kitakkun.aspectk.expression.model.ClassSignature

class ClassSignatureMatcher(private val classSignatureExpression: ClassSignatureExpression) {
    fun matches(classSignature: ClassSignature): Boolean {
        when (classSignatureExpression) {
            is ClassSignatureExpression.Normal -> {
                // support for implicit type matching for kotlin primitives
                if (classSignature.packageName == "kotlin" && classSignatureExpression.packageNames is NameSequenceExpression.Empty) {
                    if (isKotlinPrimitive(classSignature.className)) return true
                }

                val packageNameMatcher = NameSequenceExpressionMatcher(classSignatureExpression.packageNames)
                val classSequenceMatcher = NameSequenceExpressionMatcher(classSignatureExpression.classNames)
                return packageNameMatcher.matches(classSignature.packageName) && classSequenceMatcher.matches(classSignature.className)
            }

            is ClassSignatureExpression.IncludingSubClass -> {
                val packageNameMatcher = NameSequenceExpressionMatcher(classSignatureExpression.packageNames)
                val classSequenceMatcher = NameSequenceExpressionMatcher(classSignatureExpression.classNames)

                if (packageNameMatcher.matches(classSignature.packageName) && classSequenceMatcher.matches(classSignature.className)) {
                    return true
                }

                return classSignature.superTypes.any { packageNameMatcher.matches(it.packageName) && classSequenceMatcher.matches(it.className) }
            }

            is ClassSignatureExpression.Any -> {
                return true
            }
        }
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
