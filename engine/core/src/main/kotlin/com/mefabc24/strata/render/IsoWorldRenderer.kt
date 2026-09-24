package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Renders terrain and world objects in isometric depth order.
 */
class IsoWorldRenderer(
    private val projection: IsoProjection,
    objectSettings: ObjectRenderingSettings = ObjectRenderingSettings()
) {
    private val objectSettings = objectSettings.copy().also {
        it.validate()
    }

    private val batch = SpriteBatch()

    private val terrainRenderer = IsoTerrainRenderer(projection)
    private val objectRenderer = IsoObjectRenderer(
        projection = projection,
        objectSettings = this.objectSettings
    )

    private val visibleArea = Rectangle()
    private val tileBounds = Rectangle()
    private val objectBounds = Rectangle()

    private var cachedWorld: World? = null
    private var cachedObjectVersion = -1L
    private var cachedHeightVersion = -1L

    private var orderedObjects: List<PlacedObject> = emptyList()

    val stats = RenderStats()

    fun render(
        world: World,
        camera: OrthographicCamera,
        textureFor: (Tile) -> TextureRegion?,
        terrainCliffsFor: (Tile) -> TerrainCliffVisuals? = { null },
        raisedTile: TilePosition? = null,
        raiseOffsetY: Float = 0f,
        objectVisualFor: (PlacedObject) -> ObjectVisual? = { null },
        preview: PlacementPreview? = null,
        maxTerrainSpriteHeight: Float = Float.POSITIVE_INFINITY
    ) {
        stats.reset()

        val renderStartNanos = System.nanoTime()

        if (
            cachedWorld !== world ||
            cachedObjectVersion != world.objectVersion ||
            cachedHeightVersion != world.heightVersion
        ) {
            orderedObjects = IsoObjectOrdering.backToFront(
                objects = world.getObjects(),
                projection = projection,
                elevationFor = { placed ->
                    world.getHeight(placed.x, placed.y) ?: 0
                }
            )

            cachedWorld = world
            cachedObjectVersion = world.objectVersion
            cachedHeightVersion = world.heightVersion
        }

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
            logicalTileHeight = projection.logicalTileHeight,
            maxSpriteHeight = maxTerrainSpriteHeight,
            raisedOffsetY = raiseOffsetY,
            maxDepth = world.width + world.height - 2,
            maxElevationOffset = world.maxHeight * projection.elevationStep
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

                // Ground cliffs and surfaces share the normal terrain depth.
                for (x in minX..maxX) {
                    val y = depth - x
                    val elevation = world.getHeight(x, y) ?: continue
                    val tile = world.getTile(x, y) ?: continue
                    val texture = textureFor(tile) ?: continue
                    val offsetY = terrainOffsetY(
                        x = x,
                        y = y,
                        raisedTile = raisedTile,
                        raiseOffsetY = raiseOffsetY
                    )

                    stats.terrainChecked++

                    renderCliffs(
                        world = world,
                        x = x,
                        y = y,
                        elevation = elevation,
                        visuals = terrainCliffsFor(tile),
                        offsetY = offsetY
                    )

                    renderTerrainSprite(
                        x = x,
                        y = y,
                        elevation = elevation,
                        texture = texture,
                        offsetY = offsetY
                    )
                }

                // Overlays stay attached to the ground surface and have no cliffs.
                for (layerId in overlayIds) {
                    for (x in minX..maxX) {
                        val y = depth - x
                        val elevation = world.getHeight(x, y) ?: continue
                        val tile = world.getOverlayTile(
                            layerId = layerId,
                            x = x,
                            y = y
                        ) ?: continue
                        val texture = textureFor(tile) ?: continue
                        val offsetY = terrainOffsetY(
                            x = x,
                            y = y,
                            raisedTile = raisedTile,
                            raiseOffsetY = raiseOffsetY
                        )

                        stats.terrainChecked++

                        renderTerrainSprite(
                            x = x,
                            y = y,
                            elevation = elevation,
                            texture = texture,
                            offsetY = offsetY
                        )
                    }
                }
            }

        }

        val objectPlan = ObjectRenderPlan.create(
            orderedObjects = orderedObjects,
            preview = preview
        )

        for (item in objectPlan) {
            val placed = item.placedObject
            val visual = objectVisualFor(placed) ?: continue
            val elevation = world.getHeight(placed.x, placed.y) ?: 0

            IsoObjectBounds.calculate(
                projection = projection,
                placed = placed,
                visual = visual,
                result = objectBounds,
                elevation = elevation,
                objectSettings = objectSettings
            )

            when (item) {
                is ObjectRenderItem.WorldObject -> stats.objectsChecked++
                is ObjectRenderItem.Preview -> Unit
            }

            if (!objectBounds.overlaps(visibleArea)) continue

            if (item is ObjectRenderItem.Preview) {
                batch.color = if (item.preview.valid) {
                    item.preview.style.validColor
                } else {
                    item.preview.style.invalidColor
                }
            }

            objectRenderer.render(
                batch = batch,
                placed = placed,
                visual = visual,
                elevation = elevation
            )

            when (item) {
                is ObjectRenderItem.WorldObject -> stats.objectsDrawn++
                is ObjectRenderItem.Preview -> {
                    stats.previewsDrawn++
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        }

        batch.end()

        stats.drawCalls = batch.renderCalls

        stats.cpuRenderMs =
            (System.nanoTime() - renderStartNanos) / 1_000_000.0
    }

    private fun renderCliffs(
        world: World,
        x: Int,
        y: Int,
        elevation: Int,
        visuals: TerrainCliffVisuals?,
        offsetY: Float
    ) {
        if (visuals == null) return

        for (part in TerrainCliffPlan.create(world, x, y)) {
            val texture = when (part.side) {
                TerrainCliffSide.LEFT -> visuals.left
                TerrainCliffSide.RIGHT -> visuals.right
            } ?: continue

            IsoCliffBounds.calculate(
                projection = projection,
                x = x,
                y = y,
                elevation = elevation,
                levelBelowSurface = part.levelBelowSurface,
                texture = texture,
                result = tileBounds,
                offsetY = offsetY
            )

            if (!tileBounds.overlaps(visibleArea)) continue

            batch.draw(
                texture,
                tileBounds.x,
                tileBounds.y,
                tileBounds.width,
                tileBounds.height
            )
            stats.terrainDrawn++
        }
    }

    private fun renderTerrainSprite(
        x: Int,
        y: Int,
        elevation: Int,
        texture: TextureRegion,
        offsetY: Float
    ) {
        IsoTerrainBounds.calculate(
            projection = projection,
            x = x,
            y = y,
            texture = texture,
            result = tileBounds,
            offsetY = offsetY,
            elevation = elevation
        )

        if (!tileBounds.overlaps(visibleArea)) return

        terrainRenderer.render(
            batch = batch,
            x = x,
            y = y,
            texture = texture,
            offsetY = offsetY,
            elevation = elevation
        )
        stats.terrainDrawn++
    }

    private fun terrainOffsetY(
        x: Int,
        y: Int,
        raisedTile: TilePosition?,
        raiseOffsetY: Float
    ): Float {
        return if (
            raisedTile != null &&
            raisedTile.x == x &&
            raisedTile.y == y
        ) {
            raiseOffsetY
        } else {
            0f
        }
    }

    fun dispose() {
        batch.dispose()
    }
}
