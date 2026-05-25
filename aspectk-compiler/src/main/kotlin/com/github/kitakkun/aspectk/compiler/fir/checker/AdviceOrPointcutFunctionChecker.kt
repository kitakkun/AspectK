package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.expression.PointcutExpression
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
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class AdviceOrPointcutFunctionChecker : FirFunctionChecker() {
    override fun check(
        declaration: FirFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        with(context) {
            verifyScope(declaration, reporter)
            verifyPointcutExpression(declaration, reporter)
        }
    }

    context(CheckerContext)
    private fun tryGetPointcutExpression(declaration: FirFunction): String? {
        return when {
            declaration.hasAnnotation(AspectKAnnotations.POINTCUT_CLASS_ID, session) -> {
                declaration.getAnnotationByClassId(
                    AspectKAnnotations.POINTCUT_CLASS_ID,
                    session,
                )?.getStringArgument(AspectKAnnotations.POINTCUT_ARGUMENT_EXPRESSION_NAME)
            }

            declaration.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, session) -> {
                declaration.getAnnotationByClassId(
                    AspectKAnnotations.BEFORE_CLASS_ID,
                    session,
                )?.getStringArgument(AspectKAnnotations.ADVICE_ARGUMENT_POINTCUT_NAME)
            }

            declaration.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, session) -> {
                declaration.getAnnotationByClassId(
                    AspectKAnnotations.AFTER_CLASS_ID,
                    session,
                )?.getStringArgument(AspectKAnnotations.ADVICE_ARGUMENT_POINTCUT_NAME)
            }

            declaration.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, session) -> {
                declaration.getAnnotationByClassId(
                    AspectKAnnotations.AROUND_CLASS_ID,
                    session,
                )?.getStringArgument(AspectKAnnotations.ADVICE_ARGUMENT_POINTCUT_NAME)
            }

            else -> null
        }
    }

    context(CheckerContext)
    private fun verifyScope(
        declaration: FirFunction,
        reporter: DiagnosticReporter,
    ) {
        val isInsideAspectClass = declaration.hasAnnotationOrInsideAnnotatedClass(AspectKAnnotations.ASPECT_CLASS_ID, session)
        if (isInsideAspectClass) return

        when {
            declaration.hasAnnotation(AspectKAnnotations.POINTCUT_CLASS_ID, session) -> {
                reporter.reportOn(declaration.source, AspectKErrors.POINTCUT_FUNCTION_DECLARATION_SCOPE_VIOLATION)
            }

            declaration.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, session) -> {
                reporter.reportOn(
                    declaration.source,
                    AspectKErrors.ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION,
                    AspectKAnnotations.BEFORE_CLASS_ID.shortClassName.asString(),
                )
            }

            declaration.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, session) -> {
                reporter.reportOn(
                    declaration.source,
                    AspectKErrors.ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION,
                    AspectKAnnotations.AFTER_CLASS_ID.shortClassName.asString(),
                )
            }

            declaration.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, session) -> {
                reporter.reportOn(
                    declaration.source,
                    AspectKErrors.ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION,
                    AspectKAnnotations.AROUND_CLASS_ID.shortClassName.asString(),
                )
            }
        }
    }

    context(CheckerContext)
    private fun verifyPointcutExpression(
        declaration: FirFunction,
        reporter: DiagnosticReporter,
    ) {
        val pointcutExpression = tryGetPointcutExpression(declaration) ?: return

        if (pointcutExpression.isEmpty()) {
            reporter.reportOn(declaration.source, AspectKErrors.EMPTY_POINTCUT_EXPRESSION)
            return
        }

        val parsed = try {
            val tokens = AspectKLexer(pointcutExpression).analyze()
            PointcutExpressionParser(tokens).expression()
        } catch (e: Throwable) {
            reporter.reportOn(declaration.source, AspectKErrors.INVALID_POINTCUT_EXPRESSION, e.message ?: "Unknown error")
            return
        }

        verifyNamedPointcutReferences(parsed, declaration, reporter)
    }

    context(CheckerContext)
    private fun verifyNamedPointcutReferences(
        expression: PointcutExpression,
        declaration: FirFunction,
        reporter: DiagnosticReporter,
    ) {
        collectNamedReferences(expression).forEach { named ->
            val classId = namedReferenceClassId(named)
            val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
            if (classSymbol == null) {
                reporter.reportOn(
                    declaration.source,
                    AspectKErrors.NAMED_POINTCUT_CLASS_NOT_FOUND,
                    classId.asFqNameString(),
                )
                return@forEach
            }

            val targetName = named.functionName.name
            val pointcutFn = classSymbol.declarationSymbols
                .filterIsInstance<FirNamedFunctionSymbol>()
                .firstOrNull {
                    it.name.asString() == targetName &&
                        it.hasAnnotation(AspectKAnnotations.POINTCUT_CLASS_ID, session)
                }
            if (pointcutFn == null) {
                reporter.reportOn(
                    declaration.source,
                    AspectKErrors.NAMED_POINTCUT_FUNCTION_NOT_FOUND,
                    "${classId.asFqNameString()}.$targetName",
                )
            }
        }
    }

    private fun collectNamedReferences(expression: PointcutExpression): List<PointcutExpression.Named> {
        val out = mutableListOf<PointcutExpression.Named>()
        fun walk(e: PointcutExpression) {
            when (e) {
                is PointcutExpression.Named -> out.add(e)
                is PointcutExpression.And -> { walk(e.left); walk(e.right) }
                is PointcutExpression.Or -> { walk(e.left); walk(e.right) }
                is PointcutExpression.Not -> walk(e.expression)
                is PointcutExpression.Empty,
                is PointcutExpression.Execution,
                is PointcutExpression.Args,
                -> Unit
            }
        }
        walk(expression)
        return out
    }

    private fun namedReferenceClassId(named: PointcutExpression.Named): ClassId {
        // Named.classId joins packages with "/" which is not a valid FqName component.
        // Rebuild with dot-separated package parts.
        val packageFqName = FqName(named.packageNames.joinToString(".") { it.name })
        val relativeClassName = FqName(named.classNames.joinToString(".") { it.name })
        return ClassId(packageFqName, relativeClassName, false)
    }
}
