package com.github.kitakkun.aspectk.compiler.fir.checker

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId

/**
 * Validates the indexed-or-named binding annotations on advice value parameters.
 *
 * `@ValueParameter` and `@ContextParameter` each declare both `index: Int = -1`
 * and `name: String = ""`. Exactly one must be set: `index >= 0` xor
 * `name.isNotEmpty()`. The default-both and both-set forms are rejected here
 * so the user gets a clear diagnostic rather than a silently-mismatching
 * binding at IR time.
 *
 * `@DispatchReceiver` and `@ExtensionReceiver` take no arguments and are not
 * checked here.
 */
object BindingAnnotationChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    private val annotationsToValidate = listOf(
        AspectKAnnotations.VALUE_PARAMETER_CLASS_ID to "ValueParameter",
        AspectKAnnotations.CONTEXT_PARAMETER_CLASS_ID to "ContextParameter",
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        for (param in declaration.valueParameters) {
            for ((classId, displayName) in annotationsToValidate) {
                val annotation = param.getAnnotationByClassId(classId, context.session) ?: continue
                // Presence-based check: the user must pass exactly one of `index` / `name`.
                // Default values (`index = -1`, `name = ""`) leave the corresponding key
                // out of the resolved argument mapping.
                val hasIndex = AspectKAnnotations.INDEX in annotation.argumentMapping.mapping
                val hasName = AspectKAnnotations.NAME in annotation.argumentMapping.mapping
                val message = when {
                    !hasIndex && !hasName -> "specify one of index or name"
                    hasIndex && hasName -> "specify only one of index or name, not both"
                    else -> continue
                }
                val src = annotation.source ?: param.source ?: continue
                reporter.reportOn(
                    src,
                    AspectKErrors.INVALID_BINDING_ANNOTATION,
                    displayName,
                    message,
                )
            }
        }
    }
}
