plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ksp)
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("aspectk") {
            id = "com.github.kitakkun.aspectk"
            implementationClass = "com.github.kitakkun.aspectk.gradle.AspectKKotlinCompilerPluginSupportPlugin"
        }
    }
}

dependencies {
    implementation(project(":aspectk-plugin-common"))
    implementation(libs.kotlin.gradle.plugin.api)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.auto.service)
    ksp(libs.auto.service.ksp)

    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit4)
}

// The TestKit smoke test publishes every producer module into a project-local
// Maven repo before running. Guard the test wiring so it only activates when
// all producer modules are part of the current build — keeps this script
// robust against the gradle-plugin module being consumed from an isolated
// included build in the future.
val producerProjects = listOf(
    ":aspectk-compiler",
    ":aspectk-plugin-common",
    ":aspectk-annotations",
    ":aspectk-core",
    ":aspectk-expression",
    ":aspectk-gradle-plugin",
)
val producerProjectsAvailable = producerProjects.all { rootProject.findProject(it) != null }

if (producerProjectsAvailable) {
    // Project-local Maven repo that the TestKit smoke test points its synthetic
    // project at. Holds every artefact the synthetic project resolves via Maven
    // coordinates: the compiler plugin (referenced as a SubpluginArtifact by
    // `AspectKKotlinCompilerPluginSupportPlugin`), its transitive
    // `aspectk-plugin-common` / `aspectk-expression`, the AspectK Gradle plugin
    // itself, and the user-facing `aspectk-annotations` / `aspectk-core`
    // libraries imported by the synthetic source files.
    val testRepoDir = layout.buildDirectory.dir("testRepo")
    val testRepoUri = testRepoDir.map { it.asFile.toURI() }

    // Register a `testRepo` Maven repository on each producer module's
    // PublishingExtension so the `publishAllPublicationsToTestRepoRepository`
    // task materialises with `build/testRepo/` as its target.
    //
    // The producer modules may already be evaluated by the time this script
    // runs (depending on task graph order), so we route through
    // `pluginManager.withPlugin("maven-publish")` which fires whether the
    // plugin has been applied yet or not.
    producerProjects.forEach { path ->
        val producer = rootProject.project(path)
        producer.pluginManager.withPlugin("maven-publish") {
            producer.extensions
                .getByType(PublishingExtension::class.java)
                .repositories
                .maven {
                    name = "testRepo"
                    url = testRepoUri.get()
                }
        }
    }

    val publishToTestRepo by tasks.registering {
        group = "verification"
        description = "Publishes all artefacts the TestKit smoke test consumes into build/testRepo/."
        dependsOn(producerProjects.map { "$it:publishAllPublicationsToTestRepoRepository" })
    }

    tasks.test {
        useJUnitPlatform()
        dependsOn(publishToTestRepo)
        systemProperty("aspectk.test.repo", testRepoDir.get().asFile.absolutePath)
        systemProperty("aspectk.test.version", libs.versions.aspectk.get())
        systemProperty("aspectk.test.kotlinVersion", libs.versions.kotlin.get())
    }
}
