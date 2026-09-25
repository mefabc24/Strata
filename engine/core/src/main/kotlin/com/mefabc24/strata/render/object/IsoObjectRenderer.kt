package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject

/**
 * Draws placed objects using their footprint and visual configuration.
 *
 * The caller owns the SpriteBatch and determines the draw order.
 */
class IsoObjectRenderer(
    private val projection: IsoProjection,
    objectSettings: ObjectRenderingSettings = ObjectRenderingSettings()
) {

    private val objectSettings = objectSettings.copy().also {
        it.validate()
    }

    private val bounds = Rectangle()

    fun render(
        batch: SpriteBatch,
        placed: PlacedObject,
        visual: ObjectVisual
    ) {
        IsoObjectBounds.calculate(
            projection = projection,
            placed = placed,
            visual = visual,
            result = bounds,
            objectSettings = objectSettings
        )

        batch.draw(
            visual.texture,
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height
        )
    }
}
