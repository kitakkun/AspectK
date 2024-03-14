plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinJvm)
    `maven-publish`
}

dependencies {
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["kotlin"])
        }
    }
}
