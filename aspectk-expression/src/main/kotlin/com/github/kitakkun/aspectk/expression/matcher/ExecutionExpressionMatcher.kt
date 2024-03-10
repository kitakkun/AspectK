package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.model.ClassSignature

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

        // matching class
        when (expression) {
            is PointcutExpression.Execution.MemberFunction -> {
                // FIXME: super type matching is not supported yet
                val classSignature = ClassSignature(packageName = functionSpec.packageName, className = functionSpec.className, superTypes = emptyList())
                if (!ClassSignatureMatcher(expression.classSignature).matches(classSignature)) {
                    return false
                }
            }

            is PointcutExpression.Execution.TopLevelFunction -> {
                if (!NameSequenceExpressionMatcher(expression.packageNames).matches(functionSpec.packageName)) {
                    return false
                }
                if (functionSpec.className.isNotEmpty()) {
                    return false
                }
            }
        }

        val returnTypeSignature = ClassSignature(packageName = functionSpec.returnType.packageName, className = functionSpec.returnType.className, superTypes = emptyList())
        return ClassSignatureMatcher(expression.returnType).matches(returnTypeSignature)
    }
}
