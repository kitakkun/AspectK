@file:Suppress("DSL_SCOPE_VIOLATION")

import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.buildKonfig)
    `maven-publish`
}

kotlin {
    jvm()
}

buildkonfig {
    packageName = "com.github.kitakkun.aspectk.plugin.common"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "VERSION", libs.versions.aspectk.get())
    }
}
