
package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils

class CameraBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    private val edgeAllowance: Float = 0f
) {
    init {
        require(minX <= maxX)
        require(minY <= maxY)
        require(edgeAllowance in 0f..1f)
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

    /**
     * Applies camera movement without snapping an existing bounds
     * violation back into the allowed area.
     */
    fun move(
        camera: OrthographicCamera,
        deltaX: Float,
        deltaY: Float
    ) {
        val halfWidth = camera.viewportWidth * camera.zoom / 2f
        val halfHeight = camera.viewportHeight * camera.zoom / 2f

        camera.position.x = moveAxis(
            camera.position.x,
            deltaX,
            minX,
            maxX,
            halfWidth
        )

        camera.position.y = moveAxis(
            camera.position.y,
            deltaY,
            minY,
            maxY,
            halfHeight
        )
    }

    private fun moveAxis(
        position: Float,
        delta: Float,
        min: Float,
        max: Float,
        halfViewport: Float
    ): Float {
        val effectiveHalfViewport = halfViewport * (1f - edgeAllowance)

        // No panning if viewport covers entire allowed area
        if (max - min <= effectiveHalfViewport * 2f) {
            return position
        }

        val lowerBound = min + effectiveHalfViewport
        val upperBound = max - effectiveHalfViewport

        return MathUtils.clamp(
            position + delta,
            minOf(position, lowerBound),
            maxOf(position, upperBound)
        )
    }

    private fun clampAxis(
        position: Float,
        min: Float,
        max: Float,
        halfViewport: Float
    ): Float {
        val center = (min + max) / 2f

        val effectiveHalfViewport = halfViewport * (1f - edgeAllowance)

        if (max - min <= effectiveHalfViewport * 2f) {
            return center
        }

        return MathUtils.clamp(
            position,
            min + effectiveHalfViewport,
            max - effectiveHalfViewport
        )
    }
}