package com.github.kitakkun.aspectk.compiler.backend.pointcut.matcher

import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.matcher.ArgumentExpressionMatcher
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.isVararg

class IrSimpleFunctionArgumentPointcutMatcher(argumentExpression: PointcutExpression.Args) : PointcutMatcher<IrSimpleFunction> {
    private val matcher = ArgumentExpressionMatcher(argumentExpression)

    override fun matches(target: IrSimpleFunction): Boolean {
        val valueParameterClassIds = target.valueParameters.map { it.type.classOrFail.owner.classId }
        // FIXME: What may cause null classId?
        if (valueParameterClassIds.any { it == null }) error("ClassId is null")
        val lastIsVarArg = target.valueParameters.lastOrNull()?.isVararg ?: false
        return matcher.matches(
            valueParameterClassIds = valueParameterClassIds.filterNotNull(),
            lastIsVarArg = lastIsVarArg,
        )
    }
}
