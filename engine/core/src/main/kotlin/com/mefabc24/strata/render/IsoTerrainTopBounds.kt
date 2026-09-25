package com.mefabc24.strata.render

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection

/** Calculates the world-space bounds of one logical terrain top face. */
internal object IsoTerrainTopBounds {

    fun calculate(
        projection: IsoProjection,
        x: Int,
        y: Int,
        elevation: Int,
        result: Rectangle,
        offsetY: Float = 0f
    ): Rectangle {
        val tileTop = projection.tileToWorld(x, y, elevation)

        return result.set(
            tileTop.x - projection.tileWidth / 2f,
            tileTop.y - projection.tileHeight + offsetY,
            projection.tileWidth,
            projection.tileHeight
        )
    }
}
