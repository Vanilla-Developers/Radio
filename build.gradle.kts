plugins {
    // Applies `net.fabricmc.fabric-loom-remap` on obfuscated versions (<26.1, Loom's classic
    // remap flow) and `net.fabricmc.fabric-loom` on unobfuscated versions (>=26.1) automatically.
    id("dev.kikugie.loom-back-compat")
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.8.7"
}

val modId = property("mod.id") as String
val modName = property("mod.name") as String
val modGroup = property("mod.group") as String
val modMcCompat = property("mod.mc_compat") as String

version = "${property("mod.version")}+${sc.current.version}"
group = modGroup
base.archivesName = modId

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    mavenCentral()
    maven("https://maven.maxhenkel.de/repository/public")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang mappings on obfuscated versions; no-op (unobfuscated) on 26.1+.
    loomx.applyMojangMappings()

    // `mod{Config}` still works on 26.1+: loom-back-compat remaps it to the plain configuration.
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    // References CommandSourceStack, so it needs remapping on obfuscated (<26.1) versions.
    modImplementation("me.lucko:fabric-permissions-api:${property("deps.permissions_api")}")

    implementation("de.maxhenkel.voicechat:voicechat-api:${property("deps.voicechat_api")}")
    modRuntimeOnly("maven.modrinth:simple-voice-chat:${property("deps.voicechat")}")
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = requiredJava.majorVersion.toInt()
}

val mixinJava = "JAVA_${requiredJava.majorVersion}"

tasks.processResources {
    val props = mapOf(
        "id" to modId,
        "name" to modName,
        "version" to project.version.toString(),
        "minecraft" to modMcCompat,
    )
    inputs.properties(props)

    filesMatching("fabric.mod.json") { expand(props) }

    inputs.property("mixinJava", mixinJava)
    filesMatching("*.mixins.json") { expand("java" to mixinJava) }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_$modId" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = modId
            from(components["java"])
        }
    }
    repositories {
        // Add repositories to publish to here.
    }
}

if (project != rootProject) {
    val mcReleases: List<String> = sc.properties.raw("mod:mc_releases").to()

    modrinth {
        token.set(System.getenv("MODRINTH_TOKEN") ?: "")
        projectId.set("vanilla_radio")
        versionNumber.set("${property("mod.version")}-${project.name}")
        versionName.set("${property("mod.name")} ${property("mod.version")} for ${project.name}")
        versionType.set("release")
        uploadFile.set(loomx.modJar.flatMap { it.archiveFile })
        gameVersions.set(mcReleases)
        loaders.set(listOf("fabric"))
        changelog.set("Automated Stonecutter build for ${project.name}.")
    }
} else {
    tasks.named("modrinth") { enabled = false }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds mod jars for the active version and copies them to build/libs/{mc version}/"
    from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs/${sc.current.version}"))
}
