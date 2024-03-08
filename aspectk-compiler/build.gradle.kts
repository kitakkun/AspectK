@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ksp)
    `maven-publish`
}

dependencies {
    implementation(project(":aspectk-plugin-common"))
    implementation(project(":aspectk-expression"))
    implementation(libs.kotlin.compiler.embeddable)
    compileOnly(libs.auto.service)
    ksp(libs.auto.service.ksp)

    testImplementation(kotlin("test"))
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["kotlin"])
        }
    }
}
