pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("../build-logic")
include(
    ":aspectk-plugin-common",
    ":aspectk-gradle-plugin",
)

project(":aspectk-gradle-plugin").projectDir = file("../aspectk-gradle-plugin")
project(":aspectk-plugin-common").projectDir = file("../aspectk-plugin-common")

rootProject.name = "build-logic-for-testing"
