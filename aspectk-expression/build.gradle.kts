plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinJvm)
    `maven-publish`
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    testImplementation(libs.kotlin.compiler)
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["kotlin"])
        }
    }
}
