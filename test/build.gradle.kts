import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinMultiplatform)
    id("com.github.kitakkun.aspectk") version libs.versions.aspectk
}

kotlin {
    jvm()

    sourceSets.all {
        languageSettings.languageVersion = "2.0"
    }

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
tasks.withType(KotlinCompile::class).all {
    dependsOn(":aspectk-plugin-common:publishToMavenLocal")
    dependsOn(":aspectk-compiler:publishToMavenLocal")
    dependsOn(":aspectk-expression:publishToMavenLocal")
}
