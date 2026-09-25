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
    private val terrainFillRenderer = IsoTerrainFillRenderer(projection)
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
    private var cachedTerrainVersion = -1L

    private var normalRenderPlan: List<WorldRenderPrimitive> = emptyList()

    val stats = RenderStats()

    fun render(
        world: World,
        camera: OrthographicCamera,
        textureFor: (Tile) -> TextureRegion?,
        terrainFillFor: (Tile) -> TextureRegion? = { null },
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
            cachedHeightVersion != world.heightVersion ||
            cachedTerrainVersion != world.terrainVersion
        ) {
            normalRenderPlan = WorldRenderPlan.create(
                world = world,
                projection = projection,
                hasFillFor = { tile -> terrainFillFor(tile) != null }
            )

            cachedWorld = world
            cachedObjectVersion = world.objectVersion
            cachedHeightVersion = world.heightVersion
            cachedTerrainVersion = world.terrainVersion
        }

        val viewWidth = camera.viewportWidth * camera.zoom
        val viewHeight = camera.viewportHeight * camera.zoom

        visibleArea.set(
            camera.position.x - viewWidth / 2f,
            camera.position.y - viewHeight / 2f,
            viewWidth,
            viewHeight
        )

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

        val renderPlan = WorldRenderPlan.withPreview(
            normalItems = normalRenderPlan,
            preview = preview
        )

        for (item in renderPlan) {
            when (item) {
                is TerrainFill -> {
                    if (item.x + item.y !in terrainDepths) continue

                    val tile = world.getTile(item.x, item.y) ?: continue
                    val texture = terrainFillFor(tile) ?: continue
                    val offsetY = terrainOffsetY(
                        x = item.x,
                        y = item.y,
                        raisedTile = raisedTile,
                        raiseOffsetY = raiseOffsetY
                    )

                    renderFill(
                        item = item,
                        texture = texture,
                        offsetY = offsetY
                    )
                }

                is TerrainCell -> {
                    if (item.x + item.y !in terrainDepths) continue

                    renderCell(
                        world = world,
                        item = item,
                        overlayIds = overlayIds,
                        textureFor = textureFor,
                        raisedTile = raisedTile,
                        raiseOffsetY = raiseOffsetY
                    )
                }

                is WorldObjectPrimitive -> {
                    renderObject(
                        world = world,
                        placed = item.placedObject,
                        visual = objectVisualFor(item.placedObject),
                        preview = null
                    )
                }

                is PreviewRenderItem -> {
                    renderObject(
                        world = world,
                        placed = item.preview.placedObject,
                        visual = objectVisualFor(item.preview.placedObject),
                        preview = item.preview
                    )
                }
            }
        }

        batch.end()

        stats.drawCalls = batch.renderCalls

        stats.cpuRenderMs =
            (System.nanoTime() - renderStartNanos) / 1_000_000.0
    }

    private fun renderFill(
        item: TerrainFill,
        texture: TextureRegion,
        offsetY: Float
    ) {
        terrainFillRenderer.bounds(
            x = item.x,
            y = item.y,
            elevation = item.surfaceElevation,
            levelBelowSurface = item.levelBelowSurface,
            face = item.face,
            texture = texture,
            result = tileBounds,
            offsetY = offsetY
        )

        if (!tileBounds.overlaps(visibleArea)) return

        terrainFillRenderer.render(
            batch = batch,
            x = item.x,
            y = item.y,
            elevation = item.surfaceElevation,
            levelBelowSurface = item.levelBelowSurface,
            face = item.face,
            texture = texture,
            offsetY = offsetY
        )
        stats.terrainDrawn++
    }

    private fun renderCell(
        world: World,
        item: TerrainCell,
        overlayIds: List<String>,
        textureFor: (Tile) -> TextureRegion?,
        raisedTile: TilePosition?,
        raiseOffsetY: Float
    ) {
        val offsetY = terrainOffsetY(
            x = item.x,
            y = item.y,
            raisedTile = raisedTile,
            raiseOffsetY = raiseOffsetY
        )

        stats.terrainChecked++

        world.getTile(item.x, item.y)
            ?.let(textureFor)
            ?.let { texture ->
                renderTerrainSprite(
                    x = item.x,
                    y = item.y,
                    elevation = item.elevation,
                    texture = texture,
                    offsetY = offsetY
                )
            }

        for (layerId in overlayIds) {
            stats.terrainChecked++

            world.getOverlayTile(layerId, item.x, item.y)
                ?.let(textureFor)
                ?.let { texture ->
                    renderTerrainSprite(
                        x = item.x,
                        y = item.y,
                        elevation = item.elevation,
                        texture = texture,
                        offsetY = offsetY
                    )
                }
        }
    }

    private fun renderObject(
        world: World,
        placed: PlacedObject,
        visual: ObjectVisual?,
        preview: PlacementPreview?
    ) {
        if (preview == null) {
            stats.objectsChecked++
        }

        if (visual == null) return

        val elevation = world.getHeight(placed.x, placed.y) ?: 0

        IsoObjectBounds.calculate(
            projection = projection,
            placed = placed,
            visual = visual,
            result = objectBounds,
            elevation = elevation,
            objectSettings = objectSettings
        )

        if (!objectBounds.overlaps(visibleArea)) return

        if (preview != null) {
            batch.color = if (preview.valid) {
                preview.style.validColor
            } else {
                preview.style.invalidColor
            }
        }

        objectRenderer.render(
            batch = batch,
            placed = placed,
            visual = visual,
            elevation = elevation
        )

        if (preview == null) {
            stats.objectsDrawn++
        } else {
            stats.previewsDrawn++
            batch.setColor(1f, 1f, 1f, 1f)
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
