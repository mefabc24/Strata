package com.mefabc24.sandbox

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.assets.StrataAssets
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

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var worldView: IsoWorldView
    private lateinit var terrainRegistry: TerrainRegistry<TerrainType>
    private lateinit var placementController: PlacementController
    private lateinit var input: StrataInput
    private lateinit var objectRegistry: ObjectRegistry
    private lateinit var assets: StrataAssets

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

        assets = StrataAssets()

        terrainRegistry = TerrainRegistry<TerrainType>(
            directory = "tiles",
            assets = assets
        ).apply {
            register(TerrainType.GRASS, sprite = "grass.png")
            register(TerrainType.WATER, sprite = "water3.png")
            register(TerrainType.SAND, sprite = "grass.png")
        }

        objectRegistry = ObjectRegistry(
            directory = "objects",
            assets = assets
        ).apply {
            register<OakTree>("oak.png") {
                offsetY = 5f
            }

            register<House>("house.png")
        }

        assets.finishLoading()

        terrainRegistry.prepare()
        objectRegistry.prepare()

        check(
            terrainRegistry[TerrainType.GRASS].texture ===
                    terrainRegistry[TerrainType.SAND].texture
        ) {
            "Terrain types using the same sprite must share a texture."
        }

        worldView = IsoWorldView(
            world = world,
            textureFor = { tile ->
                terrainRegistry[(tile as SandboxTile).terrain]
            },
            objectVisualFor = objectRegistry::get,
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
                val region = terrainRegistry[type]

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
        assets.dispose()
    }

}