package com.github.kitakkun.aspectk.compiler

import com.github.kitakkun.aspectk.plugin.common.AspectKSubPluginOptionKey
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object AspectKCompilerConfigurationKey {
    val ENABLED = CompilerConfigurationKey<Boolean>(AspectKSubPluginOptionKey.ENABLED)
}
