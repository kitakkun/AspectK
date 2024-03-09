package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.NameExpression
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

        // matching dispatcher class
        when (expression) {
            is PointcutExpression.Execution.MemberFunction -> {
                if (!ClassMatcher(expression.packageNames, expression.classNames).matches(functionSpec.packageName, functionSpec.className)) {
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
            val packageMatch = expression.returnTypePackageNames.isEmpty() || expression.returnTypePackageNames.singleOrNull() == NameExpression.fromString("kotlin")
            val classMatch = expression.returnTypeClassNames.isEmpty() || expression.returnTypeClassNames.singleOrNull() == NameExpression.fromString("Unit")

            return packageMatch && classMatch
        }

        // normal matching return type
        return ClassMatcher(expression.returnTypePackageNames, expression.returnTypeClassNames).matches(
            packageName = functionSpec.returnType.packageFqName.asString(),
            className = functionSpec.returnType.relativeClassName.asString(),
        )
    }
}
