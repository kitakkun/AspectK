package com.github.kitakkun.aspectk.compiler.fir.checker

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

object AspectKDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {
    override val MAP =
        KtDiagnosticFactoryToRendererMap("AspectK").apply {
            put(
                factory = AspectKErrors.POINTCUT_FUNCTION_DECLARATION_SCOPE_VIOLATION,
                message = "The pointcut function annotated with @Pointcut must be a member of an @Aspect class",
            )
            put(
                factory = AspectKErrors.ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION,
                message = "The advice function annotated with @{0} must be a member of an @Aspect class",
                rendererA = TO_STRING,
            )
            put(
                factory = AspectKErrors.EMPTY_POINTCUT_EXPRESSION,
                message = "Empty pointcut expression will not match any join points",
            )
            put(
                factory = AspectKErrors.ASPECT_CLASS_WITH_NO_POINTCUT_OR_ADVICE_ENTRIES,
                message = "The @Aspect class doesn't have any pointcut or advice entries",
            )
            put(
                factory = AspectKErrors.INVALID_POINTCUT_EXPRESSION,
                message = "Invalid pointcut expression: {0}",
                rendererA = TO_STRING,
            )
        }
}
