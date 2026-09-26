package com.mefabc24.strata.render.entity

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.WorldEntity

/** Calculates a bottom-center anchored entity sprite rectangle. */
object IsoEntityBounds {

    fun calculate(
        projection: IsoProjection,
        entity: WorldEntity,
        visual: EntityVisual,
        result: Rectangle
    ): Rectangle {
        val baseWidth = visual.width ?: visual.texture.regionWidth.toFloat()
        val baseHeight = visual.height ?: (
            baseWidth * visual.texture.regionHeight /
                visual.texture.regionWidth
            )
        val width = baseWidth * visual.scale
        val height = baseHeight * visual.scale
        val anchor = projection.tileToWorld(
            entity.position.x,
            entity.position.y
        )

        return result.set(
            anchor.x - width / 2f + visual.offsetX,
            anchor.y + visual.offsetY,
            width,
            height
        )
    }
}
