package com.github.kitakkun.aspectk.compiler.test.services

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class AspectKRuntimeClasspathProvider(
    testServices: TestServices,
) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        val annotations = System.getProperty("aspectk.annotations.jar")
            ?: error("System property `aspectk.annotations.jar` is not set")
        val core = System.getProperty("aspectk.core.jar")
            ?: error("System property `aspectk.core.jar` is not set")
        return listOf(File(annotations), File(core))
    }
}
