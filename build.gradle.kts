plugins {
    id("java")
}

group = "com.mcrp"
version = "1.0.0"
description = "A self-contained DarkRP-style roleplay gamemode for Paper."

java {
    toolchain {
        // Minecraft/Paper 26.x requires Java 25.
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Targets the latest stable Paper 26.2 ("Chaos Cubed") build.
    // Bump this if your server is running a newer Paper release.
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    jar {
        archiveBaseName.set("DarkRP")
    }
}
