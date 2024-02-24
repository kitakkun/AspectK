package com.github.kitakkun.aspectk.compiler.pointcut.matcher

import com.github.kitakkun.aspectk.compiler.pointcut.expression.ExecutionExpression
import com.github.kitakkun.aspectk.compiler.pointcut.expression.FunctionMatchingExpression
import com.github.kitakkun.aspectk.compiler.pointcut.expression.FunctionModifier
import org.jetbrains.kotlin.name.ClassId

class ExecutionExpressionMatcher(private val expression: ExecutionExpression) {
    fun matches(
        packageName: String,
        className: String,
        functionName: String,
        argumentClassIds: List<ClassId>,
        returnType: ClassId,
        modifiers: Set<FunctionModifier>,
        lastArgumentIsVararg: Boolean
    ): Boolean {
        with(expression.functionMatchingExpression) {
            // name matching
            if (!NameExpressionMatcher(name).matches(functionName)) return false
            // return type matching
            if (!TypeMatchingExpressionMatcher(returnTypeMatchingExpression).matches(returnType.packageFqName.asString(), returnType.relativeClassName.asString())) return false
            // argument matching
            if (!ArgumentExpressionMatcher(argumentExpression).matches(argumentClassIds, lastArgumentIsVararg)) return false
            // modifier matching
            if (this.modifiers.any { it !in modifiers }) return false

            when (this) {
                is FunctionMatchingExpression.ClassMethod -> {
                    val classMatcher = TypeMatchingExpressionMatcher(classMatchingExpression)
                    if (!classMatcher.matches(packageName, className)) return false
                }

                is FunctionMatchingExpression.ExtensionFunction -> {
                    val receiverMatcher = TypeMatchingExpressionMatcher(receiverType)
                    if (!receiverMatcher.matches(packageName, className)) return false
                }

                is FunctionMatchingExpression.TopLevelFunction -> {
                    // no-op
                }
            }
        }
        return true
    }
}
