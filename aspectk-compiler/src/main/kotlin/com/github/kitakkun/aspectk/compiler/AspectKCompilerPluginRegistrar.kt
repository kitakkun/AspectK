package com.github.kitakkun.aspectk.compiler

import com.github.kitakkun.aspectk.compiler.backend.AspectKIrGenerationExtension
import com.github.kitakkun.aspectk.compiler.fir.AspectKFirExtensionRegistrar
import com.github.kitakkun.aspectk.plugin.common.AspectKPluginConsts
import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
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

        val reportDir = configuration[AspectKCompilerConfigurationKey.REPORT_DIR]
        val aspectIndexFile = configuration[AspectKCompilerConfigurationKey.ASPECT_INDEX_FILE]
        // Snapshot the JVM classpath so the IR extension can scan
        // META-INF/aspectk/aspects.txt entries inside dependency JARs and
        // exploded class directories. We do this at registrar time rather
        // than inside the IR extension because the CompilerConfiguration
        // isn't exposed by IrPluginContext. SDK roots (bootclasspath etc.)
        // are skipped — only user dependencies can carry AspectK markers.
        val classpathRoots = configuration.get(CLIConfigurationKeys.CONTENT_ROOTS).orEmpty()
            .filterIsInstance<JvmClasspathRoot>()
            .filterNot { it.isSdkRoot }
            .map { it.file }
        FirExtensionRegistrarAdapter.registerExtension(AspectKFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(
            AspectKIrGenerationExtension(
                reportDir = reportDir,
                aspectIndexFile = aspectIndexFile,
                classpathRoots = classpathRoots,
            ),
        )
    }
}
