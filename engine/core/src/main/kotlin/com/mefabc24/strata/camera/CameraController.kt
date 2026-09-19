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
    private val zoomBounds: CameraBounds? = bounds,
    var moveSpeed: Float = 500f,
    var zoomSpeed: Float = 0.1f,
    var minZoom: Float = 0.25f,
    var maxZoom: Float = 3f,
    private val zoomMode: ZoomMode = ZoomMode.FIXED,
    private val worldZoomBounds: Rectangle? = null,
    private val worldFill: Float = 0.85f,
    private val zoomAnchor: ZoomAnchor = ZoomAnchor.CENTER,
    val controls: CameraControls = CameraControls()
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

    /**
     * Controls whether the camera responds to user input.
     */
    var enabled: Boolean = true
        set(value) {
            field = value

            if (!value) {
                isDragging = false
                dragPointer = -1
                activeDragButton = -1
            }
        }

    private var isDragging = false
    private var dragPointer = -1
    private var activeDragButton = -1

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
            if (!enabled) return false

            if (
                !controls.mouseDraggingEnabled ||
                button != controls.dragButton ||
                isDragging
            ) {
                return false
            }

            isDragging = true
            dragPointer = pointer
            activeDragButton = button

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
            if (!enabled || !controls.mouseDraggingEnabled) {
                isDragging = false
                dragPointer = -1
                activeDragButton = -1

                return false
            }

            if (!isDragging || pointer != dragPointer) {
                return false
            }

            currentMousePosition.set(
                screenX.toFloat(),
                screenY.toFloat(),
                0f
            )

            camera.unproject(currentMousePosition)

            pan(
                lastMousePosition.x - currentMousePosition.x,
                lastMousePosition.y - currentMousePosition.y
            )

            return true
        }

        override fun touchUp(
            screenX: Int,
            screenY: Int,
            pointer: Int,
            button: Int
        ): Boolean {
            if (!enabled) return false

            if (
                !isDragging ||
                pointer != dragPointer ||
                button != activeDragButton
            ) {
                return false
            }

            isDragging = false
            dragPointer = -1
            activeDragButton = -1

            return true
        }

        override fun scrolled(
            amountX: Float,
            amountY: Float
        ): Boolean {
            if (!enabled || !controls.zoomEnabled) return false

            zoom(amountY)
            return true
        }
    }

    fun update(delta: Float) {
        if (!controls.mouseDraggingEnabled && isDragging) {
            isDragging = false
            dragPointer = -1
            activeDragButton = -1
        }

        if (!enabled || !controls.keyboardMovementEnabled) return

        val movement = moveSpeed * delta * camera.zoom

        var deltaX = 0f
        var deltaY = 0f

        if (Gdx.input.isKeyPressed(controls.moveUp)) {
            deltaY += movement
        }

        if (Gdx.input.isKeyPressed(controls.moveDown)) {
            deltaY -= movement
        }

        if (Gdx.input.isKeyPressed(controls.moveLeft)) {
            deltaX -= movement
        }

        if (Gdx.input.isKeyPressed(controls.moveRight)) {
            deltaX += movement
        }

        if (deltaX != 0f || deltaY != 0f) {
            pan(deltaX, deltaY)
        }
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

        if (camera.zoom >= effectiveMaxZoom()) {
            if (zoomMode == ZoomMode.WORLD_BASED) {
                centerWorld()
            } else {
                applyZoomBounds()
            }
        } else {
            applyZoomBounds()
        }
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

        if (
            zoomMode == ZoomMode.WORLD_BASED &&
            camera.zoom >= effectiveMaxZoom()
        ) {
            centerWorld()
        } else {
            applyZoomBounds()
        }
    }

    /**
     * Centers the camera on the world and adjusts the zoom to fit it.
     */
    fun fitWorld() {
        require(zoomMode == ZoomMode.WORLD_BASED) {
            "fitWorld() requires WORLD_BASED zoom mode."
        }

        camera.zoom = effectiveMaxZoom()
        centerWorld()
    }

    private fun centerWorld() {
        val world = requireNotNull(worldZoomBounds)

        camera.position.set(
            world.x + world.width / 2f,
            world.y + world.height / 2f,
            0f
        )

        camera.update()
    }

    private fun pan(deltaX: Float, deltaY: Float) {
        if (bounds != null) {
            bounds.move(camera, deltaX, deltaY)
        } else {
            camera.position.add(deltaX, deltaY, 0f)
        }

        camera.update()
    }

    private fun applyZoomBounds() {
        zoomBounds?.clamp(camera)
        camera.update()
    }
}