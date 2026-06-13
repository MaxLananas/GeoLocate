plugins {
    java
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.8"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()
description = providers.gradleProperty("description").get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
            "description" to project.description
        )
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    minimize()
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "geolocate"
            version = project.version.toString()
        }
    }
}
