package com.github.kitakkun.aspectk.compiler.test.runners

import com.github.kitakkun.aspectk.compiler.test.services.AspectKClasspathConfigurator
import com.github.kitakkun.aspectk.compiler.test.services.AspectKRuntimeClasspathProvider
import com.github.kitakkun.aspectk.compiler.test.services.AspectKStandardLibrariesPathProvider
import com.github.kitakkun.aspectk.compiler.test.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTestBase
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

open class AbstractAspectKDiagnosticTest : AbstractFirDiagnosticTestBase(FirParser.LightTree) {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return AspectKStandardLibrariesPathProvider
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +FirDiagnosticsDirectives.FIR_DUMP
                +JvmEnvironmentConfigurationDirectives.FULL_JDK
            }
            useConfigurators(::ExtensionRegistrarConfigurator, ::AspectKClasspathConfigurator)
            useCustomRuntimeClasspathProviders(::AspectKRuntimeClasspathProvider)
        }
    }
}
