@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.kotlin.compiler.embeddable)
    testImplementation(kotlin("test"))
}
