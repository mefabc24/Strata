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

    val stats = RenderStats()

    fun render(
        world: World,
        camera: OrthographicCamera,
        textureFor: (Tile) -> TextureRegion?,
        raisedTile: Pair<Int, Int>? = null,
        raiseOffsetY: Float = 0f,
        objectVisualFor: (PlacedObject) -> ObjectVisual? = { null },
        preview: PlacementPreview? = null,
        maxTerrainSpriteHeight: Float = Float.POSITIVE_INFINITY
    ) {
        stats.reset()

        val renderStartNanos = System.nanoTime()

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

        val terrainDepths = TerrainDepthCulling.visibleDepths(
            visibleBottom = visibleArea.y,
            visibleTop = visibleArea.y + visibleArea.height,
            tileHeight = projection.tileHeight,
            maxSpriteHeight = maxTerrainSpriteHeight,
            raisedOffsetY = raiseOffsetY,
            maxDepth = world.width + world.height - 2
        )

        val overlayIds = world.overlayLayerIds
        batch.begin()

        for (depth in 0 until world.width + world.height - 1) {
            val depthOffset = depth / 2f

            if (depth in terrainDepths) {
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

                // Render ground first, followed by overlays in registration order.
                for (layerIndex in -1 until overlayIds.size) {
                    for (x in minX..maxX) {
                        val y = depth - x

                        stats.terrainChecked++

                        val tile = if (layerIndex == -1) {
                            world.getTile(x, y)
                        } else {
                            world.getOverlayTile(
                                layerId = overlayIds[layerIndex],
                                x = x,
                                y = y
                            )
                        } ?: continue

                        val texture = textureFor(tile) ?: continue

                        val offsetY = if (
                            raisedTile != null &&
                            raisedTile.first == x &&
                            raisedTile.second == y
                        ) {
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

                        stats.terrainDrawn++
                    }
                }
            }

            objectsByDepth[depth]?.forEach { placed ->
                stats.objectsChecked++

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
                stats.objectsDrawn++
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
                        stats.previewsDrawn++

                        batch.setColor(1f, 1f, 1f, 1f)
                    }
                }
            }
        }

        batch.end()

        stats.drawCalls = batch.renderCalls

        stats.cpuRenderMs =
            (System.nanoTime() - renderStartNanos) / 1_000_000.0
    }

    fun dispose() {
        batch.dispose()
    }
}