plugins {
    id("fabric-loom") version "1.7.+"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
}

version = "1.0.0"
group = "com.cobbledelete"

base {
    archivesName.set("cobble-pc-delete")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.cobblemon.dev/releases")
    maven("https://maven.impactdev.net/repository/development/")
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings("net.fabricmc:yarn:1.21.1+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.9")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.106.1+1.21.1")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.12.3+kotlin.2.0.21")
    modCompileOnly("com.cobblemon:cobblemon-fabric:1.7.3+1.21.1")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

kotlin {
    jvmToolchain(21)
}
