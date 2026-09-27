package com.mefabc24.sandbox

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.mefabc24.sandbox.registration.registerSandboxEntities
import com.mefabc24.sandbox.registration.registerSandboxObjects
import com.mefabc24.sandbox.registration.registerSandboxSounds
import com.mefabc24.sandbox.registration.registerSandboxTerrain
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.pathfinding.findPath
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity
import com.mefabc24.strata.Strata
import com.mefabc24.strata.StrataGame

class SandboxGame : StrataGame() {

    protected override val strata =
        Strata().configure {
            engine {
                backgroundColor =
                    Color(0.53f, 0.81f, 0.92f, 1f)
            }

            scene(
                terrainDirectory = "tiles",
                objectDirectory = "objects"
            ) {
                terrain {
                    registerSandboxTerrain()
                }

                entities {
                    registerSandboxEntities()
                }

                objects {
                    registerSandboxObjects()
                }

                sounds {
                    registerSandboxSounds()
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
                    previewStyle = PlacementPreviewStyle(
                        validColor =
                            Color(0.35f, 0.75f, 0.3f, 0.7f),
                        invalidColor =
                            Color(1f, 0.25f, 0.25f, 0.7f)
                    )
                }

                controls {
                    gameplay {
                        bindings = sandboxBindings()
                    }
                }
            }
        }

    private lateinit var painter: SandboxTerrainPainter
    private lateinit var buildDrag: SandboxBuildDragController
    private lateinit var uiSkin: Skin
    private lateinit var sandboxUi: SandboxUi
    private lateinit var debugWalker: WorldEntity

    override fun onReady() {
        val world = createSandboxWorld()
        painter = SandboxTerrainPainter(world)

        strata.attachWorld(
            world = world,
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
                debugSettings = strata.debug,
                terrainEntries = strata.terrain.entries,
                objectEntries =
                    strata.objects.constructibleEntries
            )
        }
    }

    override fun updateGame(delta: Float) {
        sandboxUi.sync()

        if (!debugWalker.isMoving) {
            val destination =
                if (
                    debugWalker.currentTile ==
                    DEBUG_WALKER_END
                ) {
                    DEBUG_WALKER_START
                } else {
                    DEBUG_WALKER_END
                }

            val path = requireNotNull(
                strata.world.findPath(
                    start = debugWalker.currentTile,
                    goal = destination
                )
            )

            debugWalker.followPath(
                path,
                DEBUG_WALKER_SPEED
            )
        }
    }

    override fun disposeGame() {
        if (::uiSkin.isInitialized) {
            uiSkin.dispose()
        }
    }

    private fun sandboxBindings(): List<WorldInputBinding> {
        return listOf(
            WorldInputBinding.Entity(
                trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
                enabled = { !painter.enabled }
            ) { entity ->
                if (entity.entity is DebugWalker) {
                    println("Picked debug walker at ${entity.position}")
                    true
                } else {
                    false
                }
            },

            // Paint terrain on the world grid.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
                enabled = { painter.enabled }
            ) { x, y ->
                painter.beginPaint(x, y)
            },

            // Begin rectangular object placement without committing yet.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
                enabled = { !painter.enabled }
            ) { x, y ->
                buildDrag.begin(TilePosition(x, y))
            },

            // Continue painting while dragging.
            WorldInputBinding.Grid(
                trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                enabled = { painter.enabled }
            ) { x, y ->
                painter.dragPaint(x, y)
            },

            // Update the rectangular build preview while dragging.
            WorldInputBinding.Grid(
                trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                enabled = { !painter.enabled }
            ) { x, y ->
                buildDrag.dragTo(TilePosition(x, y))
            },

            // Commit valid origins even when release is outside the world.
            WorldInputBinding.Grid(
                trigger = WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
                enabled = { !painter.enabled }
            ) { x, y ->
                if (!buildDrag.active) {
                    false
                } else {
                    val placed = buildDrag.finish(TilePosition(x, y))

                    if (placed.isNotEmpty()) {
                        println("Placed ${placed.size} object(s)")
                        strata.audio.playSound(BuildingSound.PLACE)
                    }

                    true
                }
            },

            // Finish painting even when released outside the world.
            WorldInputBinding.NoPicking(
                trigger = WorldInputTrigger.MouseUp(Input.Buttons.LEFT)
            ) {
                if (painter.enabled) {
                    painter.endPaint()
                } else {
                    buildDrag.cancel()
                }
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
            WorldInputBinding.Grid(
                trigger = WorldInputTrigger.MouseDrag(Input.Buttons.RIGHT),
                enabled = {
                    painter.enabled && painter.layerId != null
                }
            ) { x, y ->
                painter.dragErase(x, y)
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
                strata.world.remove(placed)
                true
            },

            // Debug: inspect the ground tile.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.KeyDown(Input.Keys.P)
            ) { x, y ->
                println("Tile at ($x, $y): ${strata.world.getTile(x, y)}")
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

            // Cycle ground and overlays in world rendering order.
            WorldInputBinding.NoPicking(
                trigger = WorldInputTrigger.KeyDown(Input.Keys.O)
            ) {
                painter.cycleLayer()

                println("Selected layer: ${painter.layerId ?: "ground"}")
                true
            }
        )
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

        debugWalker = world.addEntity(
            entity = DebugWalker(),
            position = EntityPosition.centerOf(DEBUG_WALKER_START)
        )
        debugWalker.followPath(
            path = requireNotNull(
                world.findPath(
                    start = DEBUG_WALKER_START,
                    goal = DEBUG_WALKER_END
                )
            ),
            speed = DEBUG_WALKER_SPEED
        )

        return world
    }

    private companion object {
        const val WORLD_SIZE = 50
        const val DEBUG_WALKER_SPEED = 2f
        val DEBUG_WALKER_START = TilePosition(2, 5)
        val DEBUG_WALKER_END = TilePosition(9, 5)
    }
}
