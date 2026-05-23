package com.github.kitakkun.aspectk.compiler.backend.utils

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner

fun IrSymbolOwner.irBlockBuilder(pluginContext: IrPluginContext) =
    IrBlockBuilder(
        context = pluginContext,
        scope = Scope(symbol),
        startOffset = startOffset,
        endOffset = endOffset,
    )
