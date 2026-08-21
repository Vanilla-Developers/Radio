pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Multi-version build orchestration. Check latest at https://stonecutter.kikugie.dev/blog/changes/0.9
    id("dev.kikugie.stonecutter") version "0.9.7"

    // Lets a single build script apply the right Loom variant for obfuscated (<26.1)
    // and unobfuscated (>=26.1) Minecraft versions. https://codeberg.org/KikuGie/loom-back-compat
    id("dev.kikugie.loom-back-compat") version "0.4.2"

    // Ensures a compatible JDK toolchain is available even if not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Real dependency (Minecraft) versions this mod is built against.
        // 1.21.9/1.21.10 share one API surface, so a single node covers both.
        versions("1.21.8", "1.21.11")
        version("1.21.9-1.21.10", "1.21.10")
        // Minecraft switched to year-based versioning after 1.21.11 (26.1, 26.2, ...).
        version("26.1-26.2", "26.2")
        vcsVersion = "26.1-26.2"
    }
}

rootProject.name = "vpl"
