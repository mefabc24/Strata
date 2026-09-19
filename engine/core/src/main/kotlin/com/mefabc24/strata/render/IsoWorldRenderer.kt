package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import com.badlogic.gdx.math.Rectangle
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Renders terrain and world objects in isometric depth order.
 */
class IsoWorldRenderer(
    private val projection: IsoProjection
) {
    private val batch = SpriteBatch()

    private val terrainRenderer = IsoTerrainRenderer(projection)
    private val objectRenderer = IsoObjectRenderer(projection)

    private val visibleArea = Rectangle()
    private val tileBounds = Rectangle()
    private val objectBounds = Rectangle()

    private var cachedWorld: World? = null
    private var cachedObjectVersion = -1L

    private var objectsByDepth: Map<Int, List<PlacedObject>> = emptyMap()

    fun render(
        world: World,
        camera: OrthographicCamera,
        textureFor: (Tile) -> TextureRegion?,
        raisedTile: Pair<Int, Int>? = null,
        raiseOffsetY: Float = 0f,
        objectVisualFor: (PlacedObject) -> ObjectVisual? = { null },
        preview: PlacementPreview? = null
    ) {
        if (cachedWorld !== world || cachedObjectVersion != world.objectVersion) {
            objectsByDepth = world.getObjects().groupBy { placed ->
                placed.occupiedTiles().maxOf { (x, y) -> x + y }
            }

            cachedWorld = world
            cachedObjectVersion = world.objectVersion
        }

        val previewDepth = preview
            ?.placedObject
            ?.occupiedTiles()
            ?.maxOf { (x, y) -> x + y }
            ?.coerceIn(0, world.width + world.height - 2)

        val viewWidth = camera.viewportWidth * camera.zoom
        val viewHeight = camera.viewportHeight * camera.zoom

        visibleArea.set(
            camera.position.x - viewWidth / 2f,
            camera.position.y - viewHeight / 2f,
            viewWidth,
            viewHeight
        )

        val leftInTiles =
            visibleArea.x / projection.tileWidth - 0.5f

        val rightInTiles =
            (visibleArea.x + visibleArea.width) / projection.tileWidth + 0.5f

        batch.projectionMatrix = camera.combined
        batch.begin()

        for (depth in 0 until world.width + world.height - 1) {
            val depthOffset = depth / 2f

            val minX = maxOf(
                0,
                depth - world.height + 1,
                floor(leftInTiles + depthOffset).toInt()
            )

            val maxX = minOf(
                world.width - 1,
                depth,
                ceil(rightInTiles + depthOffset).toInt()
            )

            // Render only terrain sprites intersecting the camera.
            for (x in minX..maxX) {
                val y = depth - x

                val tile = world.getTile(x, y) ?: continue
                val texture = textureFor(tile) ?: continue

                val offsetY = if (raisedTile == (x to y)) {
                    raiseOffsetY
                } else {
                    0f
                }

                val scale = projection.tileWidth / texture.regionWidth

                val spriteWidth = texture.regionWidth * scale
                val spriteHeight = texture.regionHeight * scale

                val centerX = (x - y) * projection.tileWidth / 2f
                val topY = -depth * projection.tileHeight / 2f + offsetY

                tileBounds.set(
                    centerX - spriteWidth / 2f,
                    topY - spriteHeight,
                    spriteWidth,
                    spriteHeight
                )

                if (!tileBounds.overlaps(visibleArea)) {
                    continue
                }

                terrainRenderer.render(
                    batch = batch,
                    x = x,
                    y = y,
                    texture = texture,
                    offsetY = offsetY
                )
            }

            objectsByDepth[depth]?.forEach { placed ->
                val visual = objectVisualFor(placed) ?: return@forEach

                IsoObjectBounds.calculate(
                    projection = projection,
                    placed = placed,
                    visual = visual,
                    result = objectBounds
                )

                if (!objectBounds.overlaps(visibleArea)) {
                    return@forEach
                }

                objectRenderer.render(batch, placed, visual)
            }

            if (preview != null && depth == previewDepth) {
                val visual = objectVisualFor(preview.placedObject)

                if (visual != null) {
                    IsoObjectBounds.calculate(
                        projection = projection,
                        placed = preview.placedObject,
                        visual = visual,
                        result = objectBounds
                    )

                    if (objectBounds.overlaps(visibleArea)) {
                        val color = if (preview.valid) {
                            preview.style.validColor
                        } else {
                            preview.style.invalidColor
                        }

                        batch.color = color

                        objectRenderer.render(
                            batch,
                            preview.placedObject,
                            visual
                        )

                        batch.setColor(1f, 1f, 1f, 1f)
                    }
                }
            }
        }

        batch.end()
    }

    fun dispose() {
        batch.dispose()
    }
}