package com.github.kitakkun.aspectk.expression

import com.github.kitakkun.aspectk.expression.expressionparser.ArgMatchingExpression
import org.jetbrains.kotlin.javac.resolve.classId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

sealed class PointcutExpression {
    data object Empty : PointcutExpression()

    data class And(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()

    data class Or(val left: PointcutExpression, val right: PointcutExpression) : PointcutExpression()

    data class Not(val expression: PointcutExpression) : PointcutExpression()

    sealed class Execution : PointcutExpression() {
        abstract val modifiers: List<FunctionModifier>
        abstract val packageNames: NameSequenceExpression
        abstract val functionName: NameExpression
        abstract val args: Args
        abstract val returnTypePackageNames: NameSequenceExpression
        abstract val returnTypeClassNames: NameSequenceExpression

        data class TopLevelFunction(
            override val modifiers: List<FunctionModifier>,
            override val packageNames: NameSequenceExpression,
            override val functionName: NameExpression,
            override val args: Args,
            override val returnTypePackageNames: NameSequenceExpression,
            override val returnTypeClassNames: NameSequenceExpression,
        ) : Execution()

        data class MemberFunction(
            override val modifiers: List<FunctionModifier>,
            override val packageNames: NameSequenceExpression,
            val classNames: NameSequenceExpression,
            override val functionName: NameExpression,
            override val args: Args,
            override val returnTypePackageNames: NameSequenceExpression,
            override val returnTypeClassNames: NameSequenceExpression,
            val includeSubClass: Boolean,
        ) : Execution()
    }

    data class Args(
        val args: List<ArgMatchingExpression>,
        val lastIsVarArg: Boolean,
    ) : PointcutExpression()

    data class Named(
        val packageNames: List<NameExpression.Normal>,
        val classNames: List<NameExpression.Normal>,
        val functionName: NameExpression.Normal,
    ) : PointcutExpression() {
        val name = "${packageNames.joinToString("/")}/${classNames.joinToString(".")}.$functionName"
        val classId: ClassId = classId(packageNames.joinToString("/"), classNames.joinToString("."))
        val callableId = CallableId(classId = classId, callableName = Name.identifier(functionName.name))
    }
}
