package com.mefabc24.sandbox

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.camera.ZoomAnchor
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.PlacementPreviewStyle
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.World
import com.mefabc24.strata.scene.StrataScene

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var placementController: PlacementController
    private lateinit var scene: StrataScene<TerrainType, SoundCategory>

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
            performance.enabled = true
            performance.intervalSeconds = 2f

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

        scene.audio.apply {
            masterVolume = 0.8f
            soundVolume = 0.7f
            musicVolume = 0.5f

            setCategoryVolume(
                SoundCategory.BUILDING,
                0.6f
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
            tileWidth = 64f
            tileHeight = 32f

            zoomMode = ZoomMode.WORLD_BASED
            zoomAnchor = ZoomAnchor.CURSOR
            zoomEdgeAllowance = 0.3f

            controls {
                dragButton = Input.Buttons.MIDDLE

                keyboardMovementEnabled = true
                mouseDraggingEnabled = true
                zoomEnabled = true
            }

            bindings = listOf(
                WorldInputBinding.Tile(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) { x, y ->
                    val placed = placementController.placeAt(x, y)

                    if (placed != null) {
                        println("Object placed at ($x, $y)")

                        scene.audio.playSound(BuildingSound.PLACE)
                    } else {
                        println("Cannot place object at ($x, $y)")
                    }

                    true
                },

                WorldInputBinding.Object(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
                    mode = ObjectPickingMode.SPRITE_OR_FOOTPRINT
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
                }
            )
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
            preview = placementController.preview
        )
    }

    override fun dispose() {
        if (::scene.isInitialized) {
            scene.dispose()
        }
    }
}