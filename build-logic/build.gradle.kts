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
