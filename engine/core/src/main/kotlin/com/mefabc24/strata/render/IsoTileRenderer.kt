package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World

/**
 * Renders terrain tiles in isometric depth order.
 */
class IsoTileRenderer(
    private val projection: IsoProjection
) {
    private val batch = SpriteBatch()

    fun render(
        world: World,
        camera: OrthographicCamera,
        textureFor: (Tile) -> TextureRegion?,
        raisedTile: Pair<Int, Int>? = null,
        raiseOffsetY: Float = 0f
    ) {
        batch.projectionMatrix = camera.combined
        batch.begin()

        for (depth in 0 until world.width + world.height - 1) {
            val minX = maxOf(0, depth - world.height + 1)
            val maxX = minOf(world.width - 1, depth)

            for (x in minX..maxX) {
                val y = depth - x

                val tile = world.getTile(x, y) ?: continue
                val texture = textureFor(tile) ?: continue

                val offsetY = if (raisedTile == (x to y)) {
                    raiseOffsetY
                } else {
                    0f
                }

                drawTile(x, y, texture, offsetY)
            }
        }
        batch.end()
    }

    private fun drawTile(
        x: Int,
        y: Int,
        texture: TextureRegion,
        offsetY: Float = 0f
    ) {
        val position = projection.tileToWorld(x, y)

        val scale = projection.tileWidth / texture.regionWidth
        val spriteWidth = texture.regionWidth * scale
        val spriteHeight = texture.regionHeight * scale

        batch.draw(
            texture,
            position.x - spriteWidth / 2f,
            position.y - spriteHeight + offsetY,
            spriteWidth,
            spriteHeight
        )
    }

    fun dispose() {
        batch.dispose()
    }
}