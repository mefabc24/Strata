package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.iso.IsoProjection

/**
 * Draws individual terrain tiles at their isometric positions.
 *
 * The caller owns the SpriteBatch and determines the draw order.
 */
class IsoTerrainRenderer(
    private val projection: IsoProjection
) {
    fun render(
        batch: SpriteBatch,
        x: Int,
        y: Int,
        texture: TextureRegion,
        offsetY: Float = 0f,
        elevation: Int = 0
    ) {
        val scale = projection.tileWidth / texture.regionWidth

        val spriteWidth = texture.regionWidth * scale
        val spriteHeight = texture.regionHeight * scale

        val centerX = (x - y) * projection.tileWidth / 2f
        val topY = -(x + y) * projection.tileHeight / 2f + elevation * projection.elevationStep

        batch.draw(
            texture,
            centerX - spriteWidth / 2f,
            topY - spriteHeight + offsetY,
            spriteWidth,
            spriteHeight
        )
    }
}