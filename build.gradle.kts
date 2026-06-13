plugins {
    java
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.8"
}

group       = providers.gradleProperty("group").get()
version     = providers.gradleProperty("version").get()
description = providers.gradleProperty("description").get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xlint:-processing",
            "-Xlint:-serial",
            "-Werror"
        )
    )
}

tasks.named<Javadoc>("javadoc") {
    isFailOnError = false
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
        encoding = "UTF-8"
        charSet  = "UTF-8"
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
            "version"     to project.version,
            "description" to project.description
        )
    }
    // Ensure reproducible builds — strip timestamps from resource copies
    filesMatching("**/*.yml") {
        filter { line -> line }
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    minimize()
    // Ensure reproducible JAR output
    isPreserveFileTimestamps = false
    isReproducibleFileOrder  = true
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

// Ensure the plain jar is never published — only the shadow jar
tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.named("shadowJar"))
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

            groupId    = project.group.toString()
            artifactId = "geolocate"
            version    = project.version.toString()

            pom {
                name.set("GeoLocate")
                description.set(project.description)
                url.set("https://github.com/MaxLananas/GeoLocate")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("MaxLananas")
                        name.set("MaxLananas")
                    }
                }
            }
        }
    }
}
