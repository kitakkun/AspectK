package com.github.kitakkun.aspectk.compiler.backend

import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectAnalyzer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

context(MessageCollector)
class AspectKIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val aspectClasses = AspectAnalyzer.analyze(moduleFragment)
        report(CompilerMessageSeverity.WARNING, "AspectK: Found ${aspectClasses.size} aspect classes")
        report(CompilerMessageSeverity.WARNING, "AspectK: ${aspectClasses}")

        val context = AspectKIrPluginContext(pluginContext)
        with(context) {
            moduleFragment.transformChildrenVoid(AspectKTransformer(aspectClasses))
        }
    }
}
