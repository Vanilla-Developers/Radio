# Vanilla+ Radio

A single Fabric mod codebase built for multiple Minecraft versions at once using
[Stonecutter](https://stonecutter.kikugie.dev/).

## Supported versions

| Node             | Minecraft            | Notes                                             |
|------------------|-----------------------|----------------------------------------------------|
| `1.21.8`         | 1.21.8                | Obfuscated, `getAnalogOutputSignal` old signature   |
| `1.21.9-1.21.10` | 1.21.9 - 1.21.10      | Obfuscated, shared Fabric API surface               |
| `1.21.11`        | 1.21.11               | Obfuscated, last version before `Identifier` rename |
| `26.1-26.2`      | 26.1 - 26.2           | Unobfuscated (year-based versioning), Java 25       |

Starting with Minecraft 26.1, Mojang switched to year-based versions (`26.1`, `26.2`, ...)
and ships Minecraft unobfuscated, which changes how Fabric Loom builds mods for it.
This project uses [`loom-back-compat`](https://codeberg.org/KikuGie/loom-back-compat) so a
single `build.gradle.kts` works for both obfuscated (`net.fabricmc.fabric-loom-remap`) and
unobfuscated (`net.fabricmc.fabric-loom`) versions.

## Building

```sh
# Build every supported Minecraft version; jars land in build/libs/<mc version>/
./gradlew chiseledBuild

# Build just one version
./gradlew :1.21.11:build
```

## Working on the code

The shared code lives in `src/`. Version-specific behaviour is controlled with Stonecutter's
versioned comments (`//? if ...`), see the [Versioned Comments guide](https://stonecutter.kikugie.dev/wiki/start/comments).

To edit code for a specific version in your IDE, switch the active version first (also available
as "Set active project to ..." tasks in your IDE's Gradle panel):

```sh
./gradlew "stonecutterSwitchTo1.21.8"
```

Run `./gradlew "stonecutterSwitchTo26.1-26.2"` before committing, so `src/` reflects the latest version.

Per-version dependency versions (Fabric Loader, Fabric API, Simple Voice Chat, fabric-permissions-api,
Loom) are centralized in `stonecutter.properties.toml`.

## Setup

For IDE setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up).

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
