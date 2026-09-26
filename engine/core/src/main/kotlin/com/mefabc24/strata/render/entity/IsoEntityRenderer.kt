package com.mefabc24.strata.render.entity

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.WorldEntity

/** Draws entity sprites in the order selected by the world renderer. */
class IsoEntityRenderer(
    private val projection: IsoProjection
) {
    private val bounds = Rectangle()

    fun render(
        batch: SpriteBatch,
        entity: WorldEntity,
        visual: EntityVisual,
        animationTime: Float
    ) {
        IsoEntityBounds.calculate(
            projection = projection,
            entity = entity,
            visual = visual,
            result = bounds
        )

        batch.draw(
            visual.frameAt(animationTime).texture,
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height
        )
    }
}
