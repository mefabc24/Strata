package com.mefabc24.sandbox

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.audio.SoundRegistry
import com.mefabc24.strata.camera.ZoomAnchor
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.ObjectRegistry
import com.mefabc24.strata.render.PlacementPreviewStyle
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.input.StrataInput
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.World
import com.mefabc24.strata.audio.StrataAudio
import com.mefabc24.strata.scene.StrataScene

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var worldView: IsoWorldView
    private lateinit var placementController: PlacementController
    private lateinit var input: StrataInput
    private lateinit var scene: StrataScene<TerrainType, SoundCategory>

    private val previewStyle = PlacementPreviewStyle(
        validColor = Color(0.3f, 0.8f, 1f, 0.7f),
        invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
    )

    private var perfElapsed = 0f
    private var perfFrames = 0
    private var perfRenderMsSum = 0.0

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

        worldView = IsoWorldView(
            world = world,
            textureFor = { tile ->
                scene.terrain[(tile as SandboxTile).terrain]
            },
            objectVisualFor = scene.objects::get,
            tileWidth = 64f,
            tileHeight = 32f,
            zoomMode = ZoomMode.WORLD_BASED,
            zoomAnchor = ZoomAnchor.CURSOR,
            zoomEdgeAllowance = 0.3f,

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
            ),

            maxTerrainSpriteHeight = TerrainType.entries.maxOf { type ->
                val region = scene.terrain[type]

                64f * region.regionHeight / region.regionWidth
            }
        )

        input = StrataInput(worldView.inputProcessor)
        input.install()
    }


    override fun resize(width: Int, height: Int) {
        if (!::worldView.isInitialized) return

        worldView.resize(width, height)
    }

    override fun update(delta: Float) {
        worldView.update(delta)
        placementController.update(worldView.hoveredTile)
    }

    override fun render() {
        worldView.render(
            preview = placementController.preview
        )

        val stats = worldView.renderStats

        perfElapsed += Gdx.graphics.deltaTime
        perfFrames++
        perfRenderMsSum += stats.cpuRenderMs

        if (perfElapsed >= 2f) {
            val averageRenderMs = perfRenderMsSum / perfFrames

            Gdx.app.log(
                "StrataPerf",
                "FPS: ${Gdx.graphics.framesPerSecond} | " +
                        "CPU render: ${"%.2f".format(averageRenderMs)} ms | " +
                        "Tiles: ${stats.terrainDrawn}/${stats.terrainChecked} | " +
                        "Objects: ${stats.objectsDrawn}/${stats.objectsChecked} | " +
                        "Preview: ${stats.previewsDrawn} | " +
                        "Draw calls: ${stats.drawCalls}"
            )

            perfElapsed = 0f
            perfFrames = 0
            perfRenderMsSum = 0.0
        }
    }

    override fun dispose() {
        input.uninstall()

        worldView.dispose()
        scene.dispose()
    }
}