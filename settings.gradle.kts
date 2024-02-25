pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

include(":aspectk-compiler")
include(":aspectk-gradle-plugin")
include(":aspectk-annotations")
include(":aspectk-plugin-common")
include(":aspectk-expression")

rootProject.name = "AspectK"

