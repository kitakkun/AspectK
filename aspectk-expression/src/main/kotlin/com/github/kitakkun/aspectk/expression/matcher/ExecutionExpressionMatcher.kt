package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import org.jetbrains.kotlin.javac.resolve.classId
import org.jetbrains.kotlin.name.ClassId

class ExecutionExpressionMatcher(private val expression: PointcutExpression.Execution) {
    fun matches(
        packageName: String,
        className: String,
        functionName: String,
        argumentClassIds: List<ClassId>,
        returnType: ClassId,
        modifiers: Set<FunctionModifier>,
        lastArgumentIsVararg: Boolean,
    ): Boolean {
        // function name matching
        if (!NameExpressionMatcher(expression.functionName).matches(functionName)) return false

        // modifier matching
        if (expression.modifiers.isEmpty() && !modifiers.contains(FunctionModifier.PUBLIC)) {
            return false
        } else if (expression.modifiers.any { it !in modifiers }) {
            return false
        }

        // argument matching
        val argsMatcher = ArgsExpressionMatcher(expression.args)
        if (!argsMatcher.matches(argumentClassIds, lastArgumentIsVararg)) return false

        // matching dispatcher class
        if (!ClassMatcher(expression.packageNames, expression.classNames).matches(packageName, className)) {
            return false
        }

        // implicit return type matching support
        // FIXME: temporary solution
        if (returnType == classId("kotlin", "Unit")) {
            val packageMatch = expression.returnTypePackageNames.isEmpty() || expression.returnTypePackageNames.singleOrNull() == NameExpression.fromString("kotlin")
            val classMatch = expression.returnTypeClassNames.isEmpty() || expression.returnTypeClassNames.singleOrNull() == NameExpression.fromString("Unit")

            return packageMatch && classMatch
        }

        // normal matching return type
        return ClassMatcher(expression.returnTypePackageNames, expression.returnTypeClassNames).matches(
            packageName = returnType.packageFqName.asString(),
            className = returnType.relativeClassName.asString(),
        )
    }
}
