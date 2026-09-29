package com.mefabc24.strata.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.iso.CameraDebugSnapshot
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.render.RenderDebugSnapshot
import com.mefabc24.strata.world.TilePosition

/** Draws optional diagnostics sourced from real picker and renderer state. */
internal class IsoAdvancedDebugRenderer(
    private val projection: IsoProjection,
    private val settings: DebugSettings,
    private val state: DebugWorldState,
    private val shapes: ShapeRenderer = ShapeRenderer(),
    private val labels: SpriteBatch = SpriteBatch(),
    private val font: BitmapFont = BitmapFont()
) {
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
            (state.pathStart != null || path != null)
        val drawShapes = inspectionVisuals.isNotEmpty() || drawPath ||
            settings.picking.enabled || settings.culling.enabled || settings.camera.enabled ||
            state.movePreview != null
        val drawLabels = settings.renderOrder.enabled && settings.renderOrder.showLabels
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
            font.color = Color.WHITE
            for (item in renderSnapshot.items) {
                if (!item.drawn) continue
                val bounds = item.bounds ?: continue
                val name = item.placedObject?.placeable?.javaClass?.simpleName
                    ?: item.entity?.entity?.javaClass?.simpleName
                    ?: continue
                font.draw(labels, "$name  #${item.index}", bounds.x, bounds.y + bounds.height + 12f * camera.zoom)
            }
            labels.end()
        }
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
        val pathStart = state.pathStart.takeIf { settings.pathfinding.enabled }
        val drawCullingArea = settings.culling.enabled &&
            settings.culling.showVisibleArea && renderSnapshot != null
        val drawCameraArea = settings.camera.enabled && settings.camera.showVisibleArea
        val drawPickingTiles = pickingVisuals.hover is PickedTarget.Tile ||
            pickingVisuals.locked is PickedTarget.Tile
        val movePreview = state.movePreview?.takeIf { it.visible }
        if (!shouldDrawExplored && inspectionVisuals.isEmpty() && pathStart == null &&
            !drawCullingArea && !drawCameraArea && !drawPickingTiles && movePreview == null
        ) return
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        movePreview?.let { preview ->
            shapes.color = if (preview.valid) MOVE_VALID_FILL else MOVE_INVALID_FILL
            preview.occupiedTiles.forEach(::drawTileFill)
        }
        if (shouldDrawExplored) {
            shapes.color = Color(0.2f, 0.55f, 1f, 0.16f)
            explored.forEach(::drawTileFill)
        }
        shapes.color = Color(1f, 0.75f, 0.15f, 0.22f)
        pathStart?.let(::drawTileFill)
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
        Gdx.gl.glLineWidth(2f)
        drawInspectionLines(inspection, inspectionVisuals, camera, renderSnapshot)
        drawPickingLines(pickingVisuals)
        state.movePreview?.takeIf { it.visible }?.let { preview ->
            shapes.color = if (preview.valid) MOVE_VALID_OUTLINE else MOVE_INVALID_OUTLINE
            preview.occupiedTiles.forEach(::drawTileOutline)
            shapes.color = MOVE_ORIGIN_OUTLINE
            drawTileOutline(preview.target)
        }

        val result = state.pathfinding
        if (settings.pathfinding.enabled && settings.pathfinding.showFinalPath && result?.path != null) {
            shapes.color = Color(0.2f, 1f, 0.35f, 1f)
            result.path.zipWithNext().forEach { (from, to) ->
                val a = projection.tileToWorld(from.x + 0.5f, from.y + 0.5f)
                val b = projection.tileToWorld(to.x + 0.5f, to.y + 0.5f)
                shapes.line(a.x, a.y, b.x, b.y)
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
        val PICKING_HOVER_FILL = Color(1f, 0.3f, 0.9f, 0.18f)
        val PICKING_HOVER_OUTLINE = Color(1f, 0.3f, 0.9f, 1f)
        val PICKING_LOCKED_FILL = Color(0.2f, 0.9f, 1f, 0.24f)
        val PICKING_LOCKED_OUTLINE = Color(0.2f, 0.9f, 1f, 1f)
        val MOVE_VALID_FILL = Color(0.25f, 0.85f, 0.35f, 0.22f)
        val MOVE_VALID_OUTLINE = Color(0.25f, 1f, 0.4f, 1f)
        val MOVE_INVALID_FILL = Color(1f, 0.2f, 0.2f, 0.24f)
        val MOVE_INVALID_OUTLINE = Color(1f, 0.25f, 0.25f, 1f)
        val MOVE_ORIGIN_OUTLINE = Color(0.25f, 0.85f, 1f, 1f)
    }
}
