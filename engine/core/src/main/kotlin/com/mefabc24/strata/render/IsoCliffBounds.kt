package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection

/** Calculates a one-step cliff sprite's bounds from logical tile geometry. */
internal object IsoCliffBounds {

    fun calculate(
        projection: IsoProjection,
        x: Int,
        y: Int,
        elevation: Int,
        levelBelowSurface: Int,
        texture: TextureRegion,
        result: Rectangle,
        offsetY: Float = 0f
    ): Rectangle {
        return calculate(
            projection = projection,
            x = x,
            y = y,
            elevation = elevation,
            levelBelowSurface = levelBelowSurface,
            textureWidth = texture.regionWidth,
            textureHeight = texture.regionHeight,
            result = result,
            offsetY = offsetY
        )
    }

    internal fun calculate(
        projection: IsoProjection,
        x: Int,
        y: Int,
        elevation: Int,
        levelBelowSurface: Int,
        textureWidth: Int,
        textureHeight: Int,
        result: Rectangle,
        offsetY: Float = 0f
    ): Rectangle {
        require(levelBelowSurface > 0) {
            "Cliff level below surface must be positive."
        }
        require(textureWidth > 0 && textureHeight > 0) {
            "Cliff texture dimensions must be positive."
        }

        val scale = projection.tileWidth / textureWidth
        val spriteWidth = textureWidth * scale
        val spriteHeight = textureHeight * scale
        val tileTop = projection.tileToWorld(x, y, elevation)
        val surfaceSpriteBottom = tileTop.y - projection.logicalTileHeight

        return result.set(
            tileTop.x - spriteWidth / 2f,
            surfaceSpriteBottom -
                    levelBelowSurface * projection.elevationStep + offsetY,
            spriteWidth,
            spriteHeight
        )
    }
}
