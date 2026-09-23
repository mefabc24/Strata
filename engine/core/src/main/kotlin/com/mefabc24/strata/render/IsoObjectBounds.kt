package com.mefabc24.strata.render

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject

/**
 * Calculates the world-space rectangle used to draw an object.
 */
object IsoObjectBounds {

    fun calculate(
        projection: IsoProjection,
        placed: PlacedObject,
        visual: ObjectVisual,
        result: Rectangle,
        elevation: Int = 0
    ): Rectangle {
        val occupied = placed.occupiedTiles()

        val minX = occupied.minOf { it.first }
        val maxX = occupied.maxOf { it.first }
        val minY = occupied.minOf { it.second }
        val maxY = occupied.maxOf { it.second }

        val widthInTiles = maxX - minX + 1
        val heightInTiles = maxY - minY + 1

        val footprintWidth =
            (widthInTiles + heightInTiles) * projection.tileWidth / 2f

        val baseWidth = visual.width ?: footprintWidth

        val baseHeight = visual.height
            ?: (baseWidth * visual.texture.regionHeight /
                    visual.texture.regionWidth)

        val spriteWidth = baseWidth * visual.scale
        val spriteHeight = baseHeight * visual.scale

        val left = projection.tileToWorld(minX, maxY).x -
                projection.tileWidth / 2f

        val front = projection.tileToWorld(
            x = maxX,
            y = maxY,
            elevation = elevation
        )

        return result.set(
            left + (footprintWidth - spriteWidth) / 2f + visual.offsetX,
            front.y - projection.tileHeight + visual.offsetY,
            spriteWidth,
            spriteHeight
        )
    }
}