package com.mefabc24.strata.iso

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.world.TilePosition
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
     * Full logical terrain height, independent of texture dimensions.
     */
    val logicalTileHeight: Float
        get() = geometry.height

    /**
     * Vertical world-space distance between two terrain levels.
     */
    val elevationStep: Float
        get() = geometry.elevationStep

    /**
     * Projects a tile to the back vertex of its logical top face.
     */
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

    /**
     * Returns the front vertex of a tile's logical top face.
     *
     * This is the ground-contact anchor used by placed objects.
     */
    internal fun surfaceAnchor(
        x: Int,
        y: Int,
        elevation: Int = 0
    ): Vector2 {
        return tileToWorld(x, y, elevation).add(0f, -tileHeight)
    }

    fun worldToTile(
        worldX: Float,
        worldY: Float
    ): TilePosition {
        val x = worldX / tileWidth - worldY / tileHeight
        val y = -worldX / tileWidth - worldY / tileHeight

        return TilePosition(
            x = floor(x).toInt(),
            y = floor(y).toInt()
        )
    }

    fun worldBounds(
        width: Int,
        height: Int,
        padding: Float = 0f,
        maxElevation: Int = 0,
        maxSpriteHeight: Float = tileHeight
    ): Rectangle {
        require(width > 0 && height > 0)
        require(padding >= 0f && padding.isFinite())
        require(maxElevation >= 0)
        require(maxSpriteHeight > 0f && maxSpriteHeight.isFinite())

        val halfWidth = tileWidth / 2f

        val minX = -(height - 1) * halfWidth - halfWidth
        val maxX = (width - 1) * halfWidth + halfWidth

        val highestTopY = maxElevation * elevationStep

        val deepestTopY =
            -(width + height - 2) * tileHeight / 2f

        val visualOverhang = maxOf(
            0f,
            maxSpriteHeight - logicalTileHeight
        )

        val minY = deepestTopY - logicalTileHeight
        val maxY = highestTopY + visualOverhang

        return Rectangle(
            minX - padding,
            minY - padding,
            maxX - minX + padding * 2f,
            maxY - minY + padding * 2f
        )
    }
}
