package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils

class CameraBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    init {
        require(minX <= maxX)
        require(minY <= maxY)
    }

    fun clamp(camera: OrthographicCamera) {
        val halfWidth = camera.viewportWidth * camera.zoom / 2f
        val halfHeight = camera.viewportHeight * camera.zoom / 2f

        camera.position.x = clampAxis(
            camera.position.x,
            minX,
            maxX,
            halfWidth
        )

        camera.position.y = clampAxis(
            camera.position.y,
            minY,
            maxY,
            halfHeight
        )
    }

    private fun clampAxis(
        position: Float,
        min: Float,
        max: Float,
        halfViewport: Float
    ): Float {
        val center = (min + max) / 2f

        if (max - min <= halfViewport * 2f) {
            return center
        }

        return MathUtils.clamp(
            position,
            min + halfViewport,
            max - halfViewport
        )
    }
}