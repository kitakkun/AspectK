package com.github.kitakkun.aspectk.compiler.backend

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * Phase 0 stub.
 *
 * The IR-side advice application logic from the v0.x line was tied to the string DSL
 * (`execution(...)` / `args(...)` / `named(...)`) parsed by `aspectk-expression`. The
 * v1 design replaces that entire approach with a composable-annotation + signature-driven
 * scheme — see `docs/v1-roadmap.md`. Rebuilding the v0.x IR transformer for Kotlin 2.3
 * would amount to migrating code that will be deleted in Phase 4 anyway, so the
 * transformer is intentionally absent here.
 *
 * Phase 3 of the roadmap installs the new annotation/signature-driven transformer in
 * this slot.
 */
class AspectKIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // intentional no-op until Phase 3
    }
}
