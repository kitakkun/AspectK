pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeBuild("build-logic")
include(":aspectk-compiler")
include(":aspectk-gradle-plugin")
include(":aspectk-annotations")
include(":aspectk-plugin-common")
include(":aspectk-expression")
include(":aspectk-core")

rootProject.name = "AspectK"
