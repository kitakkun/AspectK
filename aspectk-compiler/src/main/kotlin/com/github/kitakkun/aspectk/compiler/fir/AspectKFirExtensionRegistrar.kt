package com.github.kitakkun.aspectk.compiler.fir

import com.github.kitakkun.aspectk.compiler.fir.checker.AspectKErrors
import com.github.kitakkun.aspectk.compiler.fir.checker.AspectKFirCheckerExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class AspectKFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::AspectKFirCheckerExtension
        registerDiagnosticContainers(AspectKErrors)
    }
}
