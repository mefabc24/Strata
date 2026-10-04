package com.mefabc24.sandbox

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.mefabc24.sandbox.registration.registerSandboxEntities
import com.mefabc24.sandbox.registration.registerSandboxObjects
import com.mefabc24.sandbox.registration.registerSandboxSounds
import com.mefabc24.sandbox.registration.registerSandboxTerrain
import com.mefabc24.strata.world.World
import com.mefabc24.strata.Strata
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.placement.PlacementPreviewBoundsPolicy
import com.mefabc24.strata.world.WorldId

class SandboxGame : StrataGame() {

    override val strata: Strata =
        Strata().configure {
            engine {
                backgroundColor =
                    Color(0.53f, 0.81f, 0.92f, 1f)
            }

            scene(
                terrainDirectory = "tiles",
                objectDirectory = "objects",
                entityDirectory = "entities"
            ) {
                registrations {
                    terrain {
                        registerSandboxTerrain()
                    }

                    objects {
                        registerSandboxObjects()
                    }

                    entities {
                        registerSandboxEntities()
                    }

                    sounds {
                        registerSandboxSounds()
                    }
                }

                audio {
                    masterVolume = 1f
                    soundVolume = 1f
                    musicVolume = 1f

                    setCategoryVolume(
                        SoundCategory.BUILDING,
                        1f
                    )
                }

                debug {
                    presets {
                        storage("strata.sandbox.debug")
                        applySavedDefaultOnStartup = true
                    }

                    ui {
                        toolRail {
                            enabled = true
                            visibleOnStartup = false
                            toggleKey = Input.Keys.TAB
                        }

                        settingsWindow {
                            enabled = true
                            visibleOnStartup = false
                            toggleKey = Input.Keys.ESCAPE
                        }
                    }

                    visuals {
                        grid {
                            enabled = false
                            showBackground = true
                            showHoverBackground = true
                            color = Color(1f, 1f, 1f, 0.4f)
                            hoverColor = Color(1f, 0f, 0f, 1f)
                            backgroundColor =
                                Color(1f, 1f, 1f, 0.2f)
                            hoverBackgroundColor =
                                Color(1f, 0f, 0f, 0.5f)
                        }

                        objects {
                            showOccupiedTiles = true
                            showOriginTile = true
                            showOccupiedTileFill = true
                            occupiedTileFillColor =
                                Color(0.2f, 0.65f, 1f, 0.2f)
                        }

                        entities {
                            showCurrentTile = true
                            showPosition = true
                            showPath = true
                            showCurrentTileFill = true
                            currentTileFillColor =
                                Color(0.3f, 1f, 0.3f, 0.18f)
                        }
                    }

                    tools {
                        delete {
                            brushSize = 3
                        }

                        onEntitySpawned { entity ->
                            roaming.control(entity)
                            strata.events.publish(
                                SandboxDebugEntitySpawned(
                                    entityType = entity.entity::class.simpleName ?: "Entity",
                                    position = entity.currentTile
                                )
                            )
                        }

                        onObjectsPlaced { placed ->
                            if (placed.isNotEmpty()) {
                                playBuildingSound()
                            }
                            placed.forEach { objectInWorld ->
                                strata.events.publish(
                                    SandboxDebugObjectPlaced(
                                        objectType = objectInWorld.placeable::class.simpleName
                                            ?: "Object",
                                        position = com.mefabc24.strata.world.TilePosition(
                                            objectInWorld.x,
                                            objectInWorld.y
                                        )
                                    )
                                )
                            }
                        }
                    }

                    operations {
                        performance {
                            terminalLoggingIntervalSeconds = 2f
                        }
                    }
                }

                camera {
                    zoomEdgeAllowance = 0.3f
                }

                rendering {
                    tileGeometry {
                        width = TILE_GEOMETRY.width
                        height = TILE_GEOMETRY.height
                    }

                    objects {
                        offsetY = 1f
                    }
                }

                placement {
                    preview {
                        objects {
                            boundsPolicy = PlacementPreviewBoundsPolicy.ORIGIN_INSIDE
                            validColor = Color(0.35f, 0.75f, 0.3f, 0.7f)
                            invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
                        }
                    }
                }

            }
        }

    private lateinit var roaming: SandboxRoamingController
    private lateinit var sandboxWorld: World

    override fun onReady() {
        sandboxWorld = createSandboxWorld()
        strata.worlds.register(SANDBOX_WORLD, sandboxWorld, ::terrainFor)
        strata.worlds.activate(SANDBOX_WORLD)

        roaming = SandboxRoamingController(
            world = sandboxWorld,
            isHeld = strata.debug::isEntityHeld
        )
    }

    override fun updateGame(simulationDelta: Float) {
        roaming.update(simulationDelta)
    }

    private fun createSandboxWorld(): World {
        val world = World(WORLD_SIZE, WORLD_SIZE) { x, y ->
            val terrain = if (x in 12..18 && y in 12..18) {
                TerrainType.WATER
            } else {
                TerrainType.LOW_GRASS
            }

            SandboxTile(terrain)
        }

        // Create a temporary overlay to verify layered rendering.
        world.addOverlayLayer("demo")

        for (x in 23..25) {
            for (y in 23..25) {
                world.setOverlayTile(
                    layerId = "demo",
                    x = x,
                    y = y,
                    tile = SandboxTile(TerrainType.BUSH)
                )
            }
        }

        check(
            world.place(
                placeable = House(),
                x = 5,
                y = 5
            ) != null
        ) {
            "Failed to place test house."
        }

        return world
    }

    private fun terrainFor(tile: com.mefabc24.strata.world.Tile) =
        (tile as SandboxTile).terrain

    private fun playBuildingSound() {
        strata.audio.playSound(BuildingSound.PLACE)
    }

    private companion object {
        private val TILE_GEOMETRY = TileGeometry(
            width = 32f,
            height = 24f
        )

        private const val WORLD_SIZE = 50

        private val SANDBOX_WORLD = WorldId("sandbox")
    }
}
