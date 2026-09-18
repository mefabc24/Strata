package com.mefabc24.sandbox

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.camera.CameraBounds
import com.mefabc24.strata.camera.CameraController
import com.mefabc24.strata.camera.CameraViewport
import com.mefabc24.strata.camera.ViewportMode
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
    private lateinit var camera: OrthographicCamera
    private lateinit var projection: IsoProjection
    private lateinit var cameraController: CameraController
    private lateinit var viewport: CameraViewport
    private lateinit var bounds: CameraBounds
    private lateinit var tilePicker: TilePicker
    private lateinit var tileRenderer: IsoTileRenderer
    private lateinit var grassTexture: Texture
    private lateinit var grassRegion: TextureRegion

    private var hoveredTile: Pair<Int, Int>? = null
    private var selectedTile: Pair<Int, Int>? = null

    // Debug
    private val worldSize = 10

    override fun create() {
        world = World(worldSize, worldSize) { _, _ ->
            SandboxTile(TerrainType.GRASS)
        }

        projection = IsoProjection(
            tileWidth = 64f,
            tileHeight = 32f
        )

        camera = OrthographicCamera().apply {
            setToOrtho(false, 1280f, 720f)
        }

        tilePicker = TilePicker(
            camera = camera,
            projection = projection,
            world = world
        )

        val worldBounds = projection.worldBounds(
            width = world.width,
            height = world.height,
            padding = 100f
        )

        bounds = CameraBounds(
            minX = worldBounds.x,
            minY = worldBounds.y,
            maxX = worldBounds.x + worldBounds.width,
            maxY = worldBounds.y + worldBounds.height
        )

        viewport = CameraViewport(
            camera = camera,
            mode = ViewportMode.FIXED_HEIGHT,
            virtualHeight = 720f,
            bounds = bounds
        )

        viewport.resize(
            Gdx.graphics.width,
            Gdx.graphics.height
        )

        cameraController = CameraController(
            camera = camera,
            bounds = bounds,
            zoomMode = ZoomMode.WORLD_BASED,
            worldZoomBounds = projection.worldBounds(
                width = world.width,
                height = world.height,
                padding = 0f
            ),
            worldFill = 0.85f
        )

        cameraController.refreshZoomBounds()

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
            cameraController.inputProcessor
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
        if (!::viewport.isInitialized) return

        viewport.resize(width, height)

        if (::cameraController.isInitialized) {
            cameraController.refreshZoomBounds()
        }
    }

    override fun update(delta: Float) {
        cameraController.update(delta)

        hoveredTile = tilePicker.pick(
            Gdx.input.x.toFloat(),
            Gdx.input.y.toFloat()
        )
    }

    override fun render() {
        tileRenderer.render(
            world = world,
            camera = camera,
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