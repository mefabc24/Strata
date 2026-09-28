# Getting started

This chapter builds the smallest desktop application that displays a Strata world.

## Requirements and modules

The current build uses JDK 21, Kotlin 2.4.0, Gradle 9.6.0, and libGDX 1.14.2. The repository contains four Gradle modules:

| Module | Purpose |
| --- | --- |
| `engine:core` | The platform-independent Strata API and libGDX core dependency |
| `engine:desktop` | The LWJGL3 backend and `DesktopLauncher` |
| `engine:tools` | Build-time texture-atlas packing; it is not a runtime dependency |
| `sandbox` | A runnable example game using the other modules |

There is no configured remote artifact repository. During development, consume Strata from a source checkout with a composite build, or publish the `0.1.0-SNAPSHOT` publications to Maven Local.

For a composite build, place the Strata checkout next to the game and add it to the game's `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "MyGame"
includeBuild("../Strata")
```

Then use the module coordinates that the included build contributes:

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.mefabc24.strata:core:0.1.0-SNAPSHOT")
    implementation("com.mefabc24.strata:desktop:0.1.0-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("example.MainKt")
}
```

Alternatively, run `./gradlew :engine:core:publishToMavenLocal :engine:desktop:publishToMavenLocal` in Strata, add `mavenLocal()` before `mavenCentral()` in the game, and use the same dependencies. Re-publish after changing the engine. The Sandbox instead uses direct `project(":engine:core")` and `project(":engine:desktop")` dependencies because it lives in this repository.

## Resources

Strata resolves runtime assets from the classpath. A minimal game can use:

```text
src/main/
├── kotlin/
│   └── example/
│       ├── MinimalGame.kt
│       └── Main.kt
└── resources/
    └── tiles/
        └── grass.png
```

`terrainDirectory = "tiles"` makes the file registration `"grass.png"` resolve to `tiles/grass.png`. Object and entity file sources work the same way with their own base directories. Atlas paths are scene-level classpath paths and do not receive a registry directory prefix.

## A complete minimal game

```kotlin
package example

import com.mefabc24.strata.Strata
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World

enum class Ground : TerrainId {
    GRASS
}

data class GroundTile(
    val terrain: Ground
) : Tile

class MinimalGame : StrataGame() {
    override val strata = Strata().configure {
        scene(
            terrainDirectory = "tiles",
            objectDirectory = "objects",
            entityDirectory = "entities"
        ) {
            registrations {
                terrain {
                    register(Ground.GRASS, "grass.png")
                }
            }

            rendering {
                tileGeometry {
                    width = 64f
                    height = 64f
                }
            }
        }
    }

    override fun onReady() {
        val world = World(width = 20, height = 20) { _, _ ->
            GroundTile(Ground.GRASS)
        }

        strata.attachWorld(
            world = world,
            terrainFor = { tile ->
                (tile as GroundTile).terrain
            }
        )
    }
}
```

`World` stores game-owned `Tile` values. `terrainFor` translates each tile into the registered `TerrainId` used by rendering. It runs while terrain is rendered, so every ground and overlay tile that may appear must map to a registered terrain ID.

Launch it through the desktop backend:

```kotlin
package example

import com.mefabc24.strata.desktop.DesktopLauncher

fun main() {
    DesktopLauncher.launch(MinimalGame()) {
        title = "My Strata Game"
        width = 1280
        height = 720
        vsync = true
        foregroundFps = 60
    }
}
```

`DesktopSettings` defaults to the values above except for the title, whose default is `"Strata Engine"`. A foreground FPS of `0` is accepted by the backend configuration.

## What happens at startup

Construction configures the game, `DesktopLauncher` creates `StrataEngine`, and libGDX calls the lifecycle. `StrataGame.create()` creates the scene before calling `onReady()`. Scene creation performs registrations, loads assets synchronously, and prepares visuals. This is why world attachment belongs in `onReady()`, not inside the scene configuration block.

Strata updates entity movement, the view, placement previews, and UI before calling the game hooks. Override `updateGame(simulationDelta)` for game progression and `updateRealTime(realDelta)` for game-owned UI or other work that must continue while paused. It renders the world before the UI. Resize and disposal are forwarded automatically. Game resources that Strata does not own, such as a supplied UI `Skin`, belong in `disposeGame()`.

Continue with [Architecture](Architecture.md) for ownership and lifecycle, then [Configuration](Configuration.md) for all setup blocks.
