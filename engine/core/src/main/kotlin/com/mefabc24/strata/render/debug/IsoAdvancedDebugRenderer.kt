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
        val path = state.pathfinding
        val drawPath = settings.pathfinding.enabled &&
            (state.pathStart != null || path != null)
        val drawShapes = inspection != null || drawPath ||
            settings.picking.enabled || settings.culling.enabled || settings.camera.enabled
        val drawLabels = settings.renderOrder.enabled && settings.renderOrder.showLabels
        if (!drawShapes && !drawLabels) return

        if (drawShapes) {
            shapes.projectionMatrix = camera.combined
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            drawFills(inspection, if (settings.pathfinding.enabled) path else null)
            drawLines(inspection, cameraSnapshot, renderSnapshot)
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
        path: com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult?
    ) {
        val explored = path?.explored.orEmpty()
        val shouldDrawExplored = settings.pathfinding.showExploredNodes && explored.isNotEmpty()
        val pathStart = state.pathStart.takeIf { settings.pathfinding.enabled }
        if (!shouldDrawExplored && inspection == null && pathStart == null) return
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        if (shouldDrawExplored) {
            shapes.color = Color(0.2f, 0.55f, 1f, 0.16f)
            explored.forEach(::drawTileFill)
        }
        shapes.color = Color(1f, 0.75f, 0.15f, 0.22f)
        pathStart?.let(::drawTileFill)
        when (inspection) {
            is DebugInspection.EntityTarget -> drawTileFill(inspection.entity.currentTile)
            is DebugInspection.ObjectTarget -> inspection.placedObject.occupiedTiles().forEach(::drawTileFill)
            is DebugInspection.TileTarget -> drawTileFill(inspection.position)
            null -> Unit
        }
        shapes.end()
    }

    private fun drawLines(
        inspection: DebugInspection?,
        camera: CameraDebugSnapshot,
        renderSnapshot: RenderDebugSnapshot?
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Line)
        Gdx.gl.glLineWidth(2f)
        shapes.color = Color(1f, 0.75f, 0.15f, 1f)
        when (inspection) {
            is DebugInspection.EntityTarget -> drawTileOutline(inspection.entity.currentTile)
            is DebugInspection.ObjectTarget -> inspection.placedObject.occupiedTiles().forEach(::drawTileOutline)
            is DebugInspection.TileTarget -> drawTileOutline(inspection.position)
            null -> Unit
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
            if (settings.picking.showSpriteBounds) {
                val picked = state.pickedObject
                val entity = state.pickedEntity
                renderSnapshot?.items?.firstOrNull {
                    it.placedObject === picked || it.entity === entity
                }?.bounds?.let { bounds ->
                    shapes.color = Color(1f, 0.3f, 0.9f, 1f)
                    shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height)
                }
            }
        }

        if (settings.culling.enabled && renderSnapshot != null) {
            if (settings.culling.showVisibleArea) {
                shapes.color = Color(0.25f, 0.8f, 1f, 1f)
                drawRect(renderSnapshot.visibleArea)
            }
            renderSnapshot.items.forEach { item ->
                val show = item.placedObject != null && settings.culling.showObjectBounds ||
                    item.entity != null && settings.culling.showEntityBounds
                if (show) item.bounds?.let { bounds ->
                    shapes.color = if (item.drawn) Color.GREEN else Color.RED
                    drawRect(bounds)
                }
            }
        }

        if (settings.camera.enabled) {
            if (settings.camera.showVisibleArea) {
                shapes.color = Color.CYAN
                drawRect(camera.visibleArea)
            }
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

    fun dispose() {
        shapes.dispose()
        labels.dispose()
        font.dispose()
    }
}
