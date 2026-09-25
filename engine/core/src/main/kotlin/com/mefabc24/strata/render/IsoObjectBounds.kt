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
        objectSettings: ObjectRenderingSettings = ObjectRenderingSettings()
    ): Rectangle {
        val occupied = placed.occupiedTiles()

        val minX = occupied.minOf { it.x }
        val maxX = occupied.maxOf { it.x }
        val minY = occupied.minOf { it.y }
        val maxY = occupied.maxOf { it.y }

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

        val surfaceAnchor = projection.surfaceAnchor(maxX, maxY)

        return result.set(
            left + (footprintWidth - spriteWidth) / 2f +
                    objectSettings.offsetX + visual.offsetX,
            surfaceAnchor.y + objectSettings.offsetY + visual.offsetY,
            spriteWidth,
            spriteHeight
        )
    }
}
