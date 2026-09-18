package com.mefabc24.sandbox

import com.badlogic.gdx.Gdx
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.camera.ZoomMode
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.camera.ZoomAnchor
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.world.World

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var worldView: IsoWorldView
    private lateinit var terrainRegistry: TerrainRegistry<TerrainType>

    private var hoveredTile: Pair<Int, Int>? = null
    private var selectedTile: Pair<Int, Int>? = null

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

        worldView = IsoWorldView(
            world = world,
            tileWidth = 64f,
            tileHeight = 32f,
            zoomMode = ZoomMode.WORLD_BASED,
            zoomAnchor = ZoomAnchor.CURSOR,
            zoomEdgeAllowance = 0.3f,

            onLeftClick = { x, y ->
                selectedTile = x to y
                true
            },

            onRightClick = { x, y ->
                println("Inspect tile: ($x, $y)")
                true
            }
        )

        Gdx.input.inputProcessor = worldView.inputProcessor

        terrainRegistry = TerrainRegistry<TerrainType>(
            directory = "tiles"
        ).apply {
            register(TerrainType.GRASS)
            register(TerrainType.WATER)
            register(TerrainType.SAND, sprite = "grass.png")
        }
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
            raiseOffsetY = 6f
        )
    }

    override fun dispose() {
        Gdx.input.inputProcessor = null

        terrainRegistry.dispose()
        worldView.dispose()
    }

}