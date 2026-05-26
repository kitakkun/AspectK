package com.github.kitakkun.aspectk.compiler.backend

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext

/**
 * Phase 0 stub.
 *
 * Wrapper that the Phase 3 IR transformer can hang plugin-wide IR lookups off (cached
 * constructor / function references, etc.). For now it adds nothing on top of
 * [IrPluginContext] — the v0.x members (joinPointClassConstructor, listOfFunction, etc.)
 * were specific to the string-DSL transformer and are dropped along with it. See
 * `docs/v1-roadmap.md`.
 */
class AspectKIrPluginContext(
    val context: IrPluginContext,
) : IrPluginContext by context
