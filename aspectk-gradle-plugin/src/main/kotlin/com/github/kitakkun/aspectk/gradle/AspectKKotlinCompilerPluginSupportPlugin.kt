package com.github.kitakkun.aspectk.gradle

import com.github.kitakkun.aspectk.gradle.extension.AspectKExtension
import com.github.kitakkun.aspectk.plugin.common.AspectKPluginConsts
import com.github.kitakkun.aspectk.plugin.common.AspectKSubPluginOptionKey
import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("UNUSED")
@AutoService(KotlinCompilerPluginSupportPlugin::class)
class AspectKKotlinCompilerPluginSupportPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("aspectk", AspectKExtension::class.java)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val extension = kotlinCompilation.target.project.extensions.getByType(AspectKExtension::class.java)
        return kotlinCompilation.target.project.provider {
            listOf(SubpluginOption(key = AspectKSubPluginOptionKey.ENABLED, value = extension.enabled.toString()))
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) = kotlinCompilation.project.plugins.hasPlugin(AspectKPluginConsts.PLUGIN_ID)

    override fun getCompilerPluginId() = AspectKPluginConsts.PLUGIN_ID

    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = "com.github.kitakkun.aspectk",
        artifactId = "aspectk-compiler",
        version = AspectKPluginConsts.PLUGIN_VERSION,
    )
}
