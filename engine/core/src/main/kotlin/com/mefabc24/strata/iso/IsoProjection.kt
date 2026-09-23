package com.mefabc24.strata.iso

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import kotlin.math.floor

class IsoProjection(
    geometry: TileGeometry = TileGeometry()
) {

    private val geometry = geometry.copy().also {
        it.validate()
    }

    /**
     * Width of the isometric top face.
     */
    val tileWidth: Float
        get() = geometry.width

    /**
     * Height of the isometric top face.
     */
    val tileHeight: Float
        get() = geometry.faceHeight

    /**
     * Vertical world-space distance between two terrain levels.
     */
    val elevationStep: Float
        get() = geometry.elevationStep

    fun tileToWorld(
        x: Int,
        y: Int,
        elevation: Int = 0
    ): Vector2 {
        return Vector2(
            (x - y) * tileWidth / 2f,
            -(x + y) * tileHeight / 2f +
                    elevation * elevationStep
        )
    }

    fun worldToTile(
        worldX: Float,
        worldY: Float
    ): Pair<Int, Int> {
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