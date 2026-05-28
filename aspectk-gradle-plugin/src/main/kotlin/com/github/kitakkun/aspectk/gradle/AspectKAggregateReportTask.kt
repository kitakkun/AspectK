package com.github.kitakkun.aspectk.gradle

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Cross-module weaving consistency check. Aggregates every project's
 * `build/reports/aspectk/matches-<module>.json` produced by the AspectK IR
 * generation extension, detects advices that matched **zero** call sites
 * across the entire aggregated view, and writes a rolled-up
 * `build/reports/aspectk/aggregate.json`.
 *
 * Behaviour on unused advices:
 *
 * - `strictMode = false` (default): emits a warning listing each unused
 *   advice as `<aspectFqName>.<adviceName> (<kind>)`. Task still succeeds.
 * - `strictMode = true`: throws `GradleException` listing the unused
 *   advices. Build fails.
 *
 * Wired up by `AspectKKotlinCompilerPluginSupportPlugin.apply(...)` on the
 * project the plugin is applied to. The task depends on every subproject's
 * `compileKotlin*` task so a single invocation produces a complete picture
 * regardless of which compile targets were primed beforehand.
 */
abstract class AspectKAggregateReportTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val perModuleReports: ConfigurableFileCollection

    @get:Input
    abstract val strictMode: Property<Boolean>

    @get:OutputFile
    abstract val aggregateFile: RegularFileProperty

    init {
        group = "verification"
        description =
            "Aggregates per-module AspectK weaving reports and flags advices that matched zero call sites."
    }

    @TaskAction
    fun run() {
        val files = perModuleReports
            .filter { it.isFile && it.name.startsWith("matches-") && it.extension == "json" }
            .sortedBy { it.name }

        val modules = files.mapNotNull { file -> parseModule(file) }
        val unused = collectUnused(modules)

        writeAggregate(modules, unused)
        reportUnused(unused)
    }

    private fun parseModule(file: File): ModuleReport? {
        val raw = runCatching { JsonSlurper().parse(file) as? Map<*, *> }
            .getOrNull() ?: run {
            logger.warn("AspectK: could not parse {} — skipping", file.absolutePath)
            return null
        }
        val name = raw["module"] as? String ?: return null
        val advices = (raw["advices"] as? List<*>).orEmpty().mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val targets = (map["targets"] as? List<*>).orEmpty()
            AdviceReport(
                aspect = map["aspect"] as? String ?: return@mapNotNull null,
                advice = map["advice"] as? String ?: return@mapNotNull null,
                kind = map["kind"] as? String ?: "",
                targetCount = targets.size,
            )
        }
        return ModuleReport(name, file.readText(), advices)
    }

    /**
     * An advice is "unused" iff its `targetCount` is zero in **every** module
     * that reports it. Cross-module weaving (aspect declared in module A,
     * targets in module B) is supported: B's compile sees A's aspect via
     * `META-INF/aspectk/aspects.txt` discovery and the matches show up in
     * B's `matches-*.json`, so the grouping below correctly aggregates the
     * non-zero count.
     */
    private fun collectUnused(modules: List<ModuleReport>): List<UnusedAdvice> {
        val byKey = linkedMapOf<Pair<String, String>, MutableList<AdviceReport>>()
        for (m in modules) {
            for (a in m.advices) {
                byKey.getOrPut(a.aspect to a.advice) { mutableListOf() } += a
            }
        }
        return byKey.entries
            .filter { (_, reports) -> reports.all { it.targetCount == 0 } }
            .map { (key, reports) ->
                UnusedAdvice(aspectFqName = key.first, adviceName = key.second, kind = reports.first().kind)
            }
    }

    private fun writeAggregate(
        modules: List<ModuleReport>,
        unused: List<UnusedAdvice>,
    ) {
        val out = aggregateFile.get().asFile
        out.parentFile?.mkdirs()
        val sb = StringBuilder()
        sb.append("{\n  \"modules\": [\n")
        modules.forEachIndexed { i, m ->
            // Splice the per-module JSON verbatim so producer-side schema
            // changes don't require updates here.
            val trimmed = m.rawText.trim()
            for (line in trimmed.lineSequence()) {
                sb.append("    ").append(line).append("\n")
            }
            sb.setLength(sb.length - 1)
            if (i != modules.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"unusedAdvices\": [\n")
        unused.forEachIndexed { i, u ->
            sb.append("    { \"aspect\": ").append(jsonString(u.aspectFqName))
            sb.append(", \"advice\": ").append(jsonString(u.adviceName))
            sb.append(", \"kind\": ").append(jsonString(u.kind)).append(" }")
            if (i != unused.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")
        out.writeText(sb.toString())
    }

    private fun reportUnused(unused: List<UnusedAdvice>) {
        if (unused.isEmpty()) return
        val lines = unused.map { "  - ${it.aspectFqName}.${it.adviceName} (${it.kind})" }
        val message = buildString {
            append("AspectK: ${unused.size} advice(s) matched zero call sites across the build:\n")
            append(lines.joinToString("\n"))
        }
        if (strictMode.getOrElse(false)) {
            throw GradleException(
                "$message\n\nDisable `aspectk.strictUnusedAspects` to downgrade to a warning, " +
                    "or narrow each advice's pointcut so it matches an existing target.",
            )
        }
        logger.warn(message)
    }

    private fun jsonString(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private data class AdviceReport(
        val aspect: String,
        val advice: String,
        val kind: String,
        val targetCount: Int,
    )

    private data class ModuleReport(
        val name: String,
        val rawText: String,
        val advices: List<AdviceReport>,
    )

    private data class UnusedAdvice(
        val aspectFqName: String,
        val adviceName: String,
        val kind: String,
    )
}
