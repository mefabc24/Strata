package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.render.`object`.IsoObjectBounds
import com.mefabc24.strata.render.`object`.IsoObjectRenderer
import com.mefabc24.strata.render.`object`.ObjectRenderingSettings
import com.mefabc24.strata.render.`object`.ObjectVisual
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.entity.IsoEntityBounds
import com.mefabc24.strata.render.entity.IsoEntityRenderer
import com.mefabc24.strata.render.order.PreviewRenderItem
import com.mefabc24.strata.render.order.TerrainCell
import com.mefabc24.strata.render.order.WorldObjectPrimitive
import com.mefabc24.strata.render.order.WorldEntityPrimitive
import com.mefabc24.strata.render.order.WorldRenderPlan
import com.mefabc24.strata.render.order.WorldRenderPrimitive
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.terrain.IsoTerrainBounds
import com.mefabc24.strata.render.terrain.IsoTerrainRenderer
import com.mefabc24.strata.render.terrain.TerrainDepthCulling
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

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
    private val entityRenderer = IsoEntityRenderer(projection)

    private val visibleArea = Rectangle()
    private val tileBounds = Rectangle()
    private val objectBounds = Rectangle()
    private val entityBounds = Rectangle()

    private var cachedWorld: World? = null
    private var cachedObjectVersion = -1L
    private var cachedEntityVersion = -1L

    private var normalRenderPlan: List<WorldRenderPrimitive> = emptyList()

    val stats = RenderStats()

    fun render(
        world: World,
        camera: OrthographicCamera,
        textureFor: (Tile, Float) -> TextureRegion?,
        objectVisualFor: (PlacedObject) -> ObjectVisual? = { null },
        entityVisualFor: (WorldEntity) -> EntityVisual? = { null },
        previews: List<PlacementPreview> = emptyList(),
        animationTime: Float = 0f,
        maxTerrainSpriteHeight: Float = Float.POSITIVE_INFINITY
    ) {
        stats.reset()

        val renderStartNanos = System.nanoTime()

        if (
            cachedWorld !== world ||
            cachedObjectVersion != world.objectVersion ||
            cachedEntityVersion != world.entityVersion ||
            world.getEntities().isNotEmpty()
        ) {
            normalRenderPlan = WorldRenderPlan.create(
                world = world,
                projection = projection
            )

            cachedWorld = world
            cachedObjectVersion = world.objectVersion
            cachedEntityVersion = world.entityVersion
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
            maxDepth = world.width + world.height - 2
        )

        val overlayIds = world.overlayLayerIds
        batch.begin()

        val renderPlan = WorldRenderPlan.withPreviews(
            normalItems = normalRenderPlan,
            previews = previews
        )

        for (item in renderPlan) {
            when (item) {
                is TerrainCell -> {
                    if (item.x + item.y !in terrainDepths) continue

                    renderCell(
                        world = world,
                        item = item,
                        overlayIds = overlayIds,
                        textureFor = textureFor,
                        animationTime = animationTime
                    )
                }

                is WorldObjectPrimitive -> {
                    renderObject(
                        placed = item.placedObject,
                        visual = objectVisualFor(item.placedObject),
                        preview = null,
                        animationTime = animationTime
                    )
                }

                is WorldEntityPrimitive -> {
                    renderEntity(
                        entity = item.worldEntity,
                        visual = entityVisualFor(item.worldEntity),
                        animationTime = animationTime
                    )
                }

                is PreviewRenderItem -> {
                    renderObject(
                        placed = item.preview.placedObject,
                        visual = objectVisualFor(item.preview.placedObject),
                        preview = item.preview,
                        animationTime = animationTime
                    )
                }
            }
        }

        batch.end()

        stats.drawCalls = batch.renderCalls

        stats.cpuRenderMs =
            (System.nanoTime() - renderStartNanos) / 1_000_000.0
    }

    private fun renderEntity(
        entity: WorldEntity,
        visual: EntityVisual?,
        animationTime: Float,
        recordStats: Boolean = true
    ) {
        if (recordStats) stats.entitiesChecked++
        if (visual == null) return

        IsoEntityBounds.calculate(
            projection = projection,
            entity = entity,
            visual = visual,
            result = entityBounds
        )
        if (!entityBounds.overlaps(visibleArea)) return

        entityRenderer.render(
            batch = batch,
            entity = entity,
            visual = visual,
            animationTime = animationTime
        )
        if (recordStats) stats.entitiesDrawn++
    }

    private fun renderCell(
        world: World,
        item: TerrainCell,
        overlayIds: List<String>,
        textureFor: (Tile, Float) -> TextureRegion?,
        animationTime: Float
    ) {
        stats.terrainChecked++

        world.getTile(item.x, item.y)
            ?.let { textureFor(it, animationTime) }
            ?.let { texture ->
                renderTerrainSprite(
                    x = item.x,
                    y = item.y,
                    texture = texture
                )
            }

        for (layerId in overlayIds) {
            stats.terrainChecked++

            world.getOverlayTile(layerId, item.x, item.y)
                ?.let { textureFor(it, animationTime) }
                ?.let { texture ->
                    renderTerrainSprite(
                        x = item.x,
                        y = item.y,
                        texture = texture
                    )
                }
        }
    }

    private fun renderObject(
        placed: PlacedObject,
        visual: ObjectVisual?,
        preview: PlacementPreview?,
        animationTime: Float,
        recordStats: Boolean = true
    ) {
        if (preview == null && recordStats) {
            stats.objectsChecked++
        }

        if (visual == null) return

        IsoObjectBounds.calculate(
            projection = projection,
            placed = placed,
            visual = visual,
            result = objectBounds,
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
            animationTime = animationTime
        )

        if (recordStats) {
            if (preview == null) {
                stats.objectsDrawn++
            } else {
                stats.previewsDrawn++
            }
        }

        if (preview != null) {
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    internal fun renderWorldOverlay(
        camera: OrthographicCamera,
        objectVisualFor: (PlacedObject) -> ObjectVisual?,
        entityVisualFor: (WorldEntity) -> EntityVisual?,
        previews: List<PlacementPreview>,
        animationTime: Float
    ) {
        batch.projectionMatrix = camera.combined

        batch.begin()

        for (item in normalRenderPlan) {
            if (item is WorldObjectPrimitive) {
                renderObject(
                    placed = item.placedObject,
                    visual = objectVisualFor(item.placedObject),
                    preview = null,
                    animationTime = animationTime,
                    recordStats = false
                )
            } else if (item is WorldEntityPrimitive) {
                renderEntity(
                    entity = item.worldEntity,
                    visual = entityVisualFor(item.worldEntity),
                    animationTime = animationTime,
                    recordStats = false
                )
            }
        }

        for (preview in previews) {
            renderObject(
                placed = preview.placedObject,
                visual = objectVisualFor(preview.placedObject),
                preview = preview,
                animationTime = animationTime,
                recordStats = false
            )
        }

        batch.end()
    }

    private fun renderTerrainSprite(
        x: Int,
        y: Int,
        texture: TextureRegion
    ) {
        IsoTerrainBounds.calculate(
            projection = projection,
            x = x,
            y = y,
            texture = texture,
            result = tileBounds
        )

        if (!tileBounds.overlaps(visibleArea)) return

        terrainRenderer.render(
            batch = batch,
            x = x,
            y = y,
            texture = texture
        )
        stats.terrainDrawn++
    }

    fun dispose() {
        batch.dispose()
    }
}
