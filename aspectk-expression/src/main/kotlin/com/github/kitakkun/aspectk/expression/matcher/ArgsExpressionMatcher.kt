package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.expressionparser.ArgMatchingExpression
import com.github.kitakkun.aspectk.expression.model.ClassSignature

class ArgsExpressionMatcher(private val expression: PointcutExpression.Args) {
    fun matches(
        valueParameterClassSignatures: List<ClassSignature>,
        lastIsVarArg: Boolean,
    ): Boolean {
        if (lastIsVarArg != expression.lastIsVarArg) return false

        if (valueParameterClassSignatures.isEmpty()) {
            return expression.args.isEmpty() || expression.args.all { it is ArgMatchingExpression.NoneOrMore }
        }

        valueParameterClassSignatures.forEachIndexed { index, classSignature ->
            val argMatchingExpression = expression.args.getOrNull(index) ?: return false

            when (argMatchingExpression) {
                is ArgMatchingExpression.AnySingle -> return@forEachIndexed

                is ArgMatchingExpression.NoneOrMore -> {
                    if (matches(valueParameterClassSignatures.drop(1), lastIsVarArg)) return true
                    if (ArgsExpressionMatcher(
                            expression.copy(args = expression.args.drop(1)),
                        ).matches(valueParameterClassSignatures, lastIsVarArg = lastIsVarArg)
                    ) {
                        return true
                    }
                    return@forEachIndexed
                }

                is ArgMatchingExpression.Class -> {
                    val classSignatureMatcher = ClassSignatureMatcher(argMatchingExpression.expression)
                    if (classSignatureMatcher.matches(classSignature)) {
                        return@forEachIndexed
                    } else {
                        return false
                    }
                }
            }
        }

        return true
    }
}
