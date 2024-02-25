package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.ArgumentMatchingExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import org.jetbrains.kotlin.name.ClassId

class ArgumentExpressionMatcher(private val argumentExpression: PointcutExpression.Args) {
    fun matches(valueParameterClassIds: List<ClassId>, lastIsVarArg: Boolean): Boolean {
        if (valueParameterClassIds.isEmpty()) {
            return argumentExpression.args.isEmpty() || argumentExpression.args.all { it is ArgumentMatchingExpression.AnyZeroOrMore }
        }

        var argMatchingExpressionCursor = 0

        valueParameterClassIds.forEachIndexed { index, classId ->
            val argMatchingExpression = argumentExpression.args.getOrNull(argMatchingExpressionCursor) ?: return false

            when (argMatchingExpression) {
                is ArgumentMatchingExpression.AnySingle -> {
                    argMatchingExpressionCursor++
                }

                is ArgumentMatchingExpression.AnyZeroOrMore -> {
                    if (matches(valueParameterClassIds.drop(1), lastIsVarArg)) return true
                    argMatchingExpressionCursor++
                }

                is ArgumentMatchingExpression.Vararg -> {
                    // FIXME: generate matcher instance here may cause performance issue
                    val typeMatcher = TypeMatchingExpressionMatcher(argMatchingExpression.expression)
                    return (typeMatcher.matches(classId.packageFqName.asString(), classId.relativeClassName.asString()))
                        && (index == valueParameterClassIds.size - 1)
                        && lastIsVarArg
                }

                is ArgumentMatchingExpression.Type -> {
                    // FIXME: generate matcher instance here may cause performance issue
                    val typeMatcher = TypeMatchingExpressionMatcher(argMatchingExpression.expression)
                    if (typeMatcher.matches(classId.packageFqName.asString(), classId.relativeClassName.asString())) {
                        argMatchingExpressionCursor++
                    } else {
                        return false
                    }
                }
            }
        }

        return (argMatchingExpressionCursor == argumentExpression.args.size) ||
            (argumentExpression.args.drop(argMatchingExpressionCursor).all { it is ArgumentMatchingExpression.AnyZeroOrMore })
    }
}
