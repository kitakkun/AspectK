package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.expressionparser.ArgMatchingExpression
import org.jetbrains.kotlin.name.ClassId

class ArgsExpressionMatcher(private val expression: PointcutExpression.Args) {
    fun matches(
        valueParameterClassIds: List<ClassId>,
        lastIsVarArg: Boolean,
    ): Boolean {
        if (lastIsVarArg != expression.lastIsVarArg) return false

        if (valueParameterClassIds.isEmpty()) {
            return expression.args.isEmpty() || expression.args.all { it is ArgMatchingExpression.NoneOrMore }
        }

        valueParameterClassIds.forEachIndexed { index, classId ->
            val argMatchingExpression = expression.args.getOrNull(index) ?: return false

            when (argMatchingExpression) {
                is ArgMatchingExpression.AnySingle -> return@forEachIndexed

                is ArgMatchingExpression.NoneOrMore -> {
                    if (matches(valueParameterClassIds.drop(1), lastIsVarArg)) return true
                    if (ArgsExpressionMatcher(
                            expression.copy(args = expression.args.drop(1)),
                        ).matches(valueParameterClassIds, lastIsVarArg = lastIsVarArg)
                    ) {
                        return true
                    }
                    return@forEachIndexed
                }

                is ArgMatchingExpression.Class -> {
                    val classMatcher = ClassMatcher(argMatchingExpression.packageNames, argMatchingExpression.classNames)
                    if (classMatcher.matches(classId.packageFqName.asString(), classId.relativeClassName.asString())) {
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
