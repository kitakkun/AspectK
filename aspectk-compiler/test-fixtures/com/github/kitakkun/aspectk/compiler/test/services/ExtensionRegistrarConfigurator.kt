package com.github.kitakkun.aspectk.compiler.test.services

import com.github.kitakkun.aspectk.compiler.backend.AspectKIrGenerationExtension
import com.github.kitakkun.aspectk.compiler.fir.AspectKFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

@OptIn(ExperimentalCompilerApi::class)
class ExtensionRegistrarConfigurator(
    testServices: TestServices,
) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        FirExtensionRegistrarAdapter.registerExtension(AspectKFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(AspectKIrGenerationExtension(reportDir = null))
    }
}
