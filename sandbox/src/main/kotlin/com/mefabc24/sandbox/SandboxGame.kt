package com.mefabc24.sandbox

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.mefabc24.strata.EngineSettings
import com.mefabc24.strata.StrataSceneGame
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.scene.StrataScene
import com.mefabc24.strata.pathfinding.findPath
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

class SandboxGame : StrataSceneGame<TerrainType, SoundCategory>() {
    override val engineSettings = EngineSettings().apply {
        backgroundColor = Color(0.53f, 0.81f, 0.92f, 1f)
    }

    private lateinit var painter: SandboxTerrainPainter
    private lateinit var buildDrag: SandboxBuildDragController
    private lateinit var uiSkin: Skin
    private lateinit var sandboxUi: SandboxUi
    private lateinit var debugWalker: WorldEntity

    override fun createScene(): StrataScene<TerrainType, SoundCategory> {
        val world = createSandboxWorld()
        painter = SandboxTerrainPainter(world)

        val createdScene = StrataScene<TerrainType, SoundCategory>(
            terrainDirectory = "tiles",
            objectDirectory = "objects"
        ) {
            terrain.register(
                TerrainType.GRASS,
                sprite = "grass.png"
            )

            terrain.register(
                TerrainType.BUSH,
                sprite = "bush.png"
            )

            terrain.register(
                TerrainType.LOW_GRASS,
                sprite = "lowgrass.png"
            )

            terrain.register(
                TerrainType.WATER,
                sprite = "water4.png"
            )

            terrain.register(
                TerrainType.ROCK
            )

            terrain.register(TerrainType.SAND)
            terrain.register(TerrainType.STONE)
            terrain.register(TerrainType.DIRT)

            terrain.registerAnimated(
                TerrainType.BUSH_ANIMATED,
                frames = listOf(
                    "bush_anim_0.png",
                    "bush_anim_1.png",
                    "bush_anim_2.png",
                    "bush_anim_3.png",
                    "bush_anim_4.png"
                ),
                frameDuration = 0.2f
            )

            entities.register<DebugWalker>(
                sprite = "flower1.png"
            ) {
                offsetY = 1f
            }

            objects.register<House>(
                sprite = "house.png",
                factory = ::House
            )

            objects.register<OakTree>(
                sprite = "oak.png",
                factory = ::OakTree
            ) {
                offsetY = 3f
            }

            objects.register<Villa>(
                sprite = "villa.png",
                factory = ::Villa
            ) {
                offsetY = -16f
                offsetX = -6f
            }

            objects.register<Pine>(
                sprite = "pine.png",
                factory = ::Pine
            ) {
                offsetY = 3f
            }

            objects.register<Trunk1>(
                sprite = "trunk1.png",
                factory = ::Trunk1
            ) {
                offsetY = 3f
            }

            objects.register<Trunk2>(
                sprite = "trunk2.png",
                factory = ::Trunk2
            ) {
                offsetY = 3f
            }

            objects.register<Trunk3>(
                sprite = "trunk3.png",
                factory = ::Trunk3
            ) {
                offsetY = 3f
            }

            objects.register<Trunk4>(
                sprite = "trunk4.png",
                factory = ::Trunk4
            ) {
                offsetY = 3f
            }

            objects.register<Flower1>(
                sprite = "flower1.png",
                factory = ::Flower1
            ) {
                offsetY = 3f
            }

            objects.register<Flower2>(
                sprite = "flower2.png",
                factory = ::Flower2
            ) {
                offsetY = 3f
            }

            objects.register<RockWater1>(
                sprite = "rock_water1.png",
                factory = ::RockWater1
            ) {
                offsetY = -3f
            }

            objects.register<RockWater2>(
                sprite = "rock_water2.png",
                factory = ::RockWater2
            ) {
                offsetY = -3f
            }

            objects.register<RockWater3>(
                sprite = "rock_water3.png",
                factory = ::RockWater3
            ) {
                offsetY = -3f
            }

            objects.register<Road1>(
                sprite = "road1.png",
                factory = ::Road1
            ) {
                offsetY = -9f

            }

            objects.register<Road2>(
                sprite = "road2.png",
                factory = ::Road2
            ) {
                offsetY = -9f

            }

            objects.register<RoadIntersection> (
                sprite = "road-intersection.png",
                factory = ::RoadIntersection
            ) {
                offsetY = -9f

            }

            sounds.register(
                id = BuildingSound.PLACE,
                path = "audio/pop.wav",
                category = SoundCategory.BUILDING
            )

            audio {
                masterVolume = 1f
                soundVolume = 1f
                musicVolume = 1f

                setCategoryVolume(SoundCategory.BUILDING, 1f)
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
                    backgroundColor = Color(1f, 1f, 1f, 0.2f)
                    hoverBackgroundColor = Color(1f, 0f, 0f, 0.5f)
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
                    validColor = Color(0.35f, 0.75f, 0.3f, 0.7f),
                    invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
                )
            }

            controls {
                gameplay {
                    bindings = sandboxBindings()
                }
            }
        }

        createdScene.attachWorld(
            world = world,
            terrainFor = { tile ->
                (tile as SandboxTile).terrain
            }
        )

        createdScene.placement.selectedFactory =
            createdScene.objects.constructibleEntries
                .firstOrNull()
                ?.let { entry ->
                    entry::create
                }

        buildDrag = SandboxBuildDragController(createdScene.placement)

        return createdScene
    }

    override fun onReady() {
        uiSkin = SandboxUi.createSkin()

        scene.createUi(
            skin = uiSkin,
            theme = SandboxUi.createTheme()
        ) {
            sandboxUi = SandboxUi(
                ui = this,
                painter = painter,
                placementController = scene.placement,
                buildDragController = buildDrag,
                debugSettings = scene.debug,
                terrainEntries = scene.terrain.entries,
                objectEntries = scene.objects.constructibleEntries
            )
        }
    }

    override fun updateGame(delta: Float) {
        sandboxUi.sync()

        if (!debugWalker.isMoving) {
            val destination = if (
                debugWalker.currentTile == DEBUG_WALKER_END
            ) {
                DEBUG_WALKER_START
            } else {
                DEBUG_WALKER_END
            }

            val path = requireNotNull(
                scene.world.findPath(
                    start = debugWalker.currentTile,
                    goal = destination
                )
            )
            debugWalker.followPath(path, DEBUG_WALKER_SPEED)
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
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                enabled = { painter.enabled }
            ) { x, y ->
                painter.dragPaint(x, y)
            },

            // Update the rectangular build preview while dragging.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                enabled = { !painter.enabled }
            ) { x, y ->
                buildDrag.dragTo(TilePosition(x, y))
            },

            // Commit the final build rectangle on release over the world.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
                enabled = { !painter.enabled }
            ) { x, y ->
                if (!buildDrag.active) {
                    false
                } else {
                    val placed = buildDrag.finish(TilePosition(x, y))

                    if (placed.isNotEmpty()) {
                        println("Placed ${placed.size} object(s)")
                        scene.audio.playSound(BuildingSound.PLACE)
                    }

                    true
                }
            },

            // Cancel the stroke if the cursor leaves the world.
            WorldInputBinding.NoPicking(
                trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                enabled = { painter.enabled }
            ) {
                painter.cancel()
                false
            },

            // Finish painting or cancel building when released off-world.
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
                scene.world.remove(placed)
                true
            },

            // Debug: inspect the ground tile.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.KeyDown(Input.Keys.P)
            ) { x, y ->
                println("Tile at ($x, $y): ${scene.world.getTile(x, y)}")
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
