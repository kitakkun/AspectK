package com.github.kitakkun.aspectk.compiler.pointcut.matcher

import com.github.kitakkun.aspectk.compiler.pointcut.expression.ArgumentExpression
import com.github.kitakkun.aspectk.compiler.pointcut.expression.ArgumentMatchingExpression
import org.jetbrains.kotlin.name.ClassId

class ArgumentExpressionMatcher(private val argumentExpression: ArgumentExpression) {
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
                    return classId == argMatchingExpression.classId && index == valueParameterClassIds.size - 1 && lastIsVarArg
                }

                is ArgumentMatchingExpression.Type -> {
                    if (classId == argMatchingExpression.classId) {
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
