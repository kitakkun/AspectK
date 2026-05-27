import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.aspectkCommon)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ksp)
    `java-test-fixtures`
    `maven-publish`
}

val testArtifacts: Configuration by configurations.creating

dependencies {
    implementation(project(":aspectk-plugin-common"))
    implementation(project(":aspectk-expression"))
    compileOnly(libs.kotlin.compiler)
    compileOnly(libs.auto.service)
    ksp(libs.auto.service.ksp)

    testFixturesApi(libs.kotlin.test.junit5)
    testFixturesApi(libs.kotlin.compiler.internal.test.framework)
    testFixturesApi(libs.kotlin.compiler)
    testFixturesRuntimeOnly(libs.junit4)

    // Resolved into test-runtime system properties so the test framework can locate
    // stdlib / reflect / test jars by absolute path at startup.
    testArtifacts(libs.kotlin.stdlib)
    testArtifacts(libs.kotlin.stdlib.jdk8)
    testArtifacts(libs.kotlin.reflect)
    testArtifacts(libs.kotlin.test)
    testArtifacts(libs.kotlin.script.runtime)
    testArtifacts(libs.kotlin.annotations.jvm)
}

sourceSets {
    test {
        java.setSrcDirs(listOf("test", "test-gen"))
        resources.setSrcDirs(listOf("testData"))
    }
    testFixtures {
        java.setSrcDirs(listOf("test-fixtures"))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xcontext-parameters")
    compilerOptions.optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
}

fun Test.setLibraryProperty(
    propName: String,
    jarName: String,
) {
    val path = testArtifacts.files
        .find { """$jarName-\d.*""".toRegex().matches(it.name) }
        ?.absolutePath
        ?: error("testArtifacts is missing $jarName — add the matching `testArtifacts(\"<group>:$jarName:<version>\")` coordinate")
    systemProperty(propName, path)
}

tasks.test {
    dependsOn(testArtifacts)
    dependsOn(":aspectk-annotations:jvmJar")
    dependsOn(":aspectk-core:jvmJar")

    useJUnitPlatform()
    workingDir = rootDir

    systemProperty("idea.home.path", rootDir)
    systemProperty("idea.ignore.disabled.plugins", "true")

    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")

    val annotationsJar = rootProject.layout.projectDirectory
        .dir("aspectk-annotations/build/libs")
        .asFileTree
        .matching { include("aspectk-annotations-jvm-*.jar") }
    val coreJar = rootProject.layout.projectDirectory
        .dir("aspectk-core/build/libs")
        .asFileTree
        .matching { include("aspectk-core-jvm-*.jar") }
    doFirst {
        systemProperty("aspectk.annotations.jar", annotationsJar.singleFile.absolutePath)
        systemProperty("aspectk.core.jar", coreJar.singleFile.absolutePath)
    }
}

val generateTests by tasks.registering(JavaExec::class) {
    inputs.dir(layout.projectDirectory.dir("testData"))
    outputs.dir(layout.projectDirectory.dir("test-gen"))
    classpath = sourceSets.testFixtures.get().runtimeClasspath
    mainClass.set("com.github.kitakkun.aspectk.compiler.test.GenerateTestsKt")
    workingDir = rootDir
}
tasks.compileTestKotlin { dependsOn(generateTests) }
tasks
    .matching {
        it.name == "kspTestKotlin" || it.name == "compileTestJava"
    }.configureEach {
        dependsOn(generateTests)
    }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["kotlin"])
        }
    }
}
