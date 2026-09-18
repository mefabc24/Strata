package com.mefabc24.strata.iso

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.camera.CameraBounds
import com.mefabc24.strata.camera.CameraController
import com.mefabc24.strata.camera.CameraViewport
import com.mefabc24.strata.camera.ViewportMode
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.world.World

/**
 * Manages the camera and viewport for an isometric world.
 */
class IsoWorldView(
    world: World,
    projection: IsoProjection,
    viewportMode: ViewportMode = ViewportMode.FIXED_HEIGHT,
    virtualHeight: Float = 720f,
    zoomMode: ZoomMode = ZoomMode.WORLD_BASED,
    cameraPadding: Float = 100f,
    worldFill: Float = 0.85f
) {
    val camera = OrthographicCamera()

    private val worldBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = 0f
    )

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
        worldFill = worldFill
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

    fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        viewport.resize(width, height)
        cameraController.refreshZoomBounds()
    }
}