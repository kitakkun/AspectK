package com.github.kitakkun.aspectk.compiler

import com.github.kitakkun.aspectk.plugin.common.AspectKSubPluginOptionKey
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object AspectKCompilerConfigurationKey {
    val ENABLED = CompilerConfigurationKey<Boolean>(AspectKSubPluginOptionKey.ENABLED)
    val REPORT_DIR = CompilerConfigurationKey<String>(AspectKSubPluginOptionKey.REPORT_DIR)
    val ASPECT_INDEX_FILE = CompilerConfigurationKey<String>(AspectKSubPluginOptionKey.ASPECT_INDEX_FILE)
}
