package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3

/**
 * Defines which point remains anchored while zooming.
 */
enum class ZoomAnchor {
    /**
     * Keeps the camera center fixed.
     */
    CENTER,

    /**
     * Keeps the world point under the mouse cursor fixed.
     */
    CURSOR
}

/**
 * Defines how the maximum zoom-out level is determined.
 */
enum class ZoomMode {
    /**
     * Uses a fixed maximum zoom value.
     */
    FIXED,

    /**
     * Calculates the maximum zoom from the world dimensions
     * and the current camera viewport.
     */
    WORLD_BASED
}

class CameraController(
    private val camera: OrthographicCamera,
    private val bounds: CameraBounds? = null,
    var moveSpeed: Float = 500f,
    var zoomSpeed: Float = 0.1f,
    var minZoom: Float = 0.25f,
    var maxZoom: Float = 3f,
    private val zoomMode: ZoomMode = ZoomMode.FIXED,
    private val worldZoomBounds: Rectangle? = null,
    private val worldFill: Float = 0.85f,
    private val zoomAnchor: ZoomAnchor = ZoomAnchor.CENTER
) {
    init {
        require(minZoom > 0f && minZoom.isFinite())
        require(maxZoom >= minZoom && maxZoom.isFinite())
        require(worldFill > 0f && worldFill <= 1f)

        if (zoomMode == ZoomMode.WORLD_BASED) {
            require(worldZoomBounds != null)
            require(worldZoomBounds.width > 0f && worldZoomBounds.height > 0f)
        }
    }

    private var isDragging = false
    private var dragPointer = -1

    private val lastMousePosition = Vector3()
    private val currentMousePosition = Vector3()

    private val zoomMouseBefore = Vector3()
    private val zoomMouseAfter = Vector3()

    val inputProcessor = object : InputAdapter() {
        override fun touchDown(
            screenX: Int,
            screenY: Int,
            pointer: Int,
            button: Int
        ): Boolean {
            if (button != Input.Buttons.MIDDLE || isDragging) {
                return false
            }

            isDragging = true
            dragPointer = pointer

            lastMousePosition.set(
                screenX.toFloat(),
                screenY.toFloat(),
                0f
            )

            camera.unproject(lastMousePosition)

            return true
        }

        override fun touchDragged(
            screenX: Int,
            screenY: Int,
            pointer: Int
        ): Boolean {
            if (!isDragging || pointer != dragPointer) {
                return false
            }

            currentMousePosition.set(
                screenX.toFloat(),
                screenY.toFloat(),
                0f
            )

            camera.unproject(currentMousePosition)

            camera.position.x +=
                lastMousePosition.x - currentMousePosition.x

            camera.position.y +=
                lastMousePosition.y - currentMousePosition.y

            applyBounds()

            return true
        }

        override fun touchUp(
            screenX: Int,
            screenY: Int,
            pointer: Int,
            button: Int
        ): Boolean {
            if (!isDragging ||
                pointer != dragPointer ||
                button != Input.Buttons.MIDDLE
            ) {
                return false
            }

            isDragging = false
            dragPointer = -1

            return true
        }

        override fun scrolled(
            amountX: Float,
            amountY: Float
        ): Boolean {
            zoom(amountY)
            return true
        }
    }

    fun update(delta: Float) {
        val movement = moveSpeed * delta * camera.zoom

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.position.y += movement
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.position.y -= movement
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.position.x -= movement
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.position.x += movement
        }

        applyBounds()
    }

    private fun effectiveMaxZoom(): Float {
        if (zoomMode == ZoomMode.FIXED) {
            return maxZoom
        }

        val world = requireNotNull(worldZoomBounds)

        val zoomX = world.width / camera.viewportWidth
        val zoomY = world.height / camera.viewportHeight

        val fitZoom = maxOf(zoomX, zoomY) / worldFill

        return maxOf(minZoom, fitZoom)
    }

    fun zoom(amount: Float) {
        if (zoomAnchor == ZoomAnchor.CURSOR) {
            zoomMouseBefore.set(
                Gdx.input.x.toFloat(),
                Gdx.input.y.toFloat(),
                0f
            )

            camera.unproject(zoomMouseBefore)
        }

        camera.zoom = MathUtils.clamp(
            camera.zoom + amount * zoomSpeed,
            minZoom,
            effectiveMaxZoom()
        )

        camera.update()

        if (zoomAnchor == ZoomAnchor.CURSOR) {
            zoomMouseAfter.set(
                Gdx.input.x.toFloat(),
                Gdx.input.y.toFloat(),
                0f
            )

            camera.unproject(zoomMouseAfter)

            camera.position.x += zoomMouseBefore.x - zoomMouseAfter.x
            camera.position.y += zoomMouseBefore.y - zoomMouseAfter.y
        }

        applyBounds()
    }

    /**
     * Reapplies the zoom limits after the camera viewport changes.
     */
    fun refreshZoomBounds() {
        camera.zoom = MathUtils.clamp(
            camera.zoom,
            minZoom,
            effectiveMaxZoom()
        )

        applyBounds()
    }

    /**
     * Centers the camera on the world and adjusts the zoom to fit it.
     */
    fun fitWorld() {
        require(zoomMode == ZoomMode.WORLD_BASED) {
            "fitWorld() requires WORLD_BASED zoom mode."
        }

        val world = requireNotNull(worldZoomBounds)

        camera.zoom = effectiveMaxZoom()

        camera.position.set(
            world.x + world.width / 2f,
            world.y + world.height / 2f,
            0f
        )

        applyBounds()
    }

    private fun applyBounds() {
        bounds?.clamp(camera)
        camera.update()
    }
}