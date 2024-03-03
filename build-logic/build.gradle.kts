@file:Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.buildKonfig) apply false
    alias(libs.plugins.ksp) apply false
}

subprojects {
    group = "com.github.kitakkun.aspectk"
    version = "1.0.0"
}
