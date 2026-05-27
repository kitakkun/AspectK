package com.github.kitakkun.aspectk.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

/**
 * Aggregates per-module weaving reports (`matches-<module>.json`) emitted by
 * the AspectK IR generation extension into a single rolled-up JSON document.
 *
 * Each producer module writes its report to `build/reports/aspectk/`. This
 * task collects them across the build (typically the root project's
 * `allprojects` reports dir) and merges them as an array of per-module
 * payloads under a top-level `modules` key. No JSON parsing happens here —
 * the per-module documents are spliced verbatim, so adding new fields to
 * the producer side doesn't require updates to this task.
 *
 * Wired up by `AspectKKotlinCompilerPluginSupportPlugin.apply(...)` on the
 * project the plugin is applied to. For multi-module builds where the plugin
 * is applied to leaves, apply it on the root project as well to make the
 * aggregator visible at `:aspectKAggregateReport`.
 */
abstract class AspectKAggregateReportTask : DefaultTask() {
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val perModuleReports: ConfigurableFileCollection

    @get:OutputFile
    abstract val aggregateFile: RegularFileProperty

    init {
        group = "verification"
        description = "Aggregates per-module AspectK weaving reports into a single JSON."
    }

    @TaskAction
    fun run() {
        val files = perModuleReports
            .filter { it.isFile && it.name.startsWith("matches-") && it.extension == "json" }
            .sortedBy { it.name }
        val out = aggregateFile.get().asFile
        out.parentFile?.mkdirs()
        val sb = StringBuilder()
        sb.append("{\n  \"modules\": [\n")
        files.forEachIndexed { index, file ->
            val content = file.readText().trim()
            content.lineSequence().forEachIndexed { lineIndex, line ->
                if (lineIndex == 0) sb.append("    ") else sb.append("    ")
                sb.append(line)
                sb.append("\n")
            }
            // Strip trailing newline added above for the closing brace of this
            // module's payload, then add a comma if this isn't the last one.
            sb.setLength(sb.length - 1)
            if (index != files.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n}\n")
        out.writeText(sb.toString())
    }
}
