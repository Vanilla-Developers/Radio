plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1-26.2"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}

// Builds every supported Minecraft version and collects the jars under build/libs/<mc version>/
tasks.register("chiseledBuild") {
    group = "build"
    description = "Builds the mod for every registered Minecraft version"
    dependsOn(stonecutter.tasks.named("buildAndCollect"))
}
