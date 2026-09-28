package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.lighting.Lighting
import com.mefabc24.strata.render.`object`.IsoObjectBounds
import com.mefabc24.strata.render.`object`.IsoObjectRenderer
import com.mefabc24.strata.render.`object`.ObjectRenderingSettings
import com.mefabc24.strata.render.`object`.ObjectVisual
import com.mefabc24.strata.render.`object`.ResolvedObjectVisual
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.entity.ResolvedEntityVisual
import com.mefabc24.strata.render.entity.IsoEntityBounds
import com.mefabc24.strata.render.entity.IsoEntityRenderer
import com.mefabc24.strata.render.order.IsoRenderOrderMetrics
import com.mefabc24.strata.render.order.PreviewRenderItem
import com.mefabc24.strata.render.order.TerrainCell
import com.mefabc24.strata.render.order.WorldObjectPrimitive
import com.mefabc24.strata.render.order.WorldEntityPrimitive
import com.mefabc24.strata.render.order.WorldRenderPlan
import com.mefabc24.strata.render.order.StaticWorldRenderPlan
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
 * Renders flat terrain first, followed by depth-ordered world content.
 */
class IsoWorldRenderer(
    private val projection: IsoProjection,
    objectSettings: ObjectRenderingSettings = ObjectRenderingSettings(),
    private val lighting: Lighting = Lighting()
) {
    private val objectSettings = objectSettings.copy().also {
        it.validate()
    }

    private val batch = SpriteBatch()
    private val lightingShader = LightingShader(projection, lighting)

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

    private var staticRenderPlan: StaticWorldRenderPlan? = null
    private var normalRenderPlan: List<WorldRenderPrimitive> = emptyList()

    private var pendingStaticPlanMs = 0.0
    private var pendingStaticPlanUpdates = 0
    private var pendingDynamicPlanMs = 0.0
    private var pendingStaticPlanRelationChecks = 0

    val stats = RenderStats()

    fun render(
        world: World,
        camera: OrthographicCamera,
        textureFor: (Tile, Float) -> TextureRegion?,
        objectVisualFor: (PlacedObject) -> ObjectVisual? = { null },
        entityVisualFor: (WorldEntity) -> EntityVisual? = { null },
        previews: List<PlacementPreview> = emptyList(),
        animationTime: Float = 0f,
        maxTerrainSpriteHeight: Float = Float.POSITIVE_INFINITY,
        resolvedObjectVisualFor: (
            (PlacedObject, Float) -> ResolvedObjectVisual?
        )? = null,
        resolvedEntityVisualFor: (
            (WorldEntity, Float) -> ResolvedEntityVisual?
        )? = null
    ) {
        stats.reset()
        stats.groundTerrainTotal = world.groundTileCount
        stats.overlayTerrainTotal = world.overlayTileCount
        stats.objectsTotal = world.placedObjectCount
        stats.entitiesTotal = world.entityCount

        val renderStartNanos = System.nanoTime()

        updateRenderPlan(world)

        stats.staticPlanMs = pendingStaticPlanMs
        stats.staticPlanUpdates = pendingStaticPlanUpdates
        stats.dynamicPlanMs = pendingDynamicPlanMs
        stats.staticPlanRelationChecks =
            pendingStaticPlanRelationChecks

        pendingStaticPlanMs = 0.0
        pendingStaticPlanUpdates = 0
        pendingDynamicPlanMs = 0.0
        pendingStaticPlanRelationChecks = 0

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

        beginWorldBatch()

        renderTerrain(
            world = world,
            terrainDepths = terrainDepths,
            overlayIds = overlayIds,
            textureFor = textureFor,
            animationTime = animationTime
        )

        for (item in normalRenderPlan) {
            when (item) {
                is TerrainCell -> Unit

                is WorldObjectPrimitive -> {
                    renderObject(
                        placed = item.placedObject,
                        visual = resolvedObjectVisualFor?.invoke(
                            item.placedObject,
                            animationTime
                        ) ?: objectVisualFor(item.placedObject)?.let {
                            ResolvedObjectVisual(
                                it,
                                animationTime,
                                null
                            )
                        },
                        preview = null
                    )
                }

                is WorldEntityPrimitive -> {
                    renderEntity(
                        entity = item.worldEntity,
                        visual = resolvedEntityVisualFor?.invoke(
                            item.worldEntity,
                            animationTime
                        ) ?: entityVisualFor(item.worldEntity)?.let {
                            ResolvedEntityVisual(
                                it,
                                animationTime,
                                null
                            )
                        }
                    )
                }
            }
        }

        for (preview in previews) {
            renderObject(
                placed = preview.placedObject,
                visual = resolvedObjectVisualFor?.invoke(
                    preview.placedObject,
                    animationTime
                ) ?: objectVisualFor(preview.placedObject)?.let {
                    ResolvedObjectVisual(
                        it,
                        animationTime,
                        null
                    )
                },
                preview = preview
            )
        }

        batch.end()

        stats.drawCalls = batch.renderCalls

        stats.cpuRenderMs =
            (System.nanoTime() - renderStartNanos) / 1_000_000.0
    }

    /**
     * Returns the current render order while reusing cached static world data.
     */
    internal fun currentRenderPlan(
        world: World
    ): List<WorldRenderPrimitive> {
        updateRenderPlan(world)
        return normalRenderPlan
    }

    private fun updateRenderPlan(world: World) {
        val cachedPlan = staticRenderPlan

        when {
            cachedWorld !== world || cachedPlan == null -> {
                val metrics = IsoRenderOrderMetrics()
                val start = System.nanoTime()

                staticRenderPlan = WorldRenderPlan.prepareStatic(
                    world = world,
                    projection = projection,
                    metrics = metrics
                )

                pendingStaticPlanMs += elapsedMs(start)
                pendingStaticPlanUpdates++
                pendingStaticPlanRelationChecks +=
                    metrics.relationChecks
            }

            cachedObjectVersion != world.objectVersion -> {
                val metrics = IsoRenderOrderMetrics()
                val start = System.nanoTime()

                staticRenderPlan = WorldRenderPlan.updateStatic(
                    previous = cachedPlan,
                    world = world,
                    projection = projection,
                    metrics = metrics
                )

                pendingStaticPlanMs += elapsedMs(start)
                pendingStaticPlanUpdates++
                pendingStaticPlanRelationChecks +=
                    metrics.relationChecks
            }
        }

        cachedWorld = world
        cachedObjectVersion = world.objectVersion

        val dynamicStart = System.nanoTime()

        normalRenderPlan = WorldRenderPlan.withEntities(
            staticPlan = checkNotNull(staticRenderPlan),
            world = world,
            projection = projection
        )

        pendingDynamicPlanMs += elapsedMs(dynamicStart)
    }

    private fun renderEntity(
        entity: WorldEntity,
        visual: ResolvedEntityVisual?,
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
            visual = visual
        )
        if (recordStats) stats.entitiesDrawn++
    }

    private fun renderTerrain(
        world: World,
        terrainDepths: IntRange,
        overlayIds: List<String>,
        textureFor: (Tile, Float) -> TextureRegion?,
        animationTime: Float
    ) {
        for (depth in terrainDepths) {
            val minX = maxOf(
                0,
                depth - world.height + 1
            )
            val maxX = minOf(
                world.width - 1,
                depth
            )

            for (x in minX..maxX) {
                val y = depth - x

                renderCell(
                    world = world,
                    x = x,
                    y = y,
                    overlayIds = overlayIds,
                    textureFor = textureFor,
                    animationTime = animationTime
                )
            }
        }
    }

    private fun renderCell(
        world: World,
        x: Int,
        y: Int,
        overlayIds: List<String>,
        textureFor: (Tile, Float) -> TextureRegion?,
        animationTime: Float
    ) {
        stats.terrainChecked++

        world.getTile(x, y)
            ?.let { textureFor(it, animationTime) }
            ?.let { texture ->
                renderTerrainSprite(
                    x = x,
                    y = y,
                    texture = texture,
                    overlay = false
                )
            }

        for (layerId in overlayIds) {
            stats.terrainChecked++

            world.getOverlayTile(layerId, x, y)
                ?.let { textureFor(it, animationTime) }
                ?.let { texture ->
                    renderTerrainSprite(
                        x = x,
                        y = y,
                        texture = texture,
                        overlay = true
                    )
                }
        }
    }

    private fun renderObject(
        placed: PlacedObject,
        visual: ResolvedObjectVisual?,
        preview: PlacementPreview?,
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
            visual = visual
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
        animationTime: Float,
        resolvedObjectVisualFor: (
            (PlacedObject, Float) -> ResolvedObjectVisual?
        )? = null,
        resolvedEntityVisualFor: (
            (WorldEntity, Float) -> ResolvedEntityVisual?
        )? = null
    ) {
        batch.projectionMatrix = camera.combined

        beginWorldBatch()

        for (item in normalRenderPlan) {
            if (item is WorldObjectPrimitive) {
                renderObject(
                    placed = item.placedObject,
                    visual = resolvedObjectVisualFor?.invoke(
                        item.placedObject,
                        animationTime
                    ) ?: objectVisualFor(item.placedObject)?.let {
                        ResolvedObjectVisual(it, animationTime, null)
                    },
                    preview = null,
                    recordStats = false
                )
            } else if (item is WorldEntityPrimitive) {
                renderEntity(
                    entity = item.worldEntity,
                    visual = resolvedEntityVisualFor?.invoke(
                        item.worldEntity,
                        animationTime
                    ) ?: entityVisualFor(item.worldEntity)?.let {
                        ResolvedEntityVisual(it, animationTime, null)
                    },
                    recordStats = false
                )
            }
        }

        for (preview in previews) {
            renderObject(
                placed = preview.placedObject,
                visual = resolvedObjectVisualFor?.invoke(
                    preview.placedObject,
                    animationTime
                ) ?: objectVisualFor(preview.placedObject)?.let {
                    ResolvedObjectVisual(it, animationTime, null)
                },
                preview = preview,
                recordStats = false
            )
        }

        batch.end()
    }

    private fun renderTerrainSprite(
        x: Int,
        y: Int,
        texture: TextureRegion,
        overlay: Boolean
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
        if (overlay) {
            stats.overlayTerrainDrawn++
        } else {
            stats.groundTerrainDrawn++
        }
    }

    private fun elapsedMs(startNanos: Long): Double {
        return (System.nanoTime() - startNanos) / 1_000_000.0
    }

    private fun beginWorldBatch() {
        if (lighting.enabled) {
            batch.shader = lightingShader.program
            batch.begin()
            lightingShader.apply()
        } else {
            batch.shader = null
            batch.begin()
        }
    }

    fun dispose() {
        try {
            lightingShader.dispose()
        } finally {
            batch.dispose()
        }
    }
}
