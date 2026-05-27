package com.github.kitakkun.aspectk.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end smoke test for the AspectK Gradle plugin.
 *
 * Generates a minimal Kotlin/JVM project on disk, applies
 * `com.github.kitakkun.aspectk` to it, and runs the synthetic project's
 * `run` task. Asserts that:
 *
 * - The build succeeds.
 * - Both the `@Before` advice's trace line and the target's own output
 *   appear in stdout, in that order — i.e. weaving actually happened end to
 *   end through the Kotlin compiler plugin pipeline.
 *
 * Three system properties wire the synthetic project to the artefacts the
 * outer build published into `build/testRepo/`:
 *
 * - `aspectk.test.repo` — absolute path of the project-local Maven repo.
 * - `aspectk.test.version` — the AspectK version (`group:artifact:VERSION`).
 * - `aspectk.test.kotlinVersion` — Kotlin version for the embedded `kotlin("jvm")`.
 */
class AspectKGradlePluginTest {
    @TempDir
    lateinit var projectDir: File

    private val testRepo: String by lazy {
        System.getProperty("aspectk.test.repo")
            ?: error("System property `aspectk.test.repo` not set — the Gradle test task must set it.")
    }
    private val aspectkVersion: String by lazy {
        System.getProperty("aspectk.test.version")
            ?: error("System property `aspectk.test.version` not set.")
    }
    private val kotlinVersion: String by lazy {
        System.getProperty("aspectk.test.kotlinVersion")
            ?: error("System property `aspectk.test.kotlinVersion` not set.")
    }

    @Test
    fun `before advice weaves into a JVM target`() {
        writeSettings()
        writeBuildScript()
        writeMain()

        val result = GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments("run", "--stacktrace", "--quiet")
            .forwardOutput()
            .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":run")?.outcome,
            "`run` task should succeed",
        )
        // Each weave-site call adds one ADVICE_BEFORE line; the target then
        // prints its own greeting.
        assertContains(result.output, "ADVICE_BEFORE")
        assertContains(result.output, "hello world")
        assertTrue(
            result.output.indexOf("ADVICE_BEFORE") < result.output.indexOf("hello world"),
            "advice should run before the target body",
        )
    }

    @Test
    fun `aspectKAggregateReport collects per-module reports`() {
        writeSettings()
        writeBuildScript()
        writeMain()

        // First compile so the per-module matches-*.json is materialised.
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments("compileKotlin", "--stacktrace", "--quiet")
            .forwardOutput()
            .build()

        val perModuleReport = File(projectDir, "build/reports/aspectk").listFiles()
            ?.firstOrNull { it.name.startsWith("matches-") && it.extension == "json" }
        assertTrue(
            perModuleReport != null && perModuleReport.exists(),
            "compileKotlin should have produced a per-module matches-*.json under build/reports/aspectk/",
        )

        val aggregateResult = GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments("aspectKAggregateReport", "--stacktrace", "--quiet")
            .forwardOutput()
            .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            aggregateResult.task(":aspectKAggregateReport")?.outcome,
            "`aspectKAggregateReport` task should succeed",
        )

        val aggregateFile = File(projectDir, "build/reports/aspectk/aggregate.json")
        assertTrue(aggregateFile.exists(), "aggregate.json should be written")
        val aggregateText = aggregateFile.readText()
        assertContains(aggregateText, "\"modules\"")
        // The synthetic project's aspect class is GreetingTracer; its advice
        // beforeGreet should appear in the aggregated report.
        assertContains(aggregateText, "GreetingTracer")
        assertContains(aggregateText, "beforeGreet")
    }

    private fun writeSettings() {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven(url = "$testRepo")
                    gradlePluginPortal()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    maven(url = "$testRepo")
                    mavenCentral()
                }
            }

            rootProject.name = "aspectk-testkit-sample"
            """.trimIndent(),
        )
    }

    private fun writeBuildScript() {
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                application
                id("com.github.kitakkun.aspectk") version "$aspectkVersion"
            }

            application {
                mainClass.set("MainKt")
            }

            dependencies {
                implementation("com.github.kitakkun.aspectk:aspectk-annotations:$aspectkVersion")
            }
            """.trimIndent(),
        )
    }

    private fun writeMain() {
        val srcDir = File(projectDir, "src/main/kotlin").apply { mkdirs() }
        File(srcDir, "Main.kt").writeText(
            """
            import com.github.kitakkun.aspectk.annotations.Aspect
            import com.github.kitakkun.aspectk.annotations.Before
            import com.github.kitakkun.aspectk.annotations.MethodName

            class Greeter {
                fun greet(name: String): String {
                    val r = "hello ${'$'}name"
                    println(r)
                    return r
                }
            }

            @Aspect
            class GreetingTracer {
                @Before
                @MethodName("greet")
                fun beforeGreet() {
                    println("ADVICE_BEFORE")
                }
            }

            fun main() {
                Greeter().greet("world")
            }
            """.trimIndent(),
        )
    }
}
