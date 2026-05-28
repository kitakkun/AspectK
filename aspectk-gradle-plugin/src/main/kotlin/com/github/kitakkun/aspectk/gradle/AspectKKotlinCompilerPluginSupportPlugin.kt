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
        registerAggregateReportTask(target)
    }

    /**
     * Idempotently registers `aspectKAggregateReport` on [target] (the
     * `allprojects` cap of "every project whose plugin's apply fires"). Uses
     * `tasks.findByName` so multi-module builds that apply the plugin to
     * multiple modules don't double-register.
     */
    private fun registerAggregateReportTask(target: Project) {
        if (target.tasks.findByName(AGGREGATE_REPORT_TASK_NAME) != null) return
        val extension = target.extensions.getByType(AspectKExtension::class.java)
        target.tasks.register(AGGREGATE_REPORT_TASK_NAME, AspectKAggregateReportTask::class.java) { task ->
            // Walk every project the rootProject can see. Each producer's
            // `build/reports/aspectk/matches-*.json` is collected as an input.
            // Resolution is lazy via `provider` so subprojects added after this
            // apply still contribute.
            task.perModuleReports.from(
                target.provider {
                    target.allprojects.map { p ->
                        p.layout.buildDirectory.dir("reports/aspectk").map { dir ->
                            dir.asFileTree.matching { it.include("matches-*.json") }
                        }
                    }
                },
            )
            task.aggregateFile.set(
                target.layout.buildDirectory.file("reports/aspectk/aggregate.json"),
            )
            task.strictMode.set(extension.strictUnusedAspects.orElse(false))

            // Force every project's Kotlin compile to run first so the
            // aggregator sees a complete picture. `dependsOn` accepts a
            // Provider, and `tasks.matching { … }` is a lazy live collection
            // resolved at graph time — subprojects added after this apply
            // still feed in.
            task.dependsOn(
                target.provider {
                    target.allprojects.flatMap { p ->
                        p.tasks.matching { isKotlinCompileTaskName(it.name) }
                    }
                },
            )
        }
    }

    private fun isKotlinCompileTaskName(name: String): Boolean =
        // Covers `compileKotlin`, `compileKotlinJvm`, `compileKotlinJvmMain`,
        // `compileTestKotlin`, etc. Excludes script-compile tasks created by
        // unrelated Kotlin DSL machinery.
        name.startsWith("compileKotlin") || (name.startsWith("compile") && name.endsWith("Kotlin"))

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(AspectKExtension::class.java)
        val reportDirProvider = project.layout.buildDirectory.dir("reports/aspectk")
        return project.provider {
            // Resolve buildDirectory lazily (inside the provider) so any
            // user-supplied `layout.buildDirectory.set(...)` override applied
            // after this plugin's `apply` is still picked up.
            listOf(
                SubpluginOption(key = AspectKSubPluginOptionKey.ENABLED, value = extension.enabled.toString()),
                SubpluginOption(
                    key = AspectKSubPluginOptionKey.REPORT_DIR,
                    value = reportDirProvider.get().asFile.absolutePath,
                ),
            )
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) = kotlinCompilation.project.plugins.hasPlugin(AspectKPluginConsts.PLUGIN_ID)

    override fun getCompilerPluginId() = AspectKPluginConsts.PLUGIN_ID

    override fun getPluginArtifact() =
        SubpluginArtifact(
            groupId = "com.github.kitakkun.aspectk",
            artifactId = "aspectk-compiler",
            version = AspectKPluginConsts.PLUGIN_VERSION,
        )

    private companion object {
        const val AGGREGATE_REPORT_TASK_NAME = "aspectKAggregateReport"
    }
}
