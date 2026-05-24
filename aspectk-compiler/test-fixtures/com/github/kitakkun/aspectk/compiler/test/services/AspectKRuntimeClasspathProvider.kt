package com.github.kitakkun.aspectk.compiler.test.services

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

/**
 * Makes the `aspectk-annotations` and `aspectk-core` runtime jars available to
 * compiled test data, so test sources can `import com.github.kitakkun.aspectk.annotations.*`
 * and `import com.github.kitakkun.aspectk.core.*`.
 *
 * Jar paths are passed by Gradle via system properties (see aspectk-compiler/build.gradle.kts).
 */
class AspectKRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        val annotations = System.getProperty("aspectk.annotations.jar")
            ?: error("System property `aspectk.annotations.jar` is not set; check aspectk-compiler/build.gradle.kts tasks.test wiring")
        val core = System.getProperty("aspectk.core.jar")
            ?: error("System property `aspectk.core.jar` is not set; check aspectk-compiler/build.gradle.kts tasks.test wiring")
        return listOf(File(annotations), File(core))
    }
}
