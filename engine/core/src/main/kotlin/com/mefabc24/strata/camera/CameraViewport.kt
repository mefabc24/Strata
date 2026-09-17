package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera

/**
 * Defines how the camera's visible world area responds to window resizing.
 */
enum class ViewportMode {

    /**
     * Keeps the virtual viewport height constant.
     *
     * The viewport width adapts to the window's aspect ratio.
     * Resizing a window without changing its aspect ratio preserves
     * the relative size of the world on screen.
     */
    FIXED_HEIGHT,

    /**
     * Uses the actual window dimensions as the camera's viewport size.
     *
     * A larger window displays more of the world while objects retain
     * approximately the same size in screen pixels at a constant zoom.
     */
    PIXEL_BASED
}

class CameraViewport(
    private val camera: OrthographicCamera,
    private val mode: ViewportMode = ViewportMode.FIXED_HEIGHT,
    private val virtualHeight: Float = 720f,
    private val bounds: CameraBounds? = null
) {
    init {
        require(virtualHeight > 0f && virtualHeight.isFinite())
    }

    fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        val currentZoom = camera.zoom

        val viewportHeight = when (mode) {
            ViewportMode.FIXED_HEIGHT -> virtualHeight
            ViewportMode.PIXEL_BASED -> height.toFloat()
        }

        val viewportWidth = when (mode) {
            ViewportMode.FIXED_HEIGHT ->
                virtualHeight * width / height.toFloat()

            ViewportMode.PIXEL_BASED ->
                width.toFloat()
        }

        camera.setToOrtho(
            false,
            viewportWidth,
            viewportHeight
        )

        camera.zoom = currentZoom

        bounds?.clamp(camera)
        camera.update()
    }
}