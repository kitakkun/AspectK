package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.PointcutExpression
import org.jetbrains.kotlin.name.ClassId

data class FunctionSpec(
    val packageName: String,
    val className: String,
    val functionName: String,
    val args: List<ClassId>,
    val returnType: ClassId,
    val modifiers: Set<FunctionModifier>,
    val lastArgumentIsVararg: Boolean,
)

class PointcutExpressionMatcher(private val expression: PointcutExpression) {
    fun matches(functionSpec: FunctionSpec): Boolean {
        when (expression) {
            is PointcutExpression.And -> {
                val left = PointcutExpressionMatcher(expression.left).matches(functionSpec)
                val right = PointcutExpressionMatcher(expression.right).matches(functionSpec)
                return left && right
            }

            is PointcutExpression.Or -> {
                val left = PointcutExpressionMatcher(expression.left).matches(functionSpec)
                val right = PointcutExpressionMatcher(expression.right).matches(functionSpec)
                return left || right
            }

            is PointcutExpression.Not -> {
                return !PointcutExpressionMatcher(expression.expression).matches(functionSpec)
            }

            is PointcutExpression.Execution -> {
                return ExecutionExpressionMatcher(expression).matches(
                    packageName = functionSpec.packageName,
                    className = functionSpec.className,
                    functionName = functionSpec.functionName,
                    argumentClassIds = functionSpec.args,
                    returnType = functionSpec.returnType,
                    modifiers = functionSpec.modifiers,
                    lastArgumentIsVararg = functionSpec.lastArgumentIsVararg,
                )
            }

            is PointcutExpression.Args -> {
                return ArgsExpressionMatcher(expression).matches(
                    valueParameterClassIds = functionSpec.args,
                    lastIsVarArg = functionSpec.lastArgumentIsVararg,
                )
            }

            else -> return false
        }
    }
}
