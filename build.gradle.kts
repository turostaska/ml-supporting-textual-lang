import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
    antlr
}

group = "me.albert"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin")
            srcDirs("src/main/java")
        }
    }
}

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
