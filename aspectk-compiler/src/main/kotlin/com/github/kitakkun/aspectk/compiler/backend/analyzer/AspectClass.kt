package com.github.kitakkun.aspectk.compiler.backend.analyzer

import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.matcher.PointcutExpressionMatcher
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.ClassId

data class AspectClass(
    val classId: ClassId,
    val classDeclaration: IrClass,
    val pointcuts: List<Pointcut>,
    val advices: List<Advice>,
) {
    val classConstructor = classDeclaration.constructors.firstOrNull { it.isPrimary }
}

data class Pointcut(
    val name: String,
    val expression: PointcutExpression,
) {
    val matcher = PointcutExpressionMatcher(expression)
}

data class Advice(
    val type: AdviceType,
    val expression: PointcutExpression,
    val functionDeclaration: IrSimpleFunction,
) {
    val matcher = PointcutExpressionMatcher(expression)
}

enum class AdviceType {
    BEFORE,
    AFTER,
    AROUND,
}
