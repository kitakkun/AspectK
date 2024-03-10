package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.NameSequenceExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import org.jetbrains.kotlin.javac.resolve.classId

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

        // matching package
        if (!NameSequenceExpressionMatcher(expression.packageNames).matches(functionSpec.packageName)) {
            return false
        }

        // matching class
        when (expression) {
            is PointcutExpression.Execution.MemberFunction -> {
                if (!NameSequenceExpressionMatcher(expression.classNames).matches(functionSpec.className)) {
                    return false
                }
            }

            is PointcutExpression.Execution.TopLevelFunction -> {
                if (functionSpec.className.isNotEmpty()) {
                    return false
                }
            }
        }

        // implicit return type matching support
        // FIXME: temporary solution
        if (functionSpec.returnType == classId("kotlin", "Unit")) {
            val unspecifiedPackage = expression.returnTypePackageNames is NameSequenceExpression.Empty
            val unspecifiedReturnType = unspecifiedPackage && expression.returnTypeClassNames is NameSequenceExpression.Empty
            if (unspecifiedReturnType || unspecifiedPackage && (expression.returnTypeClassNames as? NameSequenceExpression.Sequence)?.names?.singleOrNull() == NameExpression.fromString("Unit")) {
                return true
            }
        }

        return ClassMatcher(expression.returnTypePackageNames, expression.returnTypeClassNames)
            .matches(functionSpec.returnType.packageFqName.asString(), functionSpec.returnType.relativeClassName.asString())
    }
}
