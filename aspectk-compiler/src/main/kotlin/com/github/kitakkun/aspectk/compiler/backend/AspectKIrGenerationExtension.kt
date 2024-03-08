package com.github.kitakkun.aspectk.compiler.backend

import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectAnalyzer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class AspectKIrGenerationExtension(private val messageCollector: MessageCollector) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        val aspectClasses = AspectAnalyzer.analyze(moduleFragment)

        val context = AspectKIrPluginContext(pluginContext, messageCollector)
        with(context) {
            moduleFragment.transformChildrenVoid(AspectKTransformer(aspectClasses))
        }
    }
}
