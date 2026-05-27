package com.github.kitakkun.aspectk.plugin.common

object AspectKSubPluginOptionKey {
    const val ENABLED = "enabled"
    const val REPORT_DIR = "reportDir"

    /**
     * Absolute path the compiler plugin writes the **producer-side** aspect
     * index to. The Gradle plugin sets this to a file under the project's
     * `build/aspectk/aspectIndex/<compName>/META-INF/aspectk/aspects.txt`,
     * then arranges for every Jar task to pick it up so the index is
     * bundled into the published JAR.
     *
     * Cross-module discovery is then **driven entirely from the compiler
     * plugin side**: when compiling a consumer module, the IR extension
     * walks `JVMConfigurationKeys.CONTENT_ROOTS` looking for the same
     * `META-INF/aspectk/aspects.txt` entry inside each classpath JAR /
     * exploded class directory and resolves the listed FQNs through
     * `pluginContext.finderForBuiltins().findClass(...)`. There is no
     * separate consumer-side subplugin option for the FQN list — the
     * compiler plugin already sees the classpath via `CompilerConfiguration`.
     */
    const val ASPECT_INDEX_FILE = "aspectIndexFile"
}
