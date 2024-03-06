package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.expression.expressionparser.PointcutExpressionParser
import com.github.kitakkun.aspectk.expression.lexer.AspectKLexer
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasAnnotationOrInsideAnnotatedClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation

class AdviceOrPointcutFunctionChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        with(context) {
            verifyScope(declaration, reporter)
            verifyPointcutExpression(declaration, reporter)
        }
    }

    context(CheckerContext)
    private fun tryGetPointcutExpression(declaration: FirFunction): String? {
        return when {
            declaration.hasAnnotation(AspectKAnnotations.POINTCUT_CLASS_ID, session) -> {
                declaration.getAnnotationByClassId(AspectKAnnotations.POINTCUT_CLASS_ID, session)?.getStringArgument(AspectKAnnotations.POINTCUT_ARGUMENT_EXPRESSION_NAME)
            }

            declaration.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, session) -> {
                declaration.getAnnotationByClassId(AspectKAnnotations.BEFORE_CLASS_ID, session)?.getStringArgument(AspectKAnnotations.ADVICE_ARGUMENT_POINTCUT_NAME)
            }

            declaration.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, session) -> {
                declaration.getAnnotationByClassId(AspectKAnnotations.AFTER_CLASS_ID, session)?.getStringArgument(AspectKAnnotations.ADVICE_ARGUMENT_POINTCUT_NAME)
            }

            declaration.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, session) -> {
                declaration.getAnnotationByClassId(AspectKAnnotations.AROUND_CLASS_ID, session)?.getStringArgument(AspectKAnnotations.ADVICE_ARGUMENT_POINTCUT_NAME)
            }

            else -> null
        }
    }

    context(CheckerContext)
    private fun verifyScope(declaration: FirFunction, reporter: DiagnosticReporter) {
        val isInsideAspectClass = declaration.hasAnnotationOrInsideAnnotatedClass(AspectKAnnotations.ASPECT_CLASS_ID, session)
        if (isInsideAspectClass) return

        when {
            declaration.hasAnnotation(AspectKAnnotations.POINTCUT_CLASS_ID, session) -> {
                reporter.reportOn(declaration.source, AspectKErrors.POINTCUT_FUNCTION_DECLARATION_SCOPE_VIOLATION)
            }

            declaration.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, session) -> {
                reporter.reportOn(declaration.source, AspectKErrors.ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION, AspectKAnnotations.BEFORE_CLASS_ID.shortClassName.asString())
            }

            declaration.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, session) -> {
                reporter.reportOn(declaration.source, AspectKErrors.ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION, AspectKAnnotations.AFTER_CLASS_ID.shortClassName.asString())
            }

            declaration.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, session) -> {
                reporter.reportOn(declaration.source, AspectKErrors.ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION, AspectKAnnotations.AROUND_CLASS_ID.shortClassName.asString())
            }
        }
    }

    context(CheckerContext)
    private fun verifyPointcutExpression(declaration: FirFunction, reporter: DiagnosticReporter) {
        val pointcutExpression = tryGetPointcutExpression(declaration) ?: return

        if (pointcutExpression.isEmpty()) {
            reporter.reportOn(declaration.source, AspectKErrors.EMPTY_POINTCUT_EXPRESSION)
            return
        }

        try {
            val tokens = AspectKLexer(pointcutExpression).analyze()
            PointcutExpressionParser(tokens).expression()
        } catch (e: Throwable) {
            reporter.reportOn(declaration.source, AspectKErrors.INVALID_POINTCUT_EXPRESSION, e.message ?: "Unknown error")
        }
    }
}
