plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ksp)
    `maven-publish`
}

dependencies {
    implementation(project(":aspectk-plugin-common"))
    implementation(project(":aspectk-expression"))
    compileOnly(libs.kotlin.compiler)
    compileOnly(libs.auto.service)
    ksp(libs.auto.service.ksp)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["kotlin"])
        }
    }
}
