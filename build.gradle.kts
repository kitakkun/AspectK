@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.buildKonfig) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.aspectkCommon) apply false
}

group = "com.github.kitakkun.aspectk"
version = libs.versions.aspectk.get()

subprojects {
    group = rootProject.group
    version = rootProject.version
}

true
