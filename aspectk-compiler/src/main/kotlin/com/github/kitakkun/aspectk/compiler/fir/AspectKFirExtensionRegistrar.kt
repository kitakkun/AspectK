package com.github.kitakkun.aspectk.compiler.fir

import com.github.kitakkun.aspectk.compiler.fir.checker.AspectKFirCheckerExtension
import com.google.auto.service.AutoService
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

@AutoService(AspectKFirExtensionRegistrar::class)
class AspectKFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::AspectKFirCheckerExtension
    }
}
