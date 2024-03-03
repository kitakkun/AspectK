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

includeBuild("build-logic")
include(":aspectk-compiler")
include(":aspectk-gradle-plugin")
include(":aspectk-annotations")
include(":aspectk-plugin-common")
include(":aspectk-expression")
include(":test")

rootProject.name = "AspectK"

