package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector3

class CameraController(
    private val camera: OrthographicCamera,
    private val bounds: CameraBounds? = null,
    var moveSpeed: Float = 500f,
    var zoomSpeed: Float = 0.1f,
    var minZoom: Float = 0.25f,
    var maxZoom: Float = 3f
) {
    private var isDragging = false
    private var dragPointer = -1

    private val lastMousePosition = Vector3()
    private val currentMousePosition = Vector3()


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

    fun zoom(amount: Float) {
        camera.zoom = MathUtils.clamp(
            camera.zoom + amount * zoomSpeed,
            minZoom,
            maxZoom
        )

        applyBounds()
    }

    private fun applyBounds() {
        bounds?.clamp(camera)
        camera.update()
    }
}