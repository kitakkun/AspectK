@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
}
