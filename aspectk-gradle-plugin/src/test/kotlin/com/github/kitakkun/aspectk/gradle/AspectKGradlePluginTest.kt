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
 * End-to-end smoke tests for the AspectK Gradle plugin.
 *
 * Each test generates a minimal Kotlin/JVM project on disk, applies
 * `com.github.kitakkun.aspectk` to it, and runs a Gradle task via TestKit.
 * Three system properties wire the synthetic projects to the artefacts the
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

        // The aggregator dependsOn every project's compile task, so a single
        // invocation forces compilation + per-module report generation.
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
        // No advice is unused — the @Before(@MethodName("greet")) matches
        // Greeter.greet, so the unusedAdvices list must be empty.
        assertContains(aggregateText, "\"unusedAdvices\": [\n  ]")
    }

    @Test
    fun `aspectKAggregateReport in strict mode fails the build when an advice is unused`() {
        writeSettings()
        writeBuildScript(strictMode = true)
        // Main.kt declares @MethodName("greet") but the source has no `greet`
        // function — the advice matches zero call sites, which strict mode
        // turns into a build failure.
        writeUnboundMain()

        val result = GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments("aspectKAggregateReport", "--stacktrace", "--quiet")
            .forwardOutput()
            .buildAndFail()

        assertContains(result.output, "matched zero call sites")
        assertContains(result.output, "GreetingTracer.beforeGreet")
        assertContains(
            result.output,
            "Disable `aspectk.strictUnusedAspects` to downgrade",
        )
    }

    @Test
    fun `aspect declared in dependency module weaves into consumer module call sites`() {
        // Two-module project: ":aspect" declares an @Aspect, ":consumer"
        // depends on it and calls a function the advice should match. The
        // pipeline this exercises end-to-end: producer compile writes the
        // aspect index → Jar bundles META-INF/aspectk/aspects.txt →
        // consumer's compiler plugin scans classpath JARs → resolves the
        // external @Aspect via finderForBuiltins().findClass(...) → IR
        // weaver wraps Greeter.greet.
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

            rootProject.name = "aspectk-cross-module-sample"
            include(":aspect")
            include(":consumer")
            """.trimIndent(),
        )

        val aspectDir = File(projectDir, "aspect").apply { mkdirs() }
        File(aspectDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                id("com.github.kitakkun.aspectk") version "$aspectkVersion"
            }

            dependencies {
                implementation("com.github.kitakkun.aspectk:aspectk-annotations:$aspectkVersion")
            }
            """.trimIndent(),
        )
        val aspectSrc = File(aspectDir, "src/main/kotlin").apply { mkdirs() }
        File(aspectSrc, "GreetingTracer.kt").writeText(
            """
            package shared

            import com.github.kitakkun.aspectk.annotations.Aspect
            import com.github.kitakkun.aspectk.annotations.Before
            import com.github.kitakkun.aspectk.annotations.MethodName

            @Aspect
            class GreetingTracer {
                @Before
                @MethodName("greet")
                fun beforeGreet() {
                    println("ADVICE_BEFORE")
                }
            }
            """.trimIndent(),
        )

        val consumerDir = File(projectDir, "consumer").apply { mkdirs() }
        File(consumerDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                application
                id("com.github.kitakkun.aspectk") version "$aspectkVersion"
            }

            application {
                mainClass.set("consumer.MainKt")
            }

            dependencies {
                implementation(project(":aspect"))
                implementation("com.github.kitakkun.aspectk:aspectk-annotations:$aspectkVersion")
            }
            """.trimIndent(),
        )
        val consumerSrc = File(consumerDir, "src/main/kotlin/consumer").apply { mkdirs() }
        File(consumerSrc, "Main.kt").writeText(
            """
            package consumer

            class Greeter {
                fun greet(name: String): String {
                    val r = "hello ${'$'}name"
                    println(r)
                    return r
                }
            }

            fun main() {
                Greeter().greet("world")
            }
            """.trimIndent(),
        )

        val result = GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments(":consumer:run", "--stacktrace", "--quiet")
            .forwardOutput()
            .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":consumer:run")?.outcome,
            "`:consumer:run` should succeed",
        )
        // The @Before advice from the :aspect module wove into Greeter.greet
        // inside :consumer — ADVICE_BEFORE appears before the target's own
        // output.
        assertContains(result.output, "ADVICE_BEFORE")
        assertContains(result.output, "hello world")
        assertTrue(
            result.output.indexOf("ADVICE_BEFORE") < result.output.indexOf("hello world"),
            "cross-module advice should run before the target body",
        )
    }

    private fun writeUnboundMain() {
        val srcDir = File(projectDir, "src/main/kotlin").apply { mkdirs() }
        File(srcDir, "Main.kt").writeText(
            """
            import com.github.kitakkun.aspectk.annotations.Aspect
            import com.github.kitakkun.aspectk.annotations.Before
            import com.github.kitakkun.aspectk.annotations.MethodName

            // Note: no function named `greet` exists anywhere in this source,
            // so the advice below matches nothing.

            @Aspect
            class GreetingTracer {
                @Before
                @MethodName("greet")
                fun beforeGreet() {
                    println("ADVICE_BEFORE")
                }
            }

            fun main() {}
            """.trimIndent(),
        )
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

    private fun writeBuildScript(strictMode: Boolean = false) {
        val aspectkBlock = if (strictMode) {
            """
            aspectk {
                strictUnusedAspects.set(true)
            }
            """.trimIndent()
        } else {
            ""
        }
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

            $aspectkBlock

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
