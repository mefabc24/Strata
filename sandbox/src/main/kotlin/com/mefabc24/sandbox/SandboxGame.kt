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
                    showOutsideWorldPreviews = true
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
    private lateinit var tools: SandboxToolController
    private lateinit var entitySpawner: SandboxEntitySpawner
    private lateinit var uiSkin: Skin
    private lateinit var sandboxUi: SandboxUi
    private lateinit var wolf: WorldEntity

    private var wolfPatrolIndex = 0
    private var wolfIdleRemaining = WOLF_IDLE_SECONDS

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
        tools = SandboxToolController(
            painter = painter,
            placement = strata.placement,
            buildDrag = buildDrag
        )
        entitySpawner = SandboxEntitySpawner(
            world = world,
            tools = tools,
            entries = sandboxSpawnEntries()
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
                terrainEntries = strata.terrain.entries,
                objectEntries =
                    strata.objects.constructibleEntries
            )
        }
    }

    override fun updateGame(delta: Float) {
        sandboxUi.sync()
        updateWolfPatrol(delta)
    }

    private fun updateWolfPatrol(delta: Float) {
        if (wolf.isMoving) {
            return
        }

        if (wolfIdleRemaining > 0f) {
            wolfIdleRemaining =
                (wolfIdleRemaining - delta).coerceAtLeast(0f)
            return
        }

        val nextIndex =
            (wolfPatrolIndex + 1) % WOLF_PATROL.size

        val destination = WOLF_PATROL[nextIndex]

        val path = requireNotNull(
            strata.world.findPath(
                start = wolf.currentTile,
                goal = destination
            )
        )

        wolf.followPath(
            path = path,
            speed = WOLF_SPEED
        )

        wolfPatrolIndex = nextIndex
        wolfIdleRemaining = WOLF_IDLE_SECONDS
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
                enabled = {
                    tools.mode == SandboxMode.NONE ||
                        tools.mode == SandboxMode.BUILD
                }
            ) { entity ->
                if (entity.entity is Wolf) {
                    println(
                        "Picked wolf at ${entity.position}, " +
                                "direction=${entity.direction}, " +
                                "moving=${entity.isMoving}"
                    )
                    true
                } else {
                    false
                }
            },

            // Spawn the selected Sandbox entity at the clicked tile center.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
                enabled = { tools.mode == SandboxMode.SPAWN }
            ) { x, y ->
                val entity = entitySpawner.spawn(TilePosition(x, y))
                    ?: return@Tile false
                println(
                    "Spawned ${entity.entity::class.simpleName} " +
                        "at ${entity.position}"
                )
                true
            },

            // Paint terrain on the world grid.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
                enabled = { tools.mode == SandboxMode.PAINT }
            ) { x, y ->
                painter.beginPaint(x, y)
            },

            // Begin rectangular object placement without committing yet.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
                enabled = { tools.mode == SandboxMode.BUILD }
            ) { x, y ->
                buildDrag.begin(TilePosition(x, y))
            },

            // Continue painting while dragging.
            WorldInputBinding.Grid(
                trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                enabled = { tools.mode == SandboxMode.PAINT }
            ) { x, y ->
                painter.dragPaint(x, y)
            },

            // Update the rectangular build preview while dragging.
            WorldInputBinding.Grid(
                trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                enabled = { tools.mode == SandboxMode.BUILD }
            ) { x, y ->
                buildDrag.dragTo(TilePosition(x, y))
            },

            // Commit valid origins even when release is outside the world.
            WorldInputBinding.Grid(
                trigger = WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
                enabled = { tools.mode == SandboxMode.BUILD }
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
                trigger = WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
                enabled = { tools.mode == SandboxMode.PAINT }
            ) {
                painter.endPaint()
            },

            // Right click: begin erasing an overlay.
            WorldInputBinding.Tile(
                trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
                enabled = {
                    tools.mode == SandboxMode.PAINT &&
                        painter.layerId != null
                }
            ) { x, y ->
                painter.beginErase(x, y)
            },

            // Continue erasing while dragging.
            WorldInputBinding.Grid(
                trigger = WorldInputTrigger.MouseDrag(Input.Buttons.RIGHT),
                enabled = {
                    tools.mode == SandboxMode.PAINT &&
                        painter.layerId != null
                }
            ) { x, y ->
                painter.dragErase(x, y)
            },

            // Finish erasing even when released outside the world.
            WorldInputBinding.NoPicking(
                trigger = WorldInputTrigger.MouseUp(Input.Buttons.RIGHT),
                enabled = { tools.mode == SandboxMode.PAINT }
            ) {
                painter.endErase()
            },

            // Remove objects only outside painting mode.
            WorldInputBinding.Object(
                trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
                mode = ObjectPickingMode.SPRITE_OR_FOOTPRINT,
                enabled = {
                    tools.mode == SandboxMode.NONE ||
                        tools.mode == SandboxMode.BUILD
                }
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

            // Toggle painting mode while keeping the mode controller authoritative.
            WorldInputBinding.NoPicking(
                trigger = WorldInputTrigger.KeyDown(Input.Keys.T)
            ) {
                val mode = if (tools.mode == SandboxMode.PAINT) {
                    SandboxMode.NONE
                } else {
                    SandboxMode.PAINT
                }
                tools.select(mode)

                println("Sandbox mode: ${mode.displayName}")
                true
            },

            // Cycle ground and overlays in world rendering order.
            WorldInputBinding.NoPicking(
                trigger = WorldInputTrigger.KeyDown(Input.Keys.O),
                enabled = { tools.mode == SandboxMode.PAINT }
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

        wolf = world.addEntity(
            entity = Wolf(),
            position = EntityPosition.centerOf(WOLF_PATROL.first())
        )

        return world
    }

    private companion object {
        const val WORLD_SIZE = 50

        const val WOLF_SPEED = 2f
        const val WOLF_IDLE_SECONDS = 2f

        val WOLF_PATROL = listOf(
            TilePosition(2, 2),
            TilePosition(8, 2),
            TilePosition(8, 8),
            TilePosition(2, 8)
        )
    }
}
