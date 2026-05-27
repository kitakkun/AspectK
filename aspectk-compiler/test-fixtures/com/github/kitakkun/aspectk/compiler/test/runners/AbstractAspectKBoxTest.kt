package com.github.kitakkun.aspectk.compiler.test.runners

import com.github.kitakkun.aspectk.compiler.test.services.AspectKClasspathConfigurator
import com.github.kitakkun.aspectk.compiler.test.services.AspectKRuntimeClasspathProvider
import com.github.kitakkun.aspectk.compiler.test.services.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirBlackBoxCodegenTestBase
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

open class AbstractAspectKBoxTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree) {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider = EnvironmentBasedStandardLibrariesPathProvider

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +CodegenTestDirectives.DUMP_IR
                +CodegenTestDirectives.IGNORE_DEXING
                +JvmEnvironmentConfigurationDirectives.FULL_JDK
            }
            useConfigurators(::ExtensionRegistrarConfigurator, ::AspectKClasspathConfigurator)
            useCustomRuntimeClasspathProviders(::AspectKRuntimeClasspathProvider)
        }
    }
}
