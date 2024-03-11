package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.model.FunctionSpec

class ExecutionExpressionMatcher(private val expression: PointcutExpression.Execution) {
    fun matches(functionSpec: FunctionSpec): Boolean {
        // function name matching
        if (!NameExpressionMatcher(expression.functionName).matches(functionSpec.functionName)) return false

        // modifier matching
        if (expression.modifiers.isEmpty() && !functionSpec.modifiers.contains(FunctionModifier.PUBLIC)) {
            return false
        } else if (expression.modifiers.any { it !in functionSpec.modifiers }) {
            return false
        }

        // argument matching
        val argsMatcher = ArgsExpressionMatcher(expression.args)
        if (!argsMatcher.matches(functionSpec.args, functionSpec.lastArgumentIsVararg)) return false

        // matching package and class
        when (expression) {
            is PointcutExpression.Execution.MemberFunction -> {
                if (functionSpec !is FunctionSpec.Member) {
                    return false
                }
                if (!ClassSignatureMatcher(expression.classSignature).matches(functionSpec.classSignature)) {
                    return false
                }
            }

            is PointcutExpression.Execution.TopLevelFunction -> {
                if (functionSpec !is FunctionSpec.TopLevel) {
                    return false
                }
                if (!NameSequenceExpressionMatcher(expression.packageNames).matches(functionSpec.packageName)) {
                    return false
                }
            }
        }

        return ClassSignatureMatcher(expression.returnType).matches(functionSpec.returnType)
    }
}
