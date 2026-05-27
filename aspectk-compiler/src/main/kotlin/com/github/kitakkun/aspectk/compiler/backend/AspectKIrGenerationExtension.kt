package com.github.kitakkun.aspectk.compiler.backend

import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceKind
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceMetadata
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectAnalyzer
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectMetadata
import com.github.kitakkun.aspectk.compiler.backend.transformer.AspectKTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.util.jar.JarFile

class AspectKIrGenerationExtension(
    private val reportDir: String?,
    private val aspectIndexFile: String? = null,
    private val classpathRoots: List<File> = emptyList(),
) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        val analyzer = AspectAnalyzer()
        val localAspects = analyzer.analyze(moduleFragment)
        val externalAspects = resolveExternalAspects(analyzer, pluginContext)
        val aspects = localAspects + externalAspects

        // Write the producer-side aspect index before bailing on no-aspect
        // modules — but only when there's something to publish. Skipping
        // empty modules avoids littering every Kotlin JAR with a useless
        // META-INF/aspectk/aspects.txt marker.
        if (aspectIndexFile != null && localAspects.isNotEmpty()) {
            writeAspectIndex(File(aspectIndexFile), localAspects)
        }

        if (aspects.isEmpty()) return
        val transformer = AspectKTransformer(pluginContext, aspects)
        moduleFragment.transform(transformer, null)

        reportDir?.let { dir ->
            writeReport(File(dir), moduleFragment.name.asString(), aspects, transformer.matches)
        }
    }

    /**
     * Discovers `@Aspect` classes from the compile classpath and resolves
     * each to an [AspectMetadata] via the IR-time finder API.
     *
     * Discovery: each entry in [classpathRoots] (a snapshot of the JVM
     * `CONTENT_ROOTS` taken at registrar time) is checked for the marker
     * file `META-INF/aspectk/aspects.txt` — listed one fully-qualified
     * `@Aspect` class name per line. Both packaged JARs and exploded class
     * directories are supported, so project deps (which Gradle resolves to
     * `classes` directories) work without going through a JAR.
     *
     * Resolution: each discovered FQN is looked up through
     * `pluginContext.finderForBuiltins().findClass(...)`. The
     * builtins-flavoured finder is appropriate because the FQN list comes
     * from a build-tool channel, not from any specific source file in this
     * compilation. Names that fail to resolve are silently dropped — that
     * typically means the user's dependency was removed between the marker
     * being written and the consumer compile being launched.
     */
    private fun resolveExternalAspects(
        analyzer: AspectAnalyzer,
        pluginContext: IrPluginContext,
    ): List<AspectMetadata> {
        val externalFqNames = discoverExternalAspectFqNames()
        if (externalFqNames.isEmpty()) return emptyList()
        val finder = pluginContext.finderForBuiltins()
        return externalFqNames.mapNotNull { fqName ->
            val classId = ClassId.topLevel(FqName(fqName))
            val symbol = finder.findClass(classId) ?: return@mapNotNull null
            analyzer.buildAspectMetadata(symbol.owner)
        }
    }

    private fun discoverExternalAspectFqNames(): List<String> {
        if (classpathRoots.isEmpty()) return emptyList()
        val result = linkedSetOf<String>()
        for (root in classpathRoots) {
            when {
                !root.exists() -> continue
                root.isFile && root.name.endsWith(".jar") -> readFromJar(root).forEach { result += it }
                root.isDirectory -> readFromDirectory(root).forEach { result += it }
            }
        }
        return result.toList()
    }

    private fun readFromJar(jarFile: File): List<String> =
        runCatching {
            JarFile(jarFile).use { jar ->
                val entry = jar.getJarEntry(ASPECT_INDEX_ENTRY) ?: return@use emptyList()
                jar.getInputStream(entry).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
                }
            }
        }.getOrDefault(emptyList())

    private fun readFromDirectory(dir: File): List<String> {
        val file = dir.resolve(ASPECT_INDEX_ENTRY)
        if (!file.isFile) return emptyList()
        return runCatching {
            file.readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }
        }.getOrDefault(emptyList())
    }

    private fun writeAspectIndex(
        file: File,
        localAspects: List<AspectMetadata>,
    ) {
        runCatching {
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            val fqNames = localAspects.map { it.aspectClass.kotlinFqName.asString() }
            file.writeText(fqNames.joinToString(separator = "\n", postfix = "\n"))
        }
    }

    private fun writeReport(
        dir: File,
        moduleName: String,
        aspects: List<AspectMetadata>,
        matches: Map<AdviceMetadata, List<IrSimpleFunction>>,
    ) {
        // The report is non-essential build output — never let an IOException
        // here fail the user's compilation.
        runCatching {
            if (!dir.exists() && !dir.mkdirs()) return@runCatching
            val out = File(dir, "matches-${sanitizeForFilename(moduleName)}.json")
            out.writeText(buildReportJson(moduleName, aspects, matches))
        }
    }

    /**
     * Strips characters that are invalid in a filename on common platforms
     * (Windows is the strictest: `<`, `>`, `:`, `\`, `/`, `*`, `?`, `"`, `|`).
     * Kotlin module names are commonly bracketed (`<test>`), and may include
     * `/` in `kotlin.compiler.execution.strategy = in-process` paths.
     */
    private fun sanitizeForFilename(name: String): String {
        val sb = StringBuilder(name.length)
        for (c in name) {
            when (c) {
                '<', '>', ':', '\\', '/', '*', '?', '"', '|' -> sb.append('_')
                else -> sb.append(c)
            }
        }
        return sb.toString().trim('_').ifEmpty { "module" }
    }

    private fun buildReportJson(
        moduleName: String,
        aspects: List<AspectMetadata>,
        matches: Map<AdviceMetadata, List<IrSimpleFunction>>,
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"module\": ").append(jsonString(moduleName)).append(",\n")
        sb.append("  \"advices\": [\n")
        val adviceEntries = aspects.flatMap { aspect ->
            aspect.advices.map { aspect to it }
        }
        adviceEntries.forEachIndexed { index, (aspect, advice) ->
            val targets = matches[advice].orEmpty()
            val aspectFq = aspect.aspectClass.kotlinFqName.asString()
            sb.append("    {\n")
            sb.append("      \"aspect\": ").append(jsonString(aspectFq)).append(",\n")
            sb.append("      \"advice\": ").append(jsonString(advice.function.name.asString())).append(",\n")
            sb.append("      \"kind\": ").append(jsonString(kindLabel(advice.kind))).append(",\n")
            sb.append("      \"targets\": [")
            if (targets.isEmpty()) {
                sb.append("]\n")
            } else {
                sb.append('\n')
                targets.forEachIndexed { tIndex, target ->
                    val klass = target.parentClassOrNull?.kotlinFqName?.asString() ?: ""
                    sb.append("        { \"class\": ").append(jsonString(klass))
                    sb.append(", \"method\": ").append(jsonString(target.name.asString())).append(" }")
                    if (tIndex != targets.lastIndex) sb.append(',')
                    sb.append('\n')
                }
                sb.append("      ]\n")
            }
            sb.append("    }")
            if (index != adviceEntries.lastIndex) sb.append(',')
            sb.append('\n')
        }
        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun kindLabel(kind: AdviceKind): String =
        when (kind) {
            AdviceKind.BEFORE -> "@Before"
            AdviceKind.AFTER -> "@After"
            AdviceKind.AROUND -> "@Around"
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

    private companion object {
        const val ASPECT_INDEX_ENTRY = "META-INF/aspectk/aspects.txt"
    }
}
