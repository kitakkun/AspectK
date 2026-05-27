plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
}

kotlin {
    jvm()
}
