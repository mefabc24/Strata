package com.mefabc24.sandbox

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.camera.CameraBounds
import com.mefabc24.strata.camera.CameraController
import com.mefabc24.strata.camera.CameraViewport
import com.mefabc24.strata.camera.ViewportMode
import com.mefabc24.strata.iso.IsoProjection
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.render.IsoGridRenderer
import com.mefabc24.strata.world.World

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var camera: OrthographicCamera
    private lateinit var renderer: IsoGridRenderer
    private lateinit var projection: IsoProjection
    private lateinit var cameraController: CameraController
    private lateinit var viewport: CameraViewport
    private lateinit var bounds: CameraBounds

    private val mousePosition = Vector3()
    private var hoveredTile: Pair<Int, Int>? = null

    override fun create() {
        world = World(50, 50) { _, _ ->
            SandboxTile(TerrainType.GRASS)
        }

        projection = IsoProjection(
            tileWidth = 64f,
            tileHeight = 32f
        )

        camera = OrthographicCamera().apply {
            setToOrtho(false, 1280f, 720f)
        }

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
            bounds = bounds
        )

        Gdx.input.inputProcessor = cameraController.inputProcessor

        renderer = IsoGridRenderer(projection)
    }


    override fun resize(width: Int, height: Int) {
        if (!::viewport.isInitialized) return

        viewport.resize(width, height)
    }

    override fun update(delta: Float) {
        cameraController.update(delta)

        mousePosition.set(
            Gdx.input.x.toFloat(),
            Gdx.input.y.toFloat(),
            0f
        )

        camera.unproject(mousePosition)

        val (x, y) = projection.worldToTile(
            mousePosition.x,
            mousePosition.y
        )

        hoveredTile = if (world.getTile(x, y) != null) {
            x to y
        } else {
            null
        }
    }

    override fun render() {
        renderer.render(
            world = world,
            camera = camera,
            hoveredTile = hoveredTile
        )
    }

    override fun dispose() {
        Gdx.input.inputProcessor = null
        renderer.dispose()
    }
}