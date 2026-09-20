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
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.World
import kotlin.math.abs

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var placementController: PlacementController
    private lateinit var scene: StrataScene<TerrainType, SoundCategory>

    private var terrainPaintingEnabled = false
    // Null selects the ground layer.
    private var selectedPaintLayerId: String? = null

    private var lastPaintedTile: Pair<Int, Int>? = null
    private var lastErasedTile: Pair<Int, Int>? = null

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

            SandboxTile(TerrainType.GRASS)
        }

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

        val house = PlacedObject(
            placeable = House(),
            x = 5,
            y = 5
        )

        check(world.placeObject(house)) {
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
                sprite = "grass.png"
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

        check(
            scene.terrain[TerrainType.GRASS].texture ===
                    scene.terrain[TerrainType.SAND].texture
        ) {
            "Terrain types using the same sprite must share a texture."
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
                tileWidth = 64f
                tileHeight = 32f
            }

            controls {
                gameplay {
                    bindings = listOf(
                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                        ) { x, y ->
                            if (terrainPaintingEnabled) {
                                lastPaintedTile = null
                                paintStroke(x, y)
                            } else {
                                val placed = placementController.placeAt(x, y)

                                if (placed != null) {
                                    println("Object placed at ($x, $y)")
                                    scene.audio.playSound(BuildingSound.PLACE)
                                } else {
                                    println("Cannot place object at ($x, $y)")
                                }
                            }

                            true
                        },

                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
                            enabled = { terrainPaintingEnabled }
                        ) { x, y ->
                            paintStroke(x, y)
                            true
                        },

                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
                            enabled = {
                                terrainPaintingEnabled && selectedPaintLayerId != null
                            }
                        ) { x, y ->
                            lastErasedTile = null
                            eraseStroke(x, y)
                            true
                        },

                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.MouseDrag(Input.Buttons.RIGHT),
                            enabled = {
                                terrainPaintingEnabled && selectedPaintLayerId != null
                            }
                        ) { x, y ->
                            eraseStroke(x, y)
                            true
                        },

                        WorldInputBinding.Object(
                            trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
                            mode = ObjectPickingMode.SPRITE_OR_FOOTPRINT,
                            enabled = { !terrainPaintingEnabled }
                        ) { placed ->
                            world.removeObject(placed)
                            true
                        },

                        WorldInputBinding.Tile(
                            trigger = WorldInputTrigger.KeyDown(Input.Keys.P)
                        ) { x, y ->
                            val tile = world.getTile(x, y)

                            println("Tile at ($x, $y): $tile")

                            true
                        },

                        WorldInputBinding.NoPicking(
                            trigger = WorldInputTrigger.KeyDown(Input.Keys.T)
                        ) {
                            terrainPaintingEnabled = !terrainPaintingEnabled

                            println(
                                "Terrain painting: " +
                                        if (terrainPaintingEnabled) "enabled" else "disabled"
                            )

                            true
                        },

                        WorldInputBinding.NoPicking(
                            trigger = WorldInputTrigger.KeyDown(Input.Keys.O)
                        ) {
                            selectedPaintLayerId = if (selectedPaintLayerId == null) {
                                "demo"
                            } else {
                                null
                            }

                            println("Selected paint layer: ${selectedPaintLayerId ?: "ground"}")

                            true
                        },
                    )
                }
            }
        }
    }

    private fun paintStroke(x: Int, y: Int) {
        val layerId = selectedPaintLayerId
        val tile = SandboxTile(TerrainType.WATER)

        forEachTileOnLine(lastPaintedTile, x to y) { tileX, tileY ->
            if (layerId == null) {
                world.setTile(tileX, tileY, tile)
            } else {
                world.setOverlayTile(
                    layerId = layerId,
                    x = tileX,
                    y = tileY,
                    tile = tile
                )
            }
        }

        lastPaintedTile = x to y
    }

    private fun eraseStroke(x: Int, y: Int) {
        val layerId = selectedPaintLayerId ?: return

        forEachTileOnLine(lastErasedTile, x to y) { tileX, tileY ->
            world.setOverlayTile(
                layerId = layerId,
                x = tileX,
                y = tileY,
                tile = null
            )
        }

        lastErasedTile = x to y
    }

    private fun forEachTileOnLine(
        from: Pair<Int, Int>?,
        to: Pair<Int, Int>,
        action: (x: Int, y: Int) -> Unit
    ) {
        var x = from?.first ?: to.first
        var y = from?.second ?: to.second

        val dx = abs(to.first - x)
        val dy = abs(to.second - y)

        val stepX = if (x < to.first) 1 else -1
        val stepY = if (y < to.second) 1 else -1

        var error = dx - dy

        while (true) {
            // Avoid processing the previous tile twice.
            if (from == null || x != from.first || y != from.second) {
                action(x, y)
            }

            if (x == to.first && y == to.second) {
                break
            }

            val doubleError = 2 * error

            if (doubleError > -dy) {
                error -= dy
                x += stepX
            }

            if (doubleError < dx) {
                error += dx
                y += stepY
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        if (!::scene.isInitialized) return

        scene.resize(width, height)
    }

    override fun update(delta: Float) {
        scene.update(delta)

        placementController.update(scene.view.hoveredTile)
    }

    override fun render() {
        scene.render(
            preview = if (terrainPaintingEnabled) {
                null
            } else {
                placementController.preview
            }
        )
    }

    override fun dispose() {
        if (::scene.isInitialized) {
            scene.dispose()
        }
    }
}