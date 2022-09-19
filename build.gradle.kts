import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
    antlr
    idea
}

group = "com.kobra"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

val packageName = "com.kobra"
val outputPath = "src/main/java/${packageName.replace('.', '/')}"
tasks.generateGrammarSource {
    source = project.objects
        .sourceDirectorySet("antlr", "antlr")
        .srcDir("src/main/antlr").apply {
            include("kobra.g4")
        }
    maxHeapSize = "64m"
    outputDirectory = File(outputPath)
    arguments = listOf(
        "-visitor",
        "-listener",
        "-package", packageName,
    )
}

configure<SourceSetContainer> {
    named("main") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            kotlin.srcDir("src/main/java")
            kotlin.srcDir("src/main/kotlin")
        }
    }
}

tasks.getByName("compileKotlin").dependsOn("generateGrammarSource")

dependencies {
    testImplementation(kotlin("test"))
    antlr("org.antlr:antlr4:4.10.1")
    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "16"
}

buildscript {
    dependencies {
        classpath("org.antlr:antlr4:4.10.1")
    }
}

application {
    mainClass.set("MainKt")
}

tasks.register("jsIrBrowserTest")
tasks.register("jsLegacyBrowserTest")
