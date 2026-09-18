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
        result: Rectangle
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

        val scale = footprintWidth / visual.texture.regionWidth

        val spriteWidth = visual.texture.regionWidth * scale
        val spriteHeight = visual.texture.regionHeight * scale

        val left = projection.tileToWorld(minX, maxY).x -
                projection.tileWidth / 2f

        val front = projection.tileToWorld(maxX, maxY)

        return result.set(
            left + visual.offsetX,
            front.y - projection.tileHeight + visual.offsetY,
            spriteWidth,
            spriteHeight
        )
    }
}