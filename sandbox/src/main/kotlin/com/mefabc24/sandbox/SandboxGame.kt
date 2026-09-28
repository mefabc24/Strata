package com.mefabc24.sandbox

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.mefabc24.sandbox.input.sandboxInputBindings
import com.mefabc24.sandbox.registration.registerSandboxEntities
import com.mefabc24.sandbox.registration.registerSandboxObjects
import com.mefabc24.sandbox.registration.registerSandboxSounds
import com.mefabc24.sandbox.registration.registerSandboxTerrain
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.world.World
import com.mefabc24.strata.Strata
import com.mefabc24.strata.StrataGame

class SandboxGame : StrataGame() {

    override val strata =
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
                    performance {
                        enabled = false
                        intervalSeconds = 2f
                    }

                    grid {
                        enabled = false
                        color = Color(1f, 1f, 1f, 0.4f)
                        hoverColor = Color(1f, 0f, 0f, 1f)
                        lineWidth = 1f
                        backgroundColor =
                            Color(1f, 1f, 1f, 0.2f)
                        hoverBackgroundColor =
                            Color(1f, 0f, 0f, 0.5f)
                    }

                    objects {
                        enabled = false
                        showOccupiedTiles = true
                        showOriginTile = true
                        showSpriteBounds = false
                        occupiedTileColor =
                            Color(0.2f, 0.85f, 1f, 1f)
                        occupiedTileFillColor =
                            Color(0.2f, 0.65f, 1f, 0.2f)
                        originTileColor =
                            Color(1f, 0.35f, 0.2f, 1f)
                        spriteBoundsColor =
                            Color(1f, 0.2f, 0.75f, 1f)
                    }

                    entities {
                        enabled = false
                        showCurrentTile = true
                        showPosition = true
                        showPath = true
                        showDirection = false
                        showSpriteBounds = false
                        currentTileColor =
                            Color(0.4f, 1f, 0.3f, 1f)
                        currentTileFillColor =
                            Color(0.3f, 1f, 0.3f, 0.18f)
                        positionColor =
                            Color(1f, 0.3f, 0.2f, 1f)
                        pathColor =
                            Color(1f, 0.85f, 0.2f, 1f)
                        directionColor =
                            Color(0.3f, 0.75f, 1f, 1f)
                        spriteBoundsColor =
                            Color(1f, 0.3f, 0.9f, 1f)
                    }
                }

                camera {
                    zoomEdgeAllowance = 0.3f
                }

                rendering {
                    tileGeometry {
                        width = 32f
                        height = 24f
                    }

                    objects {
                        offsetY = 1f
                    }
                }

                placement {
                    showOutsideWorldPreviews = false
                    previewStyle = PlacementPreviewStyle(
                        validColor =
                            Color(0.35f, 0.75f, 0.3f, 0.7f),
                        invalidColor =
                            Color(1f, 0.25f, 0.25f, 0.7f)
                    )
                }

                controls {
                    gameplay {
                        bindings = sandboxInputBindings(
                            tools = { tools },
                            painter = { painter },
                            buildDrag = { buildDrag },
                            entitySpawner = { entitySpawner },
                            wolves = { wolves },
                            world = { sandboxWorld },
                            playBuildingSound = ::playBuildingSound
                        )
                    }
                }
            }
        }

    private lateinit var painter: SandboxTerrainPainter
    private lateinit var buildDrag: SandboxBuildDragController
    private lateinit var tools: SandboxToolController
    private lateinit var entitySpawner: SandboxEntitySpawner
    private lateinit var wolves: SandboxWolfController
    private lateinit var uiSkin: Skin
    private lateinit var sandboxUi: SandboxUi
    private lateinit var sandboxWorld: World

    override fun onReady() {
        sandboxWorld = createSandboxWorld()

        painter = SandboxTerrainPainter(
            sandboxWorld
        )

        strata.attachWorld(
            world = sandboxWorld,
            terrainFor = { tile ->
                (tile as SandboxTile).terrain
            }
        )

        strata.placement.selectedFactory =
            strata.objects.constructibleEntries
                .firstOrNull()
                ?.let { entry ->
                    entry::create
                }

        buildDrag =
            SandboxBuildDragController(
                strata.placement
            )
        tools = SandboxToolController(
            painter = painter,
            placement = strata.placement,
            buildDrag = buildDrag
        )
        entitySpawner = SandboxEntitySpawner(
            world = sandboxWorld,
            tools = tools,
            entries = sandboxSpawnEntries()
        )

        wolves = SandboxWolfController(
            sandboxWorld
        )

        uiSkin = SandboxUi.createSkin()

        strata.createUi(
            skin = uiSkin,
            theme = SandboxUi.createTheme()
        ) {
            sandboxUi = SandboxUi(
                ui = this,
                painter = painter,
                placementController = strata.placement,
                buildDragController = buildDrag,
                toolController = tools,
                entitySpawner = entitySpawner,
                debugSettings = strata.debug,
                renderStats = { strata.view.renderStats },
                terrainEntries = strata.terrain.entries,
                objectEntries =
                    strata.objects.constructibleEntries
            )
        }
    }

    override fun updateGame(delta: Float) {
        sandboxUi.update(delta)
        wolves.update(delta)
    }

    override fun disposeGame() {
        if (::uiSkin.isInitialized) {
            uiSkin.dispose()
        }
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

    private fun playBuildingSound() {
        strata.audio.playSound(BuildingSound.PLACE)
    }

    private companion object {
        const val WORLD_SIZE = 50
    }
}
