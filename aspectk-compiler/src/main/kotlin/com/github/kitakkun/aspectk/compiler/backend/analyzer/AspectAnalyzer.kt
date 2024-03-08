package com.github.kitakkun.aspectk.compiler.backend.analyzer

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.expression.expressionparser.PointcutExpressionParser
import com.github.kitakkun.aspectk.expression.lexer.AspectKLexer
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class AspectAnalyzer private constructor() : IrElementVisitorVoid {
    companion object {
        fun analyze(moduleFragment: IrModuleFragment): List<AspectClass> {
            with(AspectAnalyzer()) {
                moduleFragment.acceptChildrenVoid(this)
                return aspectClassList
            }
        }
    }

    private val mutableAspectClassList = mutableListOf<AspectClass>()
    val aspectClassList: List<AspectClass> get() = mutableAspectClassList

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation(AspectKAnnotations.ASPECT_FQ_NAME)) {
            val aspectClass = analyzeClass(declaration)
            mutableAspectClassList.add(aspectClass)
        }
        declaration.acceptChildrenVoid(this)
    }

    private fun analyzeClass(declaration: IrClass): AspectClass {
        val classId = declaration.classId ?: error("ClassId is null")

        val pointcuts =
            declaration.simpleFunctions()
                .filter { it.hasAnnotation(AspectKAnnotations.POINTCUT_FQ_NAME) }
                .map { it.name.asString() to it.getAnnotation(AspectKAnnotations.POINTCUT_FQ_NAME)?.getStringConstArgument(0) }
                .mapNotNull { (name, expression) ->
                    val pointcutExpression =
                        expression?.let {
                            val tokens = AspectKLexer(it).analyze()
                            PointcutExpressionParser(tokens).expression()
                        }
                    if (pointcutExpression != null) {
                        Pointcut(name, pointcutExpression)
                    } else {
                        null
                    }
                }

        val beforeAdvices =
            declaration.simpleFunctions()
                .filter { it.hasAnnotation(AspectKAnnotations.BEFORE_FQ_NAME) }
                .associateWith { it.getAnnotation(AspectKAnnotations.BEFORE_FQ_NAME)?.getStringConstArgument(0) }
                .mapNotNull { (declaration, expression) ->
                    val pointcutExpression =
                        expression?.let {
                            val tokens = AspectKLexer(it).analyze()
                            PointcutExpressionParser(tokens).expression()
                        }
                    if (pointcutExpression != null) {
                        Advice(AdviceType.BEFORE, pointcutExpression, declaration)
                    } else {
                        null
                    }
                }

        val afterAdvices =
            declaration.simpleFunctions()
                .filter { it.hasAnnotation(AspectKAnnotations.AFTER_FQ_NAME) }
                .associateWith { it.getAnnotation(AspectKAnnotations.AFTER_FQ_NAME)?.getStringConstArgument(0) }
                .mapNotNull { (declaration, expression) ->
                    val pointcutExpression =
                        expression?.let {
                            val tokens = AspectKLexer(it).analyze()
                            PointcutExpressionParser(tokens).expression()
                        }
                    if (pointcutExpression != null) {
                        Advice(AdviceType.AFTER, pointcutExpression, declaration)
                    } else {
                        null
                    }
                }

        val aroundAdvices =
            declaration.simpleFunctions()
                .filter { it.hasAnnotation(AspectKAnnotations.AROUND_FQ_NAME) }
                .associateWith { it.getAnnotation(AspectKAnnotations.AROUND_FQ_NAME)?.getStringConstArgument(0) }
                .mapNotNull { (declaration, expression) ->
                    val pointcutExpression =
                        expression?.let {
                            val tokens = AspectKLexer(it).analyze()
                            PointcutExpressionParser(tokens).expression()
                        }
                    if (pointcutExpression != null) {
                        Advice(AdviceType.AROUND, pointcutExpression, declaration)
                    } else {
                        null
                    }
                }

        val advices = beforeAdvices + afterAdvices + aroundAdvices

        return AspectClass(
            classId = classId,
            pointcuts = pointcuts,
            advices = advices,
            classDeclaration = declaration,
        )
    }
}
