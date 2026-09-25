package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection

/**
 * Draws complete authored terrain sprites using logical terrain anchoring.
 *
 * The caller owns the SpriteBatch and determines the draw order.
 */
class IsoTerrainRenderer(
    private val projection: IsoProjection
) {
    private val bounds = Rectangle()

    fun render(
        batch: SpriteBatch,
        x: Int,
        y: Int,
        texture: TextureRegion
    ) {
        IsoTerrainBounds.calculate(
            projection = projection,
            x = x,
            y = y,
            texture = texture,
            result = bounds
        )

        batch.draw(
            texture,
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height
        )
    }
}
