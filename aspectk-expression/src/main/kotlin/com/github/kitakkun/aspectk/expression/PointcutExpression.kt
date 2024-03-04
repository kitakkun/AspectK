package com.github.kitakkun.aspectk.expression

import com.github.kitakkun.aspectk.expression.expressionparser.ArgMatchingExpression
import org.jetbrains.kotlin.javac.resolve.classId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

sealed class PointcutExpression {
    data class And(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()
    data class Or(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()
    data class Not(val expression: PointcutExpression) : PointcutExpression()

    data class Execution(
        val modifiers: List<FunctionModifier>,
        val packageNames: List<NameExpression>,
        val classNames: List<NameExpression>,
        val functionName: NameExpression,
        val args: Args,
        val returnTypePackageNames: List<NameExpression>,
        val returnTypeClassNames: List<NameExpression>,
    ) : PointcutExpression()

    data class Args(
        val args: List<ArgMatchingExpression>,
        val lastIsVarArg: Boolean,
    ) : PointcutExpression()

    data class Named(
        val packageNames: List<NameExpression.Normal>,
        val classNames: List<NameExpression.Normal>,
        val functionName: NameExpression.Normal,
    ) : PointcutExpression() {
        val name = "${packageNames.joinToString("/")}/${classNames.joinToString(".")}.${functionName}"
        val classId: ClassId = classId(packageNames.joinToString("/"), classNames.joinToString("."))
        val callableId = CallableId(classId = classId, callableName = Name.identifier(functionName.name))
    }
}
