@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":aspectk-plugin-common"))
    implementation(project(":aspectk-expression"))
    implementation(libs.kotlin.compiler.embeddable)
    compileOnly(libs.auto.service)
    ksp(libs.auto.service.ksp)

    testImplementation(kotlin("test"))
}
