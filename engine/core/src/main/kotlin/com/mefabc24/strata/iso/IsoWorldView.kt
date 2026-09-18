package com.mefabc24.strata.iso

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.camera.CameraBounds
import com.mefabc24.strata.camera.CameraController
import com.mefabc24.strata.camera.CameraViewport
import com.mefabc24.strata.camera.ViewportMode
import com.badlogic.gdx.InputMultiplexer
import com.mefabc24.strata.input.TileInputProcessor
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.camera.ZoomAnchor
import com.mefabc24.strata.render.IsoTileRenderer
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.world.World

/**
 * Manages the camera and viewport for an isometric world.
 */
class IsoWorldView(
    private val world: World,
    private val projection: IsoProjection,
    viewportMode: ViewportMode = ViewportMode.FIXED_HEIGHT,
    virtualHeight: Float = 720f,
    zoomMode: ZoomMode = ZoomMode.WORLD_BASED,
    zoomAnchor: ZoomAnchor = ZoomAnchor.CURSOR,
    cameraPadding: Float = 100f,
    worldFill: Float = 0.85f,
    onLeftClick: ((x: Int, y: Int) -> Boolean)? = null,
    onRightClick: ((x: Int, y: Int) -> Boolean)? = null
) {
    val camera = OrthographicCamera()

    private val worldBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = 0f
    )

    private val tileRenderer = IsoTileRenderer(projection)

    private val cameraBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = cameraPadding
    ).let {
        CameraBounds(
            minX = it.x,
            minY = it.y,
            maxX = it.x + it.width,
            maxY = it.y + it.height
        )
    }

    private val viewport = CameraViewport(
        camera = camera,
        mode = viewportMode,
        virtualHeight = virtualHeight,
        bounds = cameraBounds
    )

    val cameraController = CameraController(
        camera = camera,
        bounds = cameraBounds,
        zoomMode = zoomMode,
        worldZoomBounds = worldBounds,
        worldFill = worldFill,
        zoomAnchor = zoomAnchor
    )

    private val tilePicker = TilePicker(
        camera = camera,
        projection = projection,
        world = world
    )

    val inputProcessor = InputMultiplexer(
        TileInputProcessor(
            tilePicker = tilePicker,
            onLeftClick = onLeftClick,
            onRightClick = onRightClick
        ),
        cameraController.inputProcessor
    )

    init {
        viewport.resize(
            Gdx.graphics.width,
            Gdx.graphics.height
        )

        if (zoomMode == ZoomMode.WORLD_BASED) {
            cameraController.fitWorld()
        } else {
            camera.position.set(
                worldBounds.x + worldBounds.width / 2f,
                worldBounds.y + worldBounds.height / 2f,
                0f
            )

            cameraController.refreshZoomBounds()
        }
    }

    fun update(delta: Float) {
        cameraController.update(delta)
    }

    /**
     * Renders the world using tile textures provided by the game.
     */
    fun render(
        textureFor: (Tile) -> TextureRegion?,
        raisedTile: Pair<Int, Int>? = null,
        raiseOffsetY: Float = 0f
    ) {
        tileRenderer.render(
            world = world,
            camera = camera,
            textureFor = textureFor,
            raisedTile = raisedTile,
            raiseOffsetY = raiseOffsetY
        )
    }

    fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        viewport.resize(width, height)
        cameraController.refreshZoomBounds()
    }

    /**
     * Returns the tile at the given screen position, or null.
     */
    fun pickTile(screenX: Float, screenY: Float): Pair<Int, Int>? {
        return tilePicker.pick(screenX, screenY)
    }

    /**
     * Releases resources owned by this world view.
     */
    fun dispose() {
        tileRenderer.dispose()
    }
}