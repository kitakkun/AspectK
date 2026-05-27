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
import java.io.File

class AspectKIrGenerationExtension(
    private val reportDir: String?,
) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        val aspects = AspectAnalyzer().analyze(moduleFragment)
        if (aspects.isEmpty()) return
        val transformer = AspectKTransformer(pluginContext, aspects)
        moduleFragment.transform(transformer, null)

        reportDir?.let { dir ->
            writeReport(File(dir), moduleFragment.name.asString(), aspects, transformer.matches)
        }
    }

    private fun writeReport(
        dir: File,
        moduleName: String,
        aspects: List<AspectMetadata>,
        matches: Map<AdviceMetadata, List<IrSimpleFunction>>,
    ) {
        if (!dir.exists() && !dir.mkdirs()) return
        val sanitized = moduleName.trim('<', '>').replace('/', '_')
        val out = File(dir, "matches-$sanitized.json")
        out.writeText(buildReportJson(moduleName, aspects, matches))
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
}
