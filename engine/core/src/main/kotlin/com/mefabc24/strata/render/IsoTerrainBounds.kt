package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection

/**
 * Calculates terrain sprite bounds from logical tile geometry.
 *
 * The logical terrain rectangle is bottom-aligned with the sprite. Texture
 * pixels above that rectangle extend above the logical tile without moving
 * its top face or object ground-contact anchor.
 */
internal object IsoTerrainBounds {

    fun calculate(
        projection: IsoProjection,
        x: Int,
        y: Int,
        texture: TextureRegion,
        result: Rectangle,
        offsetY: Float = 0f,
        elevation: Int = 0
    ): Rectangle {
        return calculate(
            projection = projection,
            x = x,
            y = y,
            textureWidth = texture.regionWidth,
            textureHeight = texture.regionHeight,
            result = result,
            offsetY = offsetY,
            elevation = elevation
        )
    }

    internal fun calculate(
        projection: IsoProjection,
        x: Int,
        y: Int,
        textureWidth: Int,
        textureHeight: Int,
        result: Rectangle,
        offsetY: Float = 0f,
        elevation: Int = 0
    ): Rectangle {
        require(textureWidth > 0 && textureHeight > 0) {
            "Terrain texture dimensions must be positive."
        }

        val scale = projection.tileWidth / textureWidth

        val spriteWidth = textureWidth * scale
        val spriteHeight = textureHeight * scale
        val tileTop = projection.tileToWorld(x, y, elevation)

        return result.set(
            tileTop.x - spriteWidth / 2f,
            tileTop.y - projection.logicalTileHeight + offsetY,
            spriteWidth,
            spriteHeight
        )
    }
}
