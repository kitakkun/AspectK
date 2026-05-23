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
import org.jetbrains.kotlin.name.ClassId

class AdviceSignatureChecker : FirFunctionChecker() {
    override fun check(
        declaration: FirFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        with(context) {
            val adviceKind = adviceKind(declaration) ?: return
            val allowedJoinPointTypes = adviceKind.allowedJoinPointTypes()
            val allowedNames = allowedJoinPointTypes.joinToString(" or ") { it.shortClassName.asString() }

            val params = declaration.valueParameters
            when {
                params.isEmpty() -> Unit

                params.size == 1 -> {
                    val paramType = params.single().returnTypeRef.coneType
                    if (paramType.classId !in allowedJoinPointTypes) {
                        reporter.reportOn(
                            declaration.source,
                            AspectKErrors.ADVICE_INVALID_SIGNATURE,
                            "@${adviceKind.shortName} advice must take either no parameters or a single $allowedNames parameter",
                        )
                    }
                }

                else -> {
                    reporter.reportOn(
                        declaration.source,
                        AspectKErrors.ADVICE_INVALID_SIGNATURE,
                        "@${adviceKind.shortName} advice must take either no parameters or a single $allowedNames parameter (got ${params.size} parameters)",
                    )
                }
            }
        }
    }

    private enum class AdviceKind(val shortName: String) {
        BEFORE("Before"),
        AFTER("After"),
        AROUND("Around"),
        ;

        fun allowedJoinPointTypes(): Set<ClassId> = when (this) {
            BEFORE, AFTER -> setOf(AspectKConsts.JOIN_POINT_CLASS_ID)
            AROUND -> setOf(AspectKConsts.JOIN_POINT_CLASS_ID, AspectKConsts.PROCEEDING_JOIN_POINT_CLASS_ID)
        }
    }

    context(CheckerContext)
    private fun adviceKind(declaration: FirFunction): AdviceKind? {
        return when {
            declaration.hasAnnotation(AspectKAnnotations.BEFORE_CLASS_ID, session) -> AdviceKind.BEFORE
            declaration.hasAnnotation(AspectKAnnotations.AFTER_CLASS_ID, session) -> AdviceKind.AFTER
            declaration.hasAnnotation(AspectKAnnotations.AROUND_CLASS_ID, session) -> AdviceKind.AROUND
            else -> null
        }
    }
}
