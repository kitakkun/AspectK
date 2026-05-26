import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinMultiplatform)
    id("com.github.kitakkun.aspectk") version libs.versions.aspectk
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":aspectk-annotations"))
            implementation(project(":aspectk-core"))
        }
    }
}

// publish required artifacts when performing sync on IDEA
tasks.prepareKotlinIdeaImport {
    dependsOn(":aspectk-plugin-common:publishToMavenLocal")
    dependsOn(":aspectk-compiler:publishToMavenLocal")
    dependsOn(":aspectk-expression:publishToMavenLocal")
}

// publish required artifacts when compiling via ./gradlew
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(":aspectk-plugin-common:publishToMavenLocal")
    dependsOn(":aspectk-compiler:publishToMavenLocal")
    dependsOn(":aspectk-expression:publishToMavenLocal")
}
