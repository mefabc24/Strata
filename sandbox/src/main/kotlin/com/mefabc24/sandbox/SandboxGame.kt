package com.mefabc24.sandbox

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.PlacementPreviewStyle
import com.mefabc24.strata.scene.StrataScene
import com.mefabc24.strata.world.World

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var placementController: PlacementController
    private lateinit var scene: StrataScene<TerrainType, SoundCategory>
    private lateinit var painter: SandboxTerrainPainter
    private lateinit var ui: SandboxUi

    private val previewStyle = PlacementPreviewStyle(
        validColor = Color(0.3f, 0.8f, 1f, 0.7f),
        invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
    )

    // Debug
    private val worldSize = 50

    override fun create() {
        world = World(worldSize, worldSize) { x, y ->
            val terrain = if (x in 12..18 && y in 12..18) {
                TerrainType.WATER
            } else {
                TerrainType.GRASS
            }

            SandboxTile(terrain)
        }

        // Create a temporary elevated plateau.
        check(
            world.terrain.setHeight(
                xRange = 28..32,
                yRange = 28..32,
                level = 1
            )
        )

        // Create a temporary overlay to verify layered rendering.
        world.addOverlayLayer("demo")

        for (x in 23..25) {
            for (y in 23..25) {
                world.setOverlayTile(
                    layerId = "demo",
                    x = x,
                    y = y,
                    tile = SandboxTile(TerrainType.WATER)
                )
            }
        }

        placementController = PlacementController(
            world = world,
            style = previewStyle
        ).apply {
            selectedPlaceable = House()
        }

        painter = SandboxTerrainPainter(world)

        check(
            world.place(
                placeable = House(),
                x = 5,
                y = 5
            ) != null
        ) {
            "Failed to place test house."
        }

        scene = StrataScene<TerrainType, SoundCategory>(
            terrainDirectory = "tiles",
            objectDirectory = "objects"
        ) {
            terrain.register(
                TerrainType.GRASS,
                sprite = "grass.png"
            )

            terrain.register(
                TerrainType.WATER,
                sprite = "water3.png"
            )

            terrain.register(
                TerrainType.SAND,
                sprite = "flowers.png"
            )

            objects.register<OakTree>("oak.png") {
                offsetY = 5f
            }

            objects.register<House>("house.png")

            sounds.register(
                id = BuildingSound.PLACE,
                path = "audio/pop.wav",
                category = SoundCategory.BUILDING
            )
        }

        scene.createView(
            world = world,
            terrainFor = { tile ->
                (tile as SandboxTile).terrain
            }
        ) {
            audio {
                masterVolume = 1f
                soundVolume = 1f
                musicVolume = 1f

                setCategoryVolume(SoundCategory.BUILDING, 1f)
            }

            debug {
                performance {
                    enabled = true
                    intervalSeconds = 2f
                }
            }

            camera {
                zoomEdgeAllowance = 0.3f
            }

            rendering {
                tileGeometry {
                    width = 32f
                    height = 32f
                }
            }

            controls {
                gameplay {
                    bindings = listOf(
                        // Left click: paint terrain or place an object.
                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                        ) { x, y ->
                            if (painter.enabled) {
                                painter.beginPaint(x, y)
                            } else {
                                val placed = placementController.placeAt(x, y)

                                if (placed != null) {
                                    println("Object placed at ($x, $y)")
                                    scene.audio.playSound(BuildingSound.PLACE)
                                }

                                true
                            }
                        },

                        // Continue painting while dragging.
                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                            enabled = { painter.enabled }
                        ) { x, y ->
                            painter.dragPaint(x, y)
                        },

                        // Cancel the stroke if the cursor leaves the world.
                        WorldInputBinding.NoPicking(
                            trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                            enabled = { painter.enabled }
                        ) {
                            painter.cancel()
                            false
                        },

                        // Finish painting even when released outside the world.
                        WorldInputBinding.NoPicking(
                            trigger = WorldInputTrigger.MouseUp(Input.Buttons.LEFT)
                        ) {
                            painter.endPaint()
                        },

                        // Right click: begin erasing an overlay.
                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
                            enabled = {
                                painter.enabled && painter.layerId != null
                            }
                        ) { x, y ->
                            painter.beginErase(x, y)
                        },

                        // Continue erasing while dragging.
                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.MouseDrag(Input.Buttons.RIGHT),
                            enabled = {
                                painter.enabled && painter.layerId != null
                            }
                        ) { x, y ->
                            painter.dragErase(x, y)
                        },

                        // Cancel erasing if the cursor leaves the world.
                        WorldInputBinding.NoPicking(
                            trigger = WorldInputTrigger.MouseDrag(Input.Buttons.RIGHT),
                            enabled = { painter.enabled }
                        ) {
                            painter.cancel()
                            false
                        },

                        // Finish erasing even when released outside the world.
                        WorldInputBinding.NoPicking(
                            trigger = WorldInputTrigger.MouseUp(Input.Buttons.RIGHT)
                        ) {
                            painter.endErase()
                        },

                        // Remove objects only outside painting mode.
                        WorldInputBinding.Object(
                            trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
                            mode = ObjectPickingMode.SPRITE_OR_FOOTPRINT,
                            enabled = { !painter.enabled }
                        ) { placed ->
                            world.remove(placed)
                            true
                        },

                        // Debug: inspect the ground tile.
                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.KeyDown(Input.Keys.P)
                        ) { x, y ->
                            println("Tile at ($x, $y): ${world.getTile(x, y)}")
                            true
                        },

                        // Toggle painting mode.
                        WorldInputBinding.NoPicking(
                            trigger = WorldInputTrigger.KeyDown(Input.Keys.T)
                        ) {
                            painter.enabled = !painter.enabled

                            println("Terrain painting: ${painter.enabled}")
                            true
                        },

                        // Switch between ground and the demo overlay.
                        WorldInputBinding.NoPicking(
                            trigger = WorldInputTrigger.KeyDown(Input.Keys.O)
                        ) {
                            painter.layerId = if (painter.layerId == null) {
                                "demo"
                            } else {
                                null
                            }

                            println("Selected layer: ${painter.layerId ?: "ground"}")
                            true
                        }
                    )
                }
            }
        }

        ui = SandboxUi(
            painter = painter
        )

        scene.input.addUiProcessor(
            ui.inputProcessor
        )
    }

    override fun resize(
        width: Int,
        height: Int
    ) {
        if (!::scene.isInitialized) return

        scene.resize(
            width,
            height
        )

        if (::ui.isInitialized) {
            ui.resize(
                width,
                height
            )
        }
    }

    override fun update(delta: Float) {
        scene.update(delta)

        placementController.update(
            scene.view.hoveredTile
        )

        ui.update(delta)
    }

    override fun render() {
        scene.render(
            preview = if (painter.enabled) {
                null
            } else {
                placementController.preview
            }
        )

        ui.render()
    }

    override fun dispose() {
        if (::ui.isInitialized) {
            if (::scene.isInitialized) {
                scene.input.removeUiProcessor(
                    ui.inputProcessor
                )
            }

            ui.dispose()
        }

        if (::scene.isInitialized) {
            scene.dispose()
        }
    }
}