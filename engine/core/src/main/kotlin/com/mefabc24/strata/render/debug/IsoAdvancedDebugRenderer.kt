package com.mefabc24.strata.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.debug.DebugRenderOrderSettings
import com.mefabc24.strata.debug.DebugVisualizationFilterContext
import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.debug.RenderPriorityFocusMode
import com.mefabc24.strata.debug.RenderOrderDebugMode
import com.mefabc24.strata.debug.TerrainHeatmapSteps
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.debug.tools.DebugBrushPreviewKind
import com.mefabc24.strata.iso.CameraDebugSnapshot
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.render.RenderDebugSnapshot
import com.mefabc24.strata.render.RenderItemDebugSnapshot
import com.mefabc24.strata.world.TilePosition
import kotlin.math.round

internal data class RenderOrderDebugLayers(
    val objectEntityLabels: Boolean,
    val priorityLabels: Boolean,
    val priorityColors: Boolean,
    val priorityFocus: Boolean,
    val sortVolumes: Boolean,
    val sortAnchors: Boolean,
    val projectedSortPositions: Boolean,
    val terrainIndices: Boolean,
    val terrainHeatmap: Boolean,
    val terrainHeatmapGrid: Boolean
)

internal fun renderOrderDebugLayers(
    settings: DebugRenderOrderSettings
): RenderOrderDebugLayers = RenderOrderDebugLayers(
    objectEntityLabels = settings.enabled && settings.showLabels,
    priorityLabels = settings.enabled && settings.showPriorityLabels,
    priorityColors = settings.enabled && settings.colorByPriority,
    priorityFocus = settings.enabled &&
        settings.priorityFocusMode != RenderPriorityFocusMode.OFF,
    sortVolumes = settings.enabled && settings.showSortVolumes,
    sortAnchors = settings.enabled && settings.showSortAnchors,
    projectedSortPositions = settings.enabled && settings.showProjectedSortPositions,
    terrainIndices = settings.enabled && settings.showTerrainIndices,
    terrainHeatmap = settings.enabled && settings.showTerrainHeatmap,
    terrainHeatmapGrid = settings.enabled && settings.showTerrainHeatmap
)

internal fun renderPriorityFocusAllows(
    settings: DebugRenderOrderSettings,
    item: RenderItemDebugSnapshot
): Boolean = settings.priorityFocusMode != RenderPriorityFocusMode.ISOLATE ||
    item.sort?.renderPriority == settings.selectedPriority

internal fun renderPriorityColor(
    priority: Int,
    alpha: Float,
    result: Color = Color()
): Color {
    val index = Math.floorMod(priority, PRIORITY_PALETTE.size)
    return result.set(PRIORITY_PALETTE[index]).also { it.a = alpha }
}

private val PRIORITY_PALETTE = arrayOf(
    Color(0.2f, 0.8f, 1f, 1f),
    Color(1f, 0.45f, 0.25f, 1f),
    Color(0.45f, 1f, 0.35f, 1f),
    Color(0.75f, 0.4f, 1f, 1f),
    Color(1f, 0.85f, 0.2f, 1f),
    Color(0.2f, 1f, 0.8f, 1f),
    Color(1f, 0.3f, 0.7f, 1f),
    Color(0.55f, 0.7f, 1f, 1f)
)

internal data class RenderOrderDebugMetadata(
    val index: Int,
    val terrainRank: Int?,
    val terrainCount: Int
)

internal fun renderOrderDebugMetadata(
    item: RenderItemDebugSnapshot,
    snapshot: RenderDebugSnapshot,
    mode: RenderOrderDebugMode
): RenderOrderDebugMetadata? = when (mode) {
    RenderOrderDebugMode.CALCULATED -> RenderOrderDebugMetadata(
        index = item.index,
        terrainRank = item.terrain?.rank,
        terrainCount = snapshot.terrainCount
    )
    RenderOrderDebugMode.ACTUAL -> item.actualIndex?.let { actualIndex ->
        RenderOrderDebugMetadata(
            index = actualIndex,
            terrainRank = item.terrain?.actualRank,
            terrainCount = snapshot.actualTerrainCount
        )
    }
}

internal fun terrainHeatmapProgress(
    rank: Int,
    terrainCount: Int,
    steps: TerrainHeatmapSteps = TerrainHeatmapSteps.PER_TILE
): Float {
    val progress = if (terrainCount <= 1) 0f else
        (rank.toFloat() / (terrainCount - 1).toFloat()).coerceIn(0f, 1f)
    val levels = steps.colorLevelCount ?: return progress
    if (progress <= 0f || progress >= 1f) return progress
    return round(progress * (levels - 1)) / (levels - 1)
}

internal fun terrainHeatmapColor(
    start: Color,
    end: Color,
    rank: Int,
    terrainCount: Int,
    steps: TerrainHeatmapSteps = TerrainHeatmapSteps.PER_TILE,
    result: Color = Color()
): Color = result.set(start).lerp(
    end,
    terrainHeatmapProgress(rank, terrainCount, steps)
)

internal inline fun <T> BitmapFont.withRelativeScale(
    relativeScale: Float,
    block: () -> T
): T {
    val previousScaleX = data.scaleX
    val previousScaleY = data.scaleY
    data.setScale(previousScaleX * relativeScale, previousScaleY * relativeScale)
    return try {
        block()
    } finally {
        data.setScale(previousScaleX, previousScaleY)
    }
}

/** Draws optional diagnostics sourced from real picker and renderer state. */
internal class IsoAdvancedDebugRenderer(
    private val projection: IsoProjection,
    private val settings: DebugSettings,
    private val state: DebugWorldState,
    private val filterContext: DebugVisualizationFilterContext,
    private val shapes: ShapeRenderer = ShapeRenderer(),
    private val labels: SpriteBatch = SpriteBatch(),
    private val font: BitmapFont = BitmapFont()
) {
    private val labelLayout = GlyphLayout()
    private val heatmapColor = Color()
    private val priorityColor = Color()
    private val sortPointA = Vector2()
    private val sortPointB = Vector2()
    private val sortPointC = Vector2()
    private val sortPointD = Vector2()

    fun render(
        camera: OrthographicCamera,
        cameraSnapshot: CameraDebugSnapshot,
        renderSnapshot: RenderDebugSnapshot?
    ) {
        val inspection = state.inspection
        val inspectionVisuals = inspectionVisuals(
            inspection,
            state.inspectionHighlightVisible,
            settings.inspect
        )
        val pickingVisuals = pickingVisualTargets(
            settings.picking.enabled,
            state.picking?.picked,
            state.pickingSelection.lockedTarget
        )
        val path = state.pathfinding
        val drawPath = settings.pathfinding.enabled &&
            (state.pathfindingWaypoints.isNotEmpty() || path != null)
        val renderOrderLayers = renderOrderDebugLayers(settings.renderOrder)
        val drawShapes = inspectionVisuals.isNotEmpty() || drawPath ||
            settings.picking.enabled || settings.culling.enabled || settings.camera.enabled ||
            state.movePreview != null || state.brushPreview != null ||
            renderOrderLayers.terrainHeatmap || renderOrderLayers.priorityColors ||
            renderOrderLayers.priorityFocus || renderOrderLayers.sortVolumes ||
            renderOrderLayers.sortAnchors || renderOrderLayers.projectedSortPositions
        val drawLabels = renderOrderLayers.objectEntityLabels ||
            renderOrderLayers.priorityLabels || renderOrderLayers.terrainIndices
        if (!drawShapes && !drawLabels) return

        if (drawShapes) {
            shapes.projectionMatrix = camera.combined
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            drawFills(
                inspection,
                inspectionVisuals,
                if (settings.pathfinding.enabled) path else null,
                camera,
                cameraSnapshot,
                renderSnapshot,
                pickingVisuals
            )
            drawLines(
                inspection,
                inspectionVisuals,
                cameraSnapshot,
                renderSnapshot,
                pickingVisuals
            )
            Gdx.gl.glLineWidth(1f)
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }

        if (drawLabels && renderSnapshot != null) {
            labels.projectionMatrix = camera.combined
            labels.begin()
            if (renderOrderLayers.objectEntityLabels || renderOrderLayers.priorityLabels) {
                val highlightColor = settings.renderOrder.priorityHighlightColor
                for (item in renderSnapshot.items) {
                    if (!item.drawn || item.terrain != null) continue
                    if (!filterContext.matches(settings.visualizationFilter, item)) continue
                    if (!renderPriorityFocusAllows(settings.renderOrder, item)) continue
                    val bounds = item.bounds ?: continue
                    val text = buildRenderLabel(
                        item,
                        renderSnapshot,
                        renderOrderLayers
                    ) ?: continue
                    font.color = if (
                        settings.renderOrder.priorityFocusMode ==
                        RenderPriorityFocusMode.HIGHLIGHT &&
                        item.sort?.renderPriority == settings.renderOrder.selectedPriority
                    ) {
                        highlightColor
                    } else {
                        Color.WHITE
                    }
                    font.draw(
                        labels,
                        text,
                        bounds.x,
                        bounds.y + bounds.height + 12f * camera.zoom
                    )
                }
            }
            if (renderOrderLayers.terrainIndices) {
                font.withRelativeScale(TERRAIN_INDEX_FONT_SCALE) {
                    for (item in renderSnapshot.items) {
                        if (!item.drawn) continue
                        if (!filterContext.matches(settings.visualizationFilter, item)) continue
                        val terrain = item.terrain ?: continue
                        val metadata = renderOrderDebugMetadata(
                            item,
                            renderSnapshot,
                            settings.renderOrder.mode
                        ) ?: continue
                        val center = projection.tileToWorld(
                            terrain.position.x + 0.5f,
                            terrain.position.y + 0.5f
                        )
                        labelLayout.setText(font, metadata.index.toString())
                        font.draw(
                            labels,
                            labelLayout,
                            center.x - labelLayout.width / 2f,
                            center.y + labelLayout.height / 2f
                        )
                    }
                }
            }
            labels.end()
        }
    }

    private fun buildRenderLabel(
        item: RenderItemDebugSnapshot,
        snapshot: RenderDebugSnapshot,
        layers: RenderOrderDebugLayers
    ): String? {
        val sort = item.sort ?: return null
        val name = item.placedObject?.placeable?.javaClass?.simpleName
            ?: item.entity?.entity?.javaClass?.simpleName
            ?: return null
        val order = if (layers.objectEntityLabels) {
            renderOrderDebugMetadata(
                item,
                snapshot,
                settings.renderOrder.mode
            )?.let { "#${it.index}" }
        } else {
            null
        }
        val priority = if (layers.priorityLabels) "P:${sort.renderPriority}" else null
        return listOfNotNull(name.takeIf { order != null }, order, priority)
            .joinToString("  ")
            .takeIf(String::isNotEmpty)
    }

    private fun drawFills(
        inspection: DebugInspection?,
        inspectionVisuals: Set<DebugInspectionVisual>,
        path: com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult?,
        camera: OrthographicCamera,
        cameraSnapshot: CameraDebugSnapshot,
        renderSnapshot: RenderDebugSnapshot?,
        pickingVisuals: DebugPickingVisualTargets
    ) {
        val explored = path?.explored.orEmpty()
        val shouldDrawExplored = settings.pathfinding.showExploredNodes && explored.isNotEmpty()
        val pathWaypoints = state.pathfindingWaypoints.takeIf {
            settings.pathfinding.enabled
        }.orEmpty()
        val drawCullingArea = settings.culling.enabled &&
            settings.culling.showVisibleArea && renderSnapshot != null
        val drawCameraArea = settings.camera.enabled && settings.camera.showVisibleArea
        val drawPickingTiles = pickingVisuals.hover is PickedTarget.Tile ||
            pickingVisuals.locked is PickedTarget.Tile
        val movePreview = state.movePreview?.takeIf { it.visible }
        val brushPreview = state.brushPreview
        val drawTerrainHeatmap = renderOrderDebugLayers(settings.renderOrder).terrainHeatmap &&
            renderSnapshot != null
        val drawPriorityColors = renderOrderDebugLayers(settings.renderOrder).priorityColors &&
            renderSnapshot != null
        if (!shouldDrawExplored && inspectionVisuals.isEmpty() && pathWaypoints.isEmpty() &&
            !drawCullingArea && !drawCameraArea && !drawPickingTiles && movePreview == null &&
            brushPreview == null && !drawTerrainHeatmap && !drawPriorityColors
        ) return
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        if (drawPriorityColors) {
            val snapshot = requireNotNull(renderSnapshot)
            snapshot.items.forEach { item ->
                if (!item.drawn || item.terrain != null) return@forEach
                if (!filterContext.matches(settings.visualizationFilter, item)) return@forEach
                if (!renderPriorityFocusAllows(settings.renderOrder, item)) return@forEach
                val bounds = item.bounds ?: return@forEach
                shapes.color = renderPriorityColor(
                    priority = item.sort?.renderPriority ?: return@forEach,
                    alpha = settings.renderOrder.priorityColorAlpha,
                    result = priorityColor
                )
                shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height)
            }
        }
        if (drawTerrainHeatmap) {
            val snapshot = requireNotNull(renderSnapshot)
            val startColor = settings.renderOrder.terrainHeatmapStartColor
            val endColor = settings.renderOrder.terrainHeatmapEndColor
            snapshot.items.forEach { item ->
                val terrain = item.terrain ?: return@forEach
                if (!item.drawn) return@forEach
                if (!filterContext.matches(settings.visualizationFilter, item)) return@forEach
                val metadata = renderOrderDebugMetadata(
                    item,
                    snapshot,
                    settings.renderOrder.mode
                ) ?: return@forEach
                val rank = metadata.terrainRank ?: return@forEach
                shapes.color = terrainHeatmapColor(
                    start = startColor,
                    end = endColor,
                    rank = rank,
                    terrainCount = metadata.terrainCount,
                    steps = settings.renderOrder.terrainHeatmapSteps,
                    result = heatmapColor
                )
                drawTileFill(terrain.position)
            }
        }
        movePreview?.let { preview ->
            shapes.color = if (preview.valid) MOVE_VALID_FILL else MOVE_INVALID_FILL
            preview.occupiedTiles.forEach(::drawTileFill)
        }
        brushPreview?.let { preview ->
            shapes.color = when (preview.kind) {
                DebugBrushPreviewKind.PAINT -> BRUSH_PAINT_FILL
                DebugBrushPreviewKind.DELETE -> BRUSH_DELETE_FILL
            }
            preview.tiles.forEach(::drawTileFill)
        }
        if (shouldDrawExplored) {
            shapes.color = Color(0.2f, 0.55f, 1f, 0.16f)
            explored.forEach(::drawTileFill)
        }
        shapes.color = Color(1f, 0.75f, 0.15f, 0.22f)
        pathWaypoints.forEach(::drawTileFill)
        when {
            inspection is DebugInspection.EntityTarget &&
                DebugInspectionVisual.ENTITY_TILE in inspectionVisuals -> {
                shapes.color = settings.entities.currentTileFillColor
                    ?: settings.entities.currentTileColor.cpy().apply { a = 0.16f }
                drawTileFill(inspection.entity.currentTile)
            }
            inspection is DebugInspection.ObjectTarget &&
                DebugInspectionVisual.OBJECT_FOOTPRINT in inspectionVisuals -> {
                shapes.color = settings.objects.occupiedTileFillColor
                    ?: settings.objects.occupiedTileColor.cpy().apply { a = 0.18f }
                inspection.placedObject.occupiedTiles().forEach(::drawTileFill)
            }
            inspection is DebugInspection.TileTarget &&
                DebugInspectionVisual.TILE in inspectionVisuals -> {
                shapes.color = Color(1f, 0.75f, 0.15f, 0.22f)
                drawTileFill(inspection.position)
            }
        }
        if (inspection is DebugInspection.EntityTarget &&
            DebugInspectionVisual.ENTITY_POSITION in inspectionVisuals
        ) {
            shapes.color = settings.entities.positionColor
            val point = projection.tileToWorld(
                inspection.entity.position.x,
                inspection.entity.position.y
            )
            shapes.circle(point.x, point.y, 3f * camera.zoom, 12)
        }
        (pickingVisuals.hover as? PickedTarget.Tile)?.let { target ->
            shapes.color = PICKING_HOVER_FILL
            drawTileFill(target.position)
        }
        (pickingVisuals.locked as? PickedTarget.Tile)?.let { target ->
            shapes.color = PICKING_LOCKED_FILL
            drawTileFill(target.position)
        }
        if (drawCullingArea) {
            shapes.color = settings.culling.visibleAreaColor
            drawPixelRectOutline(
                requireNotNull(renderSnapshot).visibleArea,
                camera.zoom,
                pixelThickness = 8f
            )
        }
        if (drawCameraArea) {
            shapes.color = Color.CYAN
            drawPixelRectOutline(
                cameraSnapshot.visibleArea,
                camera.zoom,
                pixelThickness = 4f
            )
        }
        shapes.end()
    }

    private fun drawInspectionLines(
        inspection: DebugInspection?,
        visuals: Set<DebugInspectionVisual>,
        camera: CameraDebugSnapshot,
        renderSnapshot: RenderDebugSnapshot?
    ) {
        when (inspection) {
            is DebugInspection.TileTarget -> {
                if (DebugInspectionVisual.TILE in visuals) {
                    shapes.color = Color(1f, 0.75f, 0.15f, 1f)
                    drawTileOutline(inspection.position)
                }
            }
            is DebugInspection.ObjectTarget -> {
                val placed = inspection.placedObject
                if (DebugInspectionVisual.OBJECT_FOOTPRINT in visuals) {
                    shapes.color = settings.objects.occupiedTileColor
                    placed.occupiedTiles().forEach(::drawTileOutline)
                }
                if (DebugInspectionVisual.OBJECT_ORIGIN in visuals) {
                    shapes.color = settings.objects.originTileColor
                    drawTileOutline(objectDebugOrigin(placed))
                }
                if (DebugInspectionVisual.OBJECT_SPRITE_BOUNDS in visuals) {
                    shapes.color = settings.objects.spriteBoundsColor
                    renderSnapshot?.items
                        ?.firstOrNull { it.placedObject === placed }
                        ?.bounds
                        ?.let(::drawRect)
                }
            }
            is DebugInspection.EntityTarget -> {
                val entity = inspection.entity
                if (DebugInspectionVisual.ENTITY_TILE in visuals) {
                    shapes.color = settings.entities.currentTileColor
                    drawTileOutline(entity.currentTile)
                }
                if (DebugInspectionVisual.ENTITY_PATH in visuals) {
                    shapes.color = settings.entities.pathColor
                    val markerRadius = 2f * camera.zoom
                    forEachEntityDebugPathSegment(entity) { from, to ->
                        val fromWorld = projection.tileToWorld(from.x, from.y)
                        val toWorld = projection.tileToWorld(to.x, to.y)
                        shapes.line(fromWorld.x, fromWorld.y, toWorld.x, toWorld.y)
                        shapes.circle(toWorld.x, toWorld.y, markerRadius, 12)
                    }
                }
                if (DebugInspectionVisual.ENTITY_DIRECTION in visuals) {
                    shapes.color = settings.entities.directionColor
                    val target = entityDebugDirectionTarget(entity)
                    val start = projection.tileToWorld(entity.position.x, entity.position.y)
                    val end = projection.tileToWorld(target.x, target.y)
                    shapes.line(start.x, start.y, end.x, end.y)
                }
                if (DebugInspectionVisual.ENTITY_SPRITE_BOUNDS in visuals) {
                    shapes.color = settings.entities.spriteBoundsColor
                    renderSnapshot?.items
                        ?.firstOrNull { it.entity === entity }
                        ?.bounds
                        ?.let(::drawRect)
                }
            }
            null -> Unit
        }
    }

    private fun drawPickingLines(targets: DebugPickingVisualTargets) {
        (targets.hover as? PickedTarget.Tile)?.let { target ->
            shapes.color = PICKING_HOVER_OUTLINE
            drawTileOutline(target.position)
        }
        (targets.locked as? PickedTarget.Tile)?.let { target ->
            shapes.color = PICKING_LOCKED_OUTLINE
            drawTileOutline(target.position)
        }
        if (!settings.picking.showSpriteBounds) return
        targets.hover?.takeUnless { it is PickedTarget.Tile }?.bounds?.let { bounds ->
            shapes.color = PICKING_HOVER_OUTLINE
            drawRect(bounds)
        }
        targets.locked?.takeUnless { it is PickedTarget.Tile }?.bounds?.let { bounds ->
            shapes.color = PICKING_LOCKED_OUTLINE
            drawRect(bounds)
        }
    }

    private fun drawLines(
        inspection: DebugInspection?,
        inspectionVisuals: Set<DebugInspectionVisual>,
        camera: CameraDebugSnapshot,
        renderSnapshot: RenderDebugSnapshot?,
        pickingVisuals: DebugPickingVisualTargets
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Line)
        val renderOrderLayers = renderOrderDebugLayers(settings.renderOrder)
        if (renderOrderLayers.terrainHeatmapGrid && renderSnapshot != null) {
            Gdx.gl.glLineWidth(1f)
            shapes.color = TERRAIN_HEATMAP_GRID_COLOR
            renderSnapshot.items.forEach { item ->
                val terrain = item.terrain ?: return@forEach
                if (!item.drawn) return@forEach
                if (!filterContext.matches(settings.visualizationFilter, item)) return@forEach
                if (renderOrderDebugMetadata(
                        item,
                        renderSnapshot,
                        settings.renderOrder.mode
                    ) != null
                ) {
                    drawTileOutline(terrain.position)
                }
            }
        }
        if (renderSnapshot != null && (
                renderOrderLayers.sortVolumes ||
                    renderOrderLayers.sortAnchors ||
                    renderOrderLayers.projectedSortPositions
                )
        ) {
            Gdx.gl.glLineWidth(
                (settings.renderOrder.sortGeometryLineWidth / camera.zoom)
                    .coerceAtLeast(1f)
            )
            val volumeColor = settings.renderOrder.sortVolumeColor
            val backAnchorColor = settings.renderOrder.sortBackAnchorColor
            val frontAnchorColor = settings.renderOrder.sortFrontAnchorColor
            val projectedPositionColor = settings.renderOrder.projectedSortPositionColor
            renderSnapshot.items.forEach { item ->
                if (item.terrain != null || item.sort == null) return@forEach
                if (!filterContext.matches(settings.visualizationFilter, item)) return@forEach
                if (!renderPriorityFocusAllows(settings.renderOrder, item)) return@forEach
                drawSortGeometry(
                    item = item,
                    layers = renderOrderLayers,
                    cameraZoom = camera.zoom,
                    volumeColor = volumeColor,
                    backAnchorColor = backAnchorColor,
                    frontAnchorColor = frontAnchorColor,
                    projectedPositionColor = projectedPositionColor
                )
            }
        }
        if (renderOrderLayers.priorityFocus && renderSnapshot != null) {
            Gdx.gl.glLineWidth(3f)
            val highlightColor = settings.renderOrder.priorityHighlightColor
            shapes.color = highlightColor
            renderSnapshot.items.forEach { item ->
                if (!item.drawn || item.terrain != null) return@forEach
                if (!filterContext.matches(settings.visualizationFilter, item)) return@forEach
                if (item.sort?.renderPriority != settings.renderOrder.selectedPriority) {
                    return@forEach
                }
                item.bounds?.let(::drawRect)
            }
        }
        Gdx.gl.glLineWidth(2f)
        drawInspectionLines(inspection, inspectionVisuals, camera, renderSnapshot)
        drawPickingLines(pickingVisuals)
        state.movePreview?.takeIf { it.visible }?.let { preview ->
            shapes.color = if (preview.valid) MOVE_VALID_OUTLINE else MOVE_INVALID_OUTLINE
            preview.occupiedTiles.forEach(::drawTileOutline)
            shapes.color = MOVE_ORIGIN_OUTLINE
            drawTileOutline(preview.target)
        }
        state.brushPreview?.let { preview ->
            shapes.color = when (preview.kind) {
                DebugBrushPreviewKind.PAINT -> BRUSH_PAINT_OUTLINE
                DebugBrushPreviewKind.DELETE -> BRUSH_DELETE_OUTLINE
            }
            preview.tiles.forEach(::drawTileOutline)
        }

        val visiblePath = state.pathfinding?.path
        if (settings.pathfinding.enabled && settings.pathfinding.showFinalPath && visiblePath != null) {
            shapes.color = Color(0.2f, 1f, 0.35f, 1f)
            visiblePath.zipWithNext().forEach { (from, to) ->
                val a = projection.tileToWorld(from.x + 0.5f, from.y + 0.5f)
                val b = projection.tileToWorld(to.x + 0.5f, to.y + 0.5f)
                shapes.line(a.x, a.y, b.x, b.y)
            }
        }
        if (settings.pathfinding.enabled && state.pathfindingWaypoints.isNotEmpty()) {
            shapes.color = Color(1f, 0.75f, 0.15f, 1f)
            val markerRadius = 4f * camera.zoom
            state.pathfindingWaypoints.forEach { waypoint ->
                drawTileOutline(waypoint)
                val center = projection.tileToWorld(waypoint.x + 0.5f, waypoint.y + 0.5f)
                shapes.circle(center.x, center.y, markerRadius, 16)
            }
        }

        if (settings.picking.enabled) {
            if (settings.picking.showCursorHit) {
                state.cursorWorld?.let { cursor ->
                    shapes.color = Color.YELLOW
                    val radius = 5f * camera.zoom
                    shapes.line(cursor.x - radius, cursor.y, cursor.x + radius, cursor.y)
                    shapes.line(cursor.x, cursor.y - radius, cursor.x, cursor.y + radius)
                }
            }
        }

        if (settings.culling.enabled && renderSnapshot != null) {
            val objectDrawnColor = settings.culling.objectDrawnColor
            val objectCulledColor = settings.culling.objectCulledColor
            val entityDrawnColor = settings.culling.entityDrawnColor
            val entityCulledColor = settings.culling.entityCulledColor
            renderSnapshot.items.forEach { item ->
                val classification = classifyCullingItem(item) ?: return@forEach
                if (!filterContext.matches(settings.visualizationFilter, item)) return@forEach
                val show = when (classification.kind) {
                    CullingDebugItemKind.OBJECT -> settings.culling.showObjectBounds
                    CullingDebugItemKind.ENTITY -> settings.culling.showEntityBounds
                }
                if (show) item.bounds?.let { bounds ->
                    shapes.color = when (classification.kind) {
                        CullingDebugItemKind.OBJECT -> {
                            if (classification.drawn) objectDrawnColor else objectCulledColor
                        }
                        CullingDebugItemKind.ENTITY -> {
                            if (classification.drawn) entityDrawnColor else entityCulledColor
                        }
                    }
                    drawRect(bounds)
                }
            }
        }

        if (settings.camera.enabled) {
            if (settings.camera.showWorldBounds) {
                shapes.color = Color(0.3f, 1f, 0.3f, 1f)
                drawRect(camera.worldBounds)
            }
            if (settings.camera.showClampBounds) {
                shapes.color = Color(1f, 0.4f, 0.2f, 1f)
                drawRect(camera.clampBounds)
            }
        }
        shapes.end()
    }

    private fun drawSortGeometry(
        item: RenderItemDebugSnapshot,
        layers: RenderOrderDebugLayers,
        cameraZoom: Float,
        volumeColor: Color,
        backAnchorColor: Color,
        frontAnchorColor: Color,
        projectedPositionColor: Color
    ) {
        val sort = requireNotNull(item.sort)
        projection.tileToWorld(sort.minX, sort.minY, sortPointA)
        projection.tileToWorld(sort.maxX, sort.minY, sortPointB)
        projection.tileToWorld(sort.maxX, sort.maxY, sortPointC)
        projection.tileToWorld(sort.minX, sort.maxY, sortPointD)

        if (layers.sortVolumes) {
            shapes.color = volumeColor
            shapes.line(sortPointA, sortPointB)
            shapes.line(sortPointB, sortPointC)
            shapes.line(sortPointC, sortPointD)
            shapes.line(sortPointD, sortPointA)
        }
        if (layers.sortAnchors) {
            val radius = SORT_ANCHOR_RADIUS * cameraZoom
            shapes.color = backAnchorColor
            shapes.circle(sortPointA.x, sortPointA.y, radius, SORT_MARKER_SEGMENTS)
            shapes.color = frontAnchorColor
            shapes.circle(sortPointC.x, sortPointC.y, radius, SORT_MARKER_SEGMENTS)
        }
        if (layers.projectedSortPositions) {
            val radius = PROJECTED_SORT_MARKER_RADIUS * cameraZoom
            val x = sort.projectedFrontX
            val y = sort.projectedFrontY
            shapes.color = projectedPositionColor
            shapes.line(x - radius, y, x + radius, y)
            shapes.line(x, y - radius, x, y + radius)
        }
    }

    private fun drawTileFill(position: TilePosition) {
        val top = projection.tileToWorld(position.x, position.y)
        shapes.drawIsoTileFill(projection, top.x, top.y)
    }

    private fun drawTileOutline(position: TilePosition) {
        val top = projection.tileToWorld(position.x, position.y)
        shapes.drawIsoTileOutline(projection, top.x, top.y)
    }

    private fun drawRect(rectangle: Rectangle) {
        shapes.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height)
    }

    private fun drawPixelRectOutline(
        rectangle: Rectangle,
        cameraZoom: Float,
        pixelThickness: Float
    ) {
        val thickness = (pixelThickness * cameraZoom).coerceAtMost(
            minOf(rectangle.width, rectangle.height) / 2f
        )
        if (thickness <= 0f) return
        val innerHeight = (rectangle.height - thickness * 2f).coerceAtLeast(0f)
        shapes.rect(rectangle.x, rectangle.y, rectangle.width, thickness)
        shapes.rect(
            rectangle.x,
            rectangle.y + rectangle.height - thickness,
            rectangle.width,
            thickness
        )
        shapes.rect(rectangle.x, rectangle.y + thickness, thickness, innerHeight)
        shapes.rect(
            rectangle.x + rectangle.width - thickness,
            rectangle.y + thickness,
            thickness,
            innerHeight
        )
    }

    fun dispose() {
        shapes.dispose()
        labels.dispose()
        font.dispose()
    }

    private companion object {
        const val TERRAIN_INDEX_FONT_SCALE = 0.68f
        const val SORT_ANCHOR_RADIUS = 3f
        const val PROJECTED_SORT_MARKER_RADIUS = 5f
        const val SORT_MARKER_SEGMENTS = 12
        val TERRAIN_HEATMAP_GRID_COLOR = Color(0.72f, 0.72f, 0.72f, 0.3f)
        val PICKING_HOVER_FILL = Color(1f, 0.3f, 0.9f, 0.18f)
        val PICKING_HOVER_OUTLINE = Color(1f, 0.3f, 0.9f, 1f)
        val PICKING_LOCKED_FILL = Color(0.2f, 0.9f, 1f, 0.24f)
        val PICKING_LOCKED_OUTLINE = Color(0.2f, 0.9f, 1f, 1f)
        val MOVE_VALID_FILL = Color(0.25f, 0.85f, 0.35f, 0.22f)
        val MOVE_VALID_OUTLINE = Color(0.25f, 1f, 0.4f, 1f)
        val MOVE_INVALID_FILL = Color(1f, 0.2f, 0.2f, 0.24f)
        val MOVE_INVALID_OUTLINE = Color(1f, 0.25f, 0.25f, 1f)
        val MOVE_ORIGIN_OUTLINE = Color(0.25f, 0.85f, 1f, 1f)
        val BRUSH_PAINT_FILL = Color(0.2f, 0.75f, 1f, 0.2f)
        val BRUSH_PAINT_OUTLINE = Color(0.2f, 0.85f, 1f, 1f)
        val BRUSH_DELETE_FILL = Color(1f, 0.2f, 0.2f, 0.22f)
        val BRUSH_DELETE_OUTLINE = Color(1f, 0.3f, 0.25f, 1f)
    }
}
