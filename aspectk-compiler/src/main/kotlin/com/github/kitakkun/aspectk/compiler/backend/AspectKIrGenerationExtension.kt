package com.github.kitakkun.aspectk.compiler.backend

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class AspectKIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // TODO: analyze @Aspect annotated classes

        // TODO: insert code to matching pointcuts
    }
}
