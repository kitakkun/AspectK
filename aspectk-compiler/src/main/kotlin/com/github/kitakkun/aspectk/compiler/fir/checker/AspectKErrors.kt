package com.github.kitakkun.aspectk.compiler.fir.checker

import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction

object AspectKErrors {
    val POINTCUT_FUNCTION_DECLARATION_SCOPE_VIOLATION by error0<KtFunction>()
    val ADVICE_FUNCTION_DECLARATION_SCOPE_VIOLATION by error1<KtFunction, String>()
    val EMPTY_POINTCUT_EXPRESSION by warning0<KtFunction>()
    val ASPECT_CLASS_WITH_NO_POINTCUT_OR_ADVICE_ENTRIES by warning0<KtClass>()
    val INVALID_POINTCUT_EXPRESSION by error1<KtFunction, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(AspectKDiagnosticRendererFactory)
    }
}
