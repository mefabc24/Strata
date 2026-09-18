package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World

/**
 * Renders terrain and world objects in isometric depth order.
 */
class IsoWorldRenderer(
    projection: IsoProjection
) {
    private val batch = SpriteBatch()

    private val terrainRenderer = IsoTerrainRenderer(projection)
    private val objectRenderer = IsoObjectRenderer(projection)

    fun render(
        world: World,
        camera: OrthographicCamera,
        textureFor: (Tile) -> TextureRegion?,
        raisedTile: Pair<Int, Int>? = null,
        raiseOffsetY: Float = 0f,
        objectVisualFor: (PlacedObject) -> ObjectVisual? = { null },
        preview: PlacementPreview? = null
    ) {
        val objectsByDepth = world.getObjects().groupBy { placed ->
            placed.occupiedTiles().maxOf { (x, y) -> x + y }
        }

        val previewDepth = preview
            ?.placedObject
            ?.occupiedTiles()
            ?.maxOf { (x, y) -> x + y }
            ?.coerceIn(0, world.width + world.height - 2)

        batch.projectionMatrix = camera.combined
        batch.begin()

        for (depth in 0 until world.width + world.height - 1) {
            val minX = maxOf(0, depth - world.height + 1)
            val maxX = minOf(world.width - 1, depth)

            // Render terrain at the current depth.
            for (x in minX..maxX) {
                val y = depth - x

                val tile = world.getTile(x, y) ?: continue
                val texture = textureFor(tile) ?: continue

                val offsetY = if (raisedTile == (x to y)) {
                    raiseOffsetY
                } else {
                    0f
                }

                terrainRenderer.render(
                    batch = batch,
                    x = x,
                    y = y,
                    texture = texture,
                    offsetY = offsetY
                )
            }

            // Render objects after the terrain at their depth.
            objectsByDepth[depth]?.forEach { placed ->
                val visual = objectVisualFor(placed) ?: return@forEach

                objectRenderer.render(batch, placed, visual)
            }

            if (preview != null && depth == previewDepth) {
                val visual = objectVisualFor(preview.placedObject)

                if (visual != null) {
                    val color = if (preview.valid) {
                        preview.style.validColor
                    } else {
                        preview.style.invalidColor
                    }

                    batch.color = color

                    objectRenderer.render(batch, preview.placedObject, visual)

                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        }

        batch.end()
    }

    fun dispose() {
        batch.dispose()
    }
}