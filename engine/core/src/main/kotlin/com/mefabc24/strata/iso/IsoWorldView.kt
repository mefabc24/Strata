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
import com.mefabc24.strata.render.IsoWorldRenderer
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.render.ObjectVisual
import com.mefabc24.strata.render.PlacementPreview
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.World

/**
 * Determines how placed objects are picked.
 */
enum class ObjectPickingMode {

    /**
     * Picks objects only through their occupied ground tiles.
     */
    FOOTPRINT,

    /**
     * Picks objects through their visible sprite pixels.
     */
    SPRITE_ALPHA,

    /**
     * Disables object picking.
     */
    NONE
}

/**
 * Manages the camera and viewport for an isometric world.
 */
class IsoWorldView(
    private val world: World,
    private val textureFor: (Tile) -> TextureRegion?,
    private val objectVisualFor: (PlacedObject) -> ObjectVisual? = { null },
    tileWidth: Float = 64f,
    tileHeight: Float = 32f,
    viewportMode: ViewportMode = ViewportMode.FIXED_HEIGHT,
    virtualHeight: Float = 720f,
    zoomMode: ZoomMode = ZoomMode.WORLD_BASED,
    zoomAnchor: ZoomAnchor = ZoomAnchor.CURSOR,
    cameraPadding: Float = 100f,
    zoomEdgeAllowance: Float = 0f,
    worldFill: Float = 0.85f,
    onLeftClick: ((x: Int, y: Int) -> Boolean)? = null,
    onRightClick: ((x: Int, y: Int) -> Boolean)? = null
) {
    val camera = OrthographicCamera()

    private val projection = IsoProjection(
        tileWidth = tileWidth,
        tileHeight = tileHeight
    )

    var hoveredTile: Pair<Int, Int>? = null
        private set

    private val worldBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = 0f
    )

    private val worldRenderer = IsoWorldRenderer(projection)

    /**
     * Rendering statistics from the most recent frame.
     */
    val renderStats: RenderStats
        get() = worldRenderer.stats

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

    private val zoomBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = cameraPadding
    ).let {
        CameraBounds(
            minX = it.x,
            minY = it.y,
            maxX = it.x + it.width,
            maxY = it.y + it.height,
            edgeAllowance = zoomEdgeAllowance
        )
    }

    private val viewport = CameraViewport(
        camera = camera,
        mode = viewportMode,
        virtualHeight = virtualHeight,
        bounds = zoomBounds
    )

    val cameraController = CameraController(
        camera = camera,
        bounds = cameraBounds,
        zoomBounds = zoomBounds,
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

    private val objectPicker = ObjectPicker(
        camera = camera,
        projection = projection,
        world = world,
        visualFor = objectVisualFor
    )

    private val tileInputProcessor = TileInputProcessor(
        tilePicker = tilePicker,
        onLeftClick = onLeftClick,
        onRightClick = onRightClick
    )

    /**
     * Controls whether mouse clicks interact with world tiles.
     */
    var worldClicksEnabled: Boolean
        get() = tileInputProcessor.enabled
        set(value) {
            tileInputProcessor.enabled = value
        }

    val inputProcessor = InputMultiplexer(
        tileInputProcessor,
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

        hoveredTile = tilePicker.pick(
            Gdx.input.x.toFloat(),
            Gdx.input.y.toFloat()
        )
    }

    /**
     * Renders terrain and world objects.
     */
    /**
     * Renders terrain, world objects, and an optional placement preview.
     */
    fun render(
        raisedTile: Pair<Int, Int>? = null,
        raiseOffsetY: Float = 0f,
        preview: PlacementPreview? = null
    ) {
        worldRenderer.render(
            world = world,
            camera = camera,
            textureFor = textureFor,
            raisedTile = raisedTile,
            raiseOffsetY = raiseOffsetY,
            objectVisualFor = objectVisualFor,
            preview = preview
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
     * Returns an object according to the selected picking mode.
     */
    fun pickObject(
        screenX: Float,
        screenY: Float,
        mode: ObjectPickingMode = ObjectPickingMode.SPRITE_ALPHA
    ): PlacedObject? {
        return when (mode) {
            ObjectPickingMode.FOOTPRINT -> {
                val tile = tilePicker.pick(screenX, screenY)

                tile?.let { (x, y) ->
                    world.getObjectAt(x, y)
                }
            }

            ObjectPickingMode.SPRITE_ALPHA -> {
                objectPicker.pick(screenX, screenY)
            }

            ObjectPickingMode.NONE -> null
        }
    }

    /**
     * Releases resources owned by this world view.
     */
    fun dispose() {
        worldRenderer.dispose()
    }
}