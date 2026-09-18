package com.mefabc24.sandbox

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.camera.ZoomAnchor
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.render.ObjectVisual
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.World

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var worldView: IsoWorldView
    private lateinit var terrainRegistry: TerrainRegistry<TerrainType>

    private var hoveredTile: Pair<Int, Int>? = null
    private var selectedTile: Pair<Int, Int>? = null
    private var activeTerrain: TerrainType? = null

    private lateinit var oakTexture: Texture
    private lateinit var houseTexture: Texture

    private lateinit var oakRegion: TextureRegion
    private lateinit var houseRegion: TextureRegion

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

        val house = PlacedObject(
            placeable = House(),
            x = 5,
            y = 5
        )

        check(world.placeObject(house)) {
            "Failed to place test house."
        }

        worldView = IsoWorldView(
            world = world,
            tileWidth = 64f,
            tileHeight = 32f,
            zoomMode = ZoomMode.WORLD_BASED,
            zoomAnchor = ZoomAnchor.CURSOR,
            zoomEdgeAllowance = 0.3f,

            onLeftClick = { x, y ->
                val tree = PlacedObject(
                    placeable = OakTree(),
                    x = x,
                    y = y
                )

                if (world.placeObject(tree)) {
                    println("Tree placed at ($x, $y)")
                } else {
                    println("Cannot place tree at ($x, $y)")
                }

                true
            },

            onRightClick = { x, y ->
                val placedObject = world.getObjectAt(x, y)

                when (placedObject?.placeable) {
                    is OakTree -> println("Oak tree at ($x, $y)")
                    is House -> println("House at ($x, $y)")
                    else -> println("No object at ($x, $y)")
                }

                true
            }
        )

        Gdx.input.inputProcessor = worldView.inputProcessor

        terrainRegistry = TerrainRegistry<TerrainType>(
            directory = "tiles"
        ).apply {
            register(TerrainType.GRASS, sprite = "grass.png")
            register(TerrainType.WATER, sprite = "water3.png")
            register(TerrainType.SAND, sprite = "grass.png")
        }

        oakTexture = Texture(
            Gdx.files.classpath("objects/oak.png")
        ).apply {
            setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
            )
        }

        houseTexture = Texture(
            Gdx.files.classpath("objects/house3.png")
        ).apply {
            setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
            )
        }

        oakRegion = TextureRegion(oakTexture)
        houseRegion = TextureRegion(houseTexture)
    }


    override fun resize(width: Int, height: Int) {
        if (!::worldView.isInitialized) return

        worldView.resize(width, height)
    }

    override fun update(delta: Float) {
        worldView.update(delta)

        hoveredTile = worldView.pickTile(
            Gdx.input.x.toFloat(),
            Gdx.input.y.toFloat()
        )
    }

    override fun render() {
        worldView.render(
            textureFor = { tile ->
                terrainRegistry[(tile as SandboxTile).terrain]
            },
            raisedTile = hoveredTile,
            raiseOffsetY = 6f,

            objectVisualFor = { placed ->
                when (placed.placeable) {
                    is OakTree -> ObjectVisual(
                        texture = oakRegion,
                        offsetY = 5f
                    )

                    is House -> ObjectVisual(
                        texture = houseRegion
                    )

                    else -> null
                }
            }
        )
    }

    override fun dispose() {
        Gdx.input.inputProcessor = null

        oakTexture.dispose()
        houseTexture.dispose()

        terrainRegistry.dispose()
        worldView.dispose()
    }

}