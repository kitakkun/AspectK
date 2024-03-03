@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ksp)
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":aspectk-plugin-common"))
    implementation(libs.kotlin.gradle.plugin.api)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.auto.service)
    ksp(libs.auto.service.ksp)
}
