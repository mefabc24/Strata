package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.InputAdapter

class CameraController(
    private val camera: OrthographicCamera,
    var moveSpeed: Float = 500f,
    var zoomSpeed: Float = 0.1f,
    var minZoom: Float = 0.25f,
    var maxZoom: Float = 3f
) {
    val inputProcessor = object : InputAdapter() {

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

        camera.update()
    }

    fun zoom(amount: Float) {
        camera.zoom = MathUtils.clamp(
            camera.zoom + amount * zoomSpeed,
            minZoom,
            maxZoom
        )

        camera.update()
    }
}