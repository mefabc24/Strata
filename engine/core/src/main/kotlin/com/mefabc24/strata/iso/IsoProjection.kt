package com.mefabc24.strata.iso

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.world.TilePosition
import kotlin.math.abs
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
     * Projects a tile to the back vertex of its logical top face.
     */
    fun tileToWorld(x: Int, y: Int): Vector2 {
        return Vector2(
            (x - y) * tileWidth / 2f,
            -(x + y) * tileHeight / 2f
        )
    }

    /**
     * Projects a continuous logical tile-space position onto the ground plane.
     * Integer coordinates are tile boundaries and half coordinates are tile
     * centers.
     */
    fun tileToWorld(x: Float, y: Float): Vector2 {
        return Vector2(
            (x - y) * tileWidth / 2f,
            -(x + y) * tileHeight / 2f
        )
    }

    /**
     * Returns the front vertex of a tile's logical top face.
     *
     * This is the ground-contact anchor used by placed objects.
     */
    internal fun surfaceAnchor(x: Int, y: Int): Vector2 {
        return tileToWorld(x, y).add(0f, -tileHeight)
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

    /**
     * Returns whether a world position lies on a tile's logical top face.
     *
     * The interaction surface uses the isometric face dimensions only. The
     * terrain's full logical height and texture dimensions do not enlarge it.
     * Shared edges are included; [worldToTile] provides the deterministic
     * candidate when two adjacent faces share that edge.
     */
    fun containsTopFace(
        worldX: Float,
        worldY: Float,
        x: Int,
        y: Int
    ): Boolean {
        val top = tileToWorld(x, y)
        val centerX = top.x
        val centerY = top.y - tileHeight / 2f

        val normalizedDistance =
            abs(worldX - centerX) / (tileWidth / 2f) +
                abs(worldY - centerY) / (tileHeight / 2f)

        return normalizedDistance <= 1f + TOP_FACE_EPSILON
    }

    fun worldBounds(
        width: Int,
        height: Int,
        padding: Float = 0f,
        maxSpriteHeight: Float = tileHeight
    ): Rectangle {
        require(width > 0 && height > 0)
        require(padding >= 0f && padding.isFinite())
        require(maxSpriteHeight > 0f && maxSpriteHeight.isFinite())

        val halfWidth = tileWidth / 2f

        val minX = -(height - 1) * halfWidth - halfWidth
        val maxX = (width - 1) * halfWidth + halfWidth

        val deepestTopY =
            -(width + height - 2) * tileHeight / 2f

        val visualOverhang = maxOf(
            0f,
            maxSpriteHeight - logicalTileHeight
        )

        val minY = deepestTopY - logicalTileHeight
        val maxY = visualOverhang

        return Rectangle(
            minX - padding,
            minY - padding,
            maxX - minX + padding * 2f,
            maxY - minY + padding * 2f
        )
    }

    private companion object {
        const val TOP_FACE_EPSILON = 0.0001f
    }

}
