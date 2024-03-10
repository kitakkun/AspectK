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
    fun matches(
        functionSpec: FunctionSpec,
        namedPointcutResolver: (PointcutExpression.Named) -> PointcutExpression?,
    ): Boolean {
        when (expression) {
            is PointcutExpression.Empty -> {
                return false
            }

            is PointcutExpression.And -> {
                val left = PointcutExpressionMatcher(expression.left).matches(functionSpec, namedPointcutResolver)
                val right = PointcutExpressionMatcher(expression.right).matches(functionSpec, namedPointcutResolver)
                return left && right
            }

            is PointcutExpression.Or -> {
                val left = PointcutExpressionMatcher(expression.left).matches(functionSpec, namedPointcutResolver)
                val right = PointcutExpressionMatcher(expression.right).matches(functionSpec, namedPointcutResolver)
                return left || right
            }

            is PointcutExpression.Not -> {
                return !PointcutExpressionMatcher(expression.expression).matches(functionSpec, namedPointcutResolver)
            }

            is PointcutExpression.Execution -> {
                return ExecutionExpressionMatcher(expression).matches(functionSpec = functionSpec)
            }

            is PointcutExpression.Args -> {
                return ArgsExpressionMatcher(expression).matches(
                    valueParameterClassIds = functionSpec.args,
                    lastIsVarArg = functionSpec.lastArgumentIsVararg,
                )
            }

            is PointcutExpression.Named -> {
                val correspondingExpression = namedPointcutResolver(expression) ?: throw IllegalStateException("Named pointcut ${expression.name} is not found.")
                return PointcutExpressionMatcher(correspondingExpression).matches(functionSpec, namedPointcutResolver)
            }
        }
    }
}
