package com.github.kitakkun.aspectk.compiler

import com.github.kitakkun.aspectk.compiler.backend.AspectKIrGenerationExtension
import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration


@Suppress("UNUSED")
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class AspectKCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val enabled = configuration[AspectKCompilerConfigurationKey.ENABLED] ?: false
        if (!enabled) return

        IrGenerationExtension.registerExtension(AspectKIrGenerationExtension())
    }
}
