package com.github.kitakkun.aspectk.compiler.test.services

import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import java.io.File

/**
 * Wraps [EnvironmentBasedStandardLibrariesPathProvider] but redirects the "minimal"
 * runtime jar to the full one, since `dist/kotlin-stdlib-jvm-minimal-for-test.jar`
 * only exists inside the JetBrains Kotlin compiler monorepo.
 */
object AspectKStandardLibrariesPathProvider : KotlinStandardLibrariesPathProvider() {
    private val delegate = EnvironmentBasedStandardLibrariesPathProvider

    override fun runtimeJarForTests(): File = delegate.runtimeJarForTests()
    override fun runtimeJarForTestsWithJdk8(): File = delegate.runtimeJarForTestsWithJdk8()
    override fun minimalRuntimeJarForTests(): File = delegate.runtimeJarForTests()
    override fun reflectJarForTests(): File = delegate.reflectJarForTests()
    override fun kotlinTestJarForTests(): File = delegate.kotlinTestJarForTests()
    override fun scriptRuntimeJarForTests(): File = delegate.scriptRuntimeJarForTests()
    override fun jvmAnnotationsForTests(): File = delegate.jvmAnnotationsForTests()
    override fun getAnnotationsJar(): File = delegate.getAnnotationsJar()
    override fun fullJsStdlib(): File = delegate.fullJsStdlib()
    override fun defaultJsStdlib(): File = delegate.defaultJsStdlib()
    override fun kotlinTestJsKLib(): File = delegate.kotlinTestJsKLib()
}
