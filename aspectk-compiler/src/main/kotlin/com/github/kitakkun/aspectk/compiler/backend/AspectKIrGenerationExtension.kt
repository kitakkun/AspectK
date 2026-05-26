package com.github.kitakkun.aspectk.compiler.backend

import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectAnalyzer
import com.github.kitakkun.aspectk.compiler.backend.transformer.AspectKTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class AspectKIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        val aspects = AspectAnalyzer().analyze(moduleFragment)
        if (aspects.isEmpty()) return
        moduleFragment.transform(AspectKTransformer(pluginContext, aspects), null)
    }
}
