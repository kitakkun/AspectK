package com.github.kitakkun.aspectk.compiler

import com.github.kitakkun.aspectk.compiler.backend.AspectKIrGenerationExtension
import com.github.kitakkun.aspectk.compiler.fir.AspectKFirExtensionRegistrar
import com.github.kitakkun.aspectk.plugin.common.AspectKPluginConsts
import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@Suppress("UNUSED")
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class AspectKCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = AspectKPluginConsts.PLUGIN_ID
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val enabled = configuration[AspectKCompilerConfigurationKey.ENABLED] ?: false
        if (!enabled) return

        FirExtensionRegistrarAdapter.registerExtension(AspectKFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(AspectKIrGenerationExtension())
    }
}
