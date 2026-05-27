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
 * Validates binding annotations on advice value parameters.
 *
 * Two distinct checks run per parameter:
 *
 * 1. **Multiple binding annotations on the same parameter** — at most one
 *    of `@DispatchReceiver` / `@ExtensionReceiver` / `@ContextParameter` /
 *    `@ValueParameter` / `@ContextParameters` / `@ValueParameters` may be
 *    present. More than one is ambiguous and would otherwise lead the
 *    analyzer to silently pick the first one in lookup order.
 * 2. **Index/name xor** — `@ValueParameter` and `@ContextParameter` carry
 *    `index: Int = -1` and `name: String = ""`. Exactly one must be set;
 *    the default-both and both-set forms are rejected so the user gets a
 *    clear diagnostic rather than a silently-mismatching binding at IR time.
 *
 * `@DispatchReceiver`, `@ExtensionReceiver`, `@ValueParameters`, and
 * `@ContextParameters` take no arguments and are not subject to the second
 * check.
 */
object BindingAnnotationChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    private val allBindingAnnotations = listOf(
        AspectKAnnotations.DISPATCH_RECEIVER_CLASS_ID to "DispatchReceiver",
        AspectKAnnotations.EXTENSION_RECEIVER_CLASS_ID to "ExtensionReceiver",
        AspectKAnnotations.CONTEXT_PARAMETER_CLASS_ID to "ContextParameter",
        AspectKAnnotations.VALUE_PARAMETER_CLASS_ID to "ValueParameter",
        AspectKAnnotations.CONTEXT_PARAMETERS_CLASS_ID to "ContextParameters",
        AspectKAnnotations.VALUE_PARAMETERS_CLASS_ID to "ValueParameters",
    )

    private val indexOrNameAnnotations = listOf(
        AspectKAnnotations.VALUE_PARAMETER_CLASS_ID to "ValueParameter",
        AspectKAnnotations.CONTEXT_PARAMETER_CLASS_ID to "ContextParameter",
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        for (param in declaration.valueParameters) {
            val present = allBindingAnnotations.mapNotNull { (classId, displayName) ->
                param.getAnnotationByClassId(classId, context.session)?.let { it to displayName }
            }
            if (present.size >= 2) {
                val firstAnnotation = present[0].first
                val src = firstAnnotation.source ?: param.source ?: continue
                val names = present.joinToString(" / ") { "@${it.second}" }
                reporter.reportOn(
                    src,
                    AspectKErrors.INVALID_BINDING_ANNOTATION,
                    present[0].second,
                    "at most one binding annotation per parameter (found $names)",
                )
                continue
            }
            for ((classId, displayName) in indexOrNameAnnotations) {
                val annotation = param.getAnnotationByClassId(classId, context.session) ?: continue
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
