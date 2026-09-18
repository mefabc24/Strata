package com.mefabc24.sandbox

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.input.TileInputProcessor
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TilePicker
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.render.IsoTileRenderer
import com.mefabc24.strata.world.World

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var projection: IsoProjection
    private lateinit var worldView: IsoWorldView
    private lateinit var tilePicker: TilePicker
    private lateinit var tileRenderer: IsoTileRenderer
    private lateinit var grassTexture: Texture
    private lateinit var grassRegion: TextureRegion

    private var hoveredTile: Pair<Int, Int>? = null
    private var selectedTile: Pair<Int, Int>? = null

    // Debug
    private val worldSize = 50

    override fun create() {
        world = World(worldSize, worldSize) { _, _ ->
            SandboxTile(TerrainType.GRASS)
        }

        projection = IsoProjection(
            tileWidth = 64f,
            tileHeight = 32f
        )

        worldView = IsoWorldView(
            world = world,
            projection = projection,
            zoomMode = ZoomMode.WORLD_BASED
        )

        tilePicker = TilePicker(
            camera = worldView.camera,
            projection = projection,
            world = world
        )

        Gdx.input.inputProcessor = InputMultiplexer(
            TileInputProcessor(
                tilePicker = tilePicker,

                onLeftClick = { x, y ->
                    selectedTile = x to y
                    true
                },

                onRightClick = { x, y ->
                    println("Inspect tile: ($x, $y)")
                    true
                }
            ),
            worldView.cameraController.inputProcessor
        )

        tileRenderer = IsoTileRenderer(projection)

        grassTexture = Texture(
            Gdx.files.classpath("tiles/grass.png")
        ).apply {
            setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
            )
        }

        grassRegion = TextureRegion(grassTexture)
    }


    override fun resize(width: Int, height: Int) {
        if (!::worldView.isInitialized) return

        worldView.resize(width, height)
    }

    override fun update(delta: Float) {
        worldView.update(delta)

        hoveredTile = tilePicker.pick(
            Gdx.input.x.toFloat(),
            Gdx.input.y.toFloat()
        )
    }

    override fun render() {
        tileRenderer.render(
            world = world,
            camera = worldView.camera,
            textureFor = { grassRegion },
            raisedTile = hoveredTile,
            raiseOffsetY = 6f
        )
    }

    override fun dispose() {
        Gdx.input.inputProcessor = null

        tileRenderer.dispose()
        grassTexture.dispose()
    }

}