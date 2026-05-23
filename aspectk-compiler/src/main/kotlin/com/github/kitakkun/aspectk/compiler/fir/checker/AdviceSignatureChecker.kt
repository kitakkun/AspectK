package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.AspectKConsts
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType

class AdviceSignatureChecker : FirFunctionChecker() {
    override fun check(
        declaration: FirFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        with(context) {
            val adviceAnnotationShortName = adviceAnnotationShortName(declaration) ?: return

            val params = declaration.valueParameters
            when {
                params.isEmpty() -> Unit

                params.size == 1 -> {
                    val paramType = params.single().returnTypeRef.coneType
                    if (paramType.classId != AspectKConsts.JOIN_POINT_CLASS_ID) {
                        reporter.reportOn(
                            declaration.source,
                            AspectKErrors.ADVICE_INVALID_SIGNATURE,
                            "@$adviceAnnotationShortName advice must take either no parameters or a single ${AspectKConsts.JOIN_POINT_CLASS_ID.shortClassName.asString()} parameter",
                        )
                    }
                }

                else -> {
                    reporter.reportOn(
                        declaration.source,
                        AspectKErrors.ADVICE_INVALID_SIGNATURE,
                        "@$adviceAnnotationShortName advice must take either no parameters or a single ${AspectKConsts.JOIN_POINT_CLASS_ID.shortClassName.asString()} parameter (got ${params.size} parameters)",
                    )
                }
            }
        }
    }

    context(CheckerContext)
    private fun adviceAnnotationShortName(declaration: FirFunction): String? {
        return when {
            declaration.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, session) ->
                AspectKAnnotations.BEFORE_CLASS_ID.shortClassName.asString()
            declaration.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, session) ->
                AspectKAnnotations.AFTER_CLASS_ID.shortClassName.asString()
            declaration.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, session) ->
                AspectKAnnotations.AROUND_CLASS_ID.shortClassName.asString()
            else -> null
        }
    }
}
