package com.github.kitakkun.aspectk.compiler

import com.github.kitakkun.aspectk.plugin.common.AspectKPluginConsts
import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@Suppress("UNUSED")
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class AspectKCommandLineProcessor : CommandLineProcessor {
    override val pluginId = AspectKPluginConsts.PLUGIN_ID

    override val pluginOptions =
        listOf(
            CliOption(
                optionName = "enabled",
                description = "Enable AspectK plugin or not",
                valueDescription = "<true|false>",
                allowMultipleOccurrences = false,
                required = false,
            ),
        )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            "enabled" -> configuration.put(AspectKCompilerConfigurationKey.ENABLED, value.toBoolean())
            else -> error("Unexpected config option ${option.optionName}")
        }
    }
}
