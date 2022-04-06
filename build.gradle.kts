import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
    antlr
    idea
}

group = "me.albert"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

val langPath = "src/main/java/"
tasks.generateGrammarSource {
    maxHeapSize = "64m"
    outputDirectory = File(langPath)
    arguments = listOf("-visitor", "-listener")
}

configure<SourceSetContainer> {
    named("main") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            // kotlin.srcDir("build/generated-src/antlr/main")
            kotlin.srcDir(langPath)
            kotlin.srcDir("src/main/kotlin")
        }
    }
}

tasks.getByName("compileKotlin").dependsOn("generateGrammarSource")

dependencies {
    testImplementation(kotlin("test"))
    antlr("org.antlr:antlr4:4.9.3")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

buildscript {
    dependencies {
        // add the plugin to the classpath
        classpath("org.antlr:antlr4:4.9.3")
    }
}

application {
    mainClass.set("MainKt")
}

tasks.register("jsIrBrowserTest")
tasks.register("jsLegacyBrowserTest")
