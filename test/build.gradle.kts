@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.github.kitakkun.aspectk") version libs.versions.aspectk
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":aspectk-annotations"))
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
tasks.withType(KotlinCompile::class).all {
    dependsOn(":aspectk-plugin-common:publishToMavenLocal")
    dependsOn(":aspectk-compiler:publishToMavenLocal")
    dependsOn(":aspectk-expression:publishToMavenLocal")
}
