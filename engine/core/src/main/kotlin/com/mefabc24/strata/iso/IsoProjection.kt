package com.mefabc24.strata.iso

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.world.World
import kotlin.math.floor

class IsoProjection(
    val tileWidth: Float = 64f,
    val tileHeight: Float = 32f,
) {
    init {
        require(tileWidth > 0f && tileWidth.isFinite())
        require(tileHeight > 0f && tileHeight.isFinite())
    }

    /**
     * Vertical world-space distance between two terrain levels.
     */
    val elevationStep: Float
        get() = tileHeight / 2f

    fun tileToWorld(
        x: Int,
        y: Int,
        elevation: Int = 0
    ): Vector2 {
        return Vector2(
            (x - y) * tileWidth / 2f,
            -(x + y) * tileHeight / 2f + elevation * elevationStep
        )
    }

    fun worldToTile(worldX: Float, worldY: Float): Pair<Int, Int> {
        val x = worldX / tileWidth - worldY / tileHeight
        val y = -worldX / tileWidth - worldY / tileHeight

        return floor(x).toInt() to floor(y).toInt()
    }

    fun worldBounds(
        width: Int,
        height: Int,
        padding: Float = 0f
    ): Rectangle {
        require(width > 0 && height > 0)
        require(padding >= 0f && padding.isFinite())

        val halfWidth = tileWidth / 2f

        val minX = -(height - 1) * halfWidth - halfWidth
        val maxX = (width - 1) * halfWidth + halfWidth

        val maxY = 0f
        val minY = -(width + height) * tileHeight / 2f

        return Rectangle(
            minX - padding,
            minY - padding,
            maxX - minX + padding * 2f,
            maxY - minY + padding * 2f
        )
    }
}