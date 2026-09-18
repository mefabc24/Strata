package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject

/**
 * Draws placed objects using their footprint and visual configuration.
 *
 * The caller owns the SpriteBatch and determines the draw order.
 */
class IsoObjectRenderer(
    private val projection: IsoProjection
) {

    fun render(
        batch: SpriteBatch,
        placed: PlacedObject,
        visual: ObjectVisual
    ) {
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

        batch.draw(
            visual.texture,
            left + visual.offsetX,
            front.y - projection.tileHeight + visual.offsetY,
            spriteWidth,
            spriteHeight
        )
    }
}