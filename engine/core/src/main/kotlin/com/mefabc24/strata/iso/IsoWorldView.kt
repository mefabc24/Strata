package com.mefabc24.strata.iso

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.camera.CameraBounds
import com.mefabc24.strata.camera.CameraController
import com.mefabc24.strata.camera.CameraViewport
import com.badlogic.gdx.InputMultiplexer
import com.mefabc24.strata.input.WorldInputProcessor
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.render.IsoWorldRenderer
import com.mefabc24.strata.render.IsoGridRenderer
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.render.ObjectVisual
import com.mefabc24.strata.render.PlacementPreview
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.World
import com.mefabc24.strata.camera.CameraSettings
import com.mefabc24.strata.input.ControlsSettings
import com.mefabc24.strata.render.RenderingSettings
import com.mefabc24.strata.scene.DebugGridSettings
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.scene.DebugGridRenderLayer

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
     * Picks sprite pixels first, then falls back to occupied ground tiles.
     */
    SPRITE_OR_FOOTPRINT,

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

    cameraSettings: CameraSettings = CameraSettings(),
    controls: ControlsSettings = ControlsSettings(),
    renderingSettings: RenderingSettings = RenderingSettings(),
    debugGridSettings: DebugGridSettings = DebugGridSettings()
) {

    private val cameraConfig = cameraSettings.copy().also {
        it.validate()
    }

    private val renderingConfig = renderingSettings.copy().also {
        it.validate()
    }

    private val maxTerrainSpriteHeight =
        renderingConfig.maxTerrainSpriteHeight ?: Float.POSITIVE_INFINITY

    private val boundsSpriteHeight =
        if (maxTerrainSpriteHeight.isFinite()) {
            maxTerrainSpriteHeight
        } else {
            renderingConfig.tileGeometry.height
        }

    val camera = OrthographicCamera()

    private val projection = IsoProjection(
        geometry = renderingConfig.tileGeometry
    )

    var hoveredTile: TilePosition? = null
        private set

    private val worldBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = 0f,
        maxSpriteHeight = boundsSpriteHeight
    )

    private val worldRenderer = IsoWorldRenderer(
        projection = projection,
        objectSettings = renderingConfig.objects
    )

    private val debugGridConfig = debugGridSettings

    private val gridRenderer = IsoGridRenderer(
        projection = projection,
        settings = debugGridConfig
    )

    /**
     * Rendering statistics from the most recent frame.
     */
    val renderStats: RenderStats
        get() = worldRenderer.stats

    private val cameraBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = cameraConfig.cameraPadding,
        maxSpriteHeight = boundsSpriteHeight
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
        padding = cameraConfig.cameraPadding,
        maxSpriteHeight = boundsSpriteHeight
    ).let {
        CameraBounds(
            minX = it.x,
            minY = it.y,
            maxX = it.x + it.width,
            maxY = it.y + it.height,
            edgeAllowance = cameraConfig.zoomEdgeAllowance
        )
    }

    private val viewport = CameraViewport(
        camera = camera,
        mode = cameraConfig.viewportMode,
        virtualHeight = cameraConfig.virtualHeight,
        bounds = zoomBounds
    )

    val cameraController = CameraController(
        camera = camera,
        settings = cameraConfig,
        bounds = cameraBounds,
        zoomBounds = zoomBounds,
        worldZoomBounds = worldBounds,
        controls = controls.camera
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
        visualFor = objectVisualFor,
        objectSettings = renderingConfig.objects
    )

    private val worldInputProcessor = WorldInputProcessor(
        bindings = controls.gameplay.bindings,
        pickTile = { screenX, screenY ->
            tilePicker.pick(screenX, screenY)
        },
        pickObject = { screenX, screenY, mode ->
            pickObject(screenX, screenY, mode)
        }
    )

    /**
     * Controls whether world input bindings are processed.
     */
    var worldInputEnabled: Boolean
        get() = worldInputProcessor.enabled
        set(value) {
            worldInputProcessor.enabled = value
        }

    val inputProcessor = InputMultiplexer(
        worldInputProcessor,
        cameraController.inputProcessor
    )

    init {
        viewport.resize(
            Gdx.graphics.width,
            Gdx.graphics.height
        )

        if (cameraConfig.zoomMode == ZoomMode.WORLD_BASED) {
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

    /**
     * Updates the state of the world view.
     */
    fun update(delta: Float) {
        cameraController.update(delta)

        hoveredTile = tilePicker.pick(
            Gdx.input.x.toFloat(),
            Gdx.input.y.toFloat()
        )
    }

    /**
     * Renders terrain, world objects, and an optional placement preview.
     */
    fun render(preview: PlacementPreview? = null) {
        worldRenderer.render(
            world = world,
            camera = camera,
            textureFor = textureFor,
            objectVisualFor = objectVisualFor,
            preview = preview,
            maxTerrainSpriteHeight = maxTerrainSpriteHeight
        )

        if (debugGridConfig.enabled) {
            gridRenderer.render(
                world = world,
                camera = camera,
                hoveredTile = hoveredTile
            )

            if (
                debugGridConfig.renderLayer ==
                DebugGridRenderLayer.BELOW_OBJECTS
            ) {
                worldRenderer.renderObjectsOverlay(
                    camera = camera,
                    objectVisualFor = objectVisualFor,
                    preview = preview
                )
            }
        }
    }

    fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        viewport.resize(width, height)
        cameraController.refreshZoomBounds()
    }

    /** Returns the tile at the given screen position, or null. */
    fun pickTile(screenX: Float, screenY: Float): TilePosition? {
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

            ObjectPickingMode.SPRITE_OR_FOOTPRINT -> {
                objectPicker.pick(screenX, screenY)
                    ?: tilePicker.pick(screenX, screenY)?.let { (x, y) ->
                        world.getObjectAt(x, y)
                    }
            }

            ObjectPickingMode.NONE -> null
        }
    }

    /**
     * Releases resources owned by this world view.
     */
    fun dispose() {
        try {
            gridRenderer.dispose()
        } finally {
            worldRenderer.dispose()
        }
    }
}
