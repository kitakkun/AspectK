package com.github.kitakkun.aspectk.compiler.test.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class AspectKClasspathConfigurator(
    testServices: TestServices,
) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(
        configuration: CompilerConfiguration,
        module: TestModule,
    ) {
        val annotations = System.getProperty("aspectk.annotations.jar")
            ?: error("System property `aspectk.annotations.jar` is not set")
        val core = System.getProperty("aspectk.core.jar")
            ?: error("System property `aspectk.core.jar` is not set")
        configuration.addJvmClasspathRoot(File(annotations))
        configuration.addJvmClasspathRoot(File(core))
    }
}
