@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("aspectkCommon") {
            id = "aspectk.common"
            implementationClass = "AspectKCommonConventionPlugin"
        }
    }
}

dependencies {
    compileOnly(libs.ktlint.gradle)
}
