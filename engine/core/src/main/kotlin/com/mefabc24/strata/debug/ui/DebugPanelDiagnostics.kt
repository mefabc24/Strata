package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Gdx
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.RenderOrderDebugMode
import com.mefabc24.strata.debug.hasActiveVisuals
import com.mefabc24.strata.debug.resolvePickingDiagnostics
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.debug.inspector.formatEntityPosition
import com.mefabc24.strata.debug.inspector.formatFootprint
import com.mefabc24.strata.debug.inspector.formatTilePosition
import com.mefabc24.strata.debug.inspector.formatTilePositions
import com.mefabc24.strata.debug.tools.DebugPathfindingTool
import com.mefabc24.strata.debug.tools.DebugPaintTarget
import com.mefabc24.strata.debug.tools.DebugTerrainPainter
import com.mefabc24.strata.debug.tools.DebugToolController
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.debug.cullingDebugCounts
import com.mefabc24.strata.render.`object`.ObjectEntry
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.render.entity.EntityEntry
import com.mefabc24.strata.terrain.TerrainEntry
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.ui.StrataSelectionGroup
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import java.util.Locale

/** Formats and synchronizes diagnostic data shown by DebugPanel. */
internal class DebugPanelDiagnostics(
    private val settings: DebugSettings,
    private val tools: DebugToolController,
    private val painter: DebugTerrainPainter,
    private val inspector: DebugInspector,
    private val pathfinding: DebugPathfindingTool,
    private val placement: PlacementController?,
    private val world: World,
    private val view: IsoWorldView,
    private val terrainFor: (Tile) -> TerrainId,
    private val objects: ObjectRegistry,
    private val toolRailState: DebugToolRailState,
    private val buildSelection: StrataSelectionGroup<ObjectEntry>?,
    private val terrainSelection: StrataSelectionGroup<TerrainEntry>?,
    private val spawnSelection: StrataSelectionGroup<EntityEntry>?
) {
    fun sync(
        inspectorRows: DebugDiagnosticTable?,
        pickingRows: DebugDiagnosticTable?,
        cameraRows: DebugDiagnosticTable?,
        cullingRows: DebugDiagnosticTable?,
        toolStatusRows: Map<DebugToolMode, DebugDiagnosticTable>
    ) {
        if (settings.ui.toolRail.enabled) {
            inspectorRows?.show(formatInspection())
            syncContextFooter(toolStatusRows)
        }
        if (settings.ui.settingsWindow.enabled) {
            pickingRows?.show(
                if (settings.visuals.picking.hasActiveVisuals && view.pickingAvailable) {
                    formatPicking()
                } else {
                    emptyList()
                }
            )
            cameraRows?.show(if (settings.visuals.camera.hasActiveVisuals) formatCamera() else emptyList())
            cullingRows?.show(if (settings.visuals.culling.hasActiveVisuals) formatCulling() else emptyList())
        }
    }

    private fun formatPicking(): List<DebugDiagnosticRow> {
        val x = Gdx.input.x.toFloat()
        val y = Gdx.input.y.toFloat()
        val snapshot = settings.worldState.picking
        val diagnostics = resolvePickingDiagnostics(
            selection = settings.worldState.pickingSelection,
            hoverTarget = snapshot?.picked,
            cursorScreen = com.badlogic.gdx.math.Vector2(x, y),
            cursorWorld = view.screenToWorld(x, y),
            cursorGrid = view.pickGrid(x, y),
            cursorTile = view.pickTile(x, y),
            world = world,
            tileCenterWorld = view::tileCenterWorld,
            objectOriginWorld = view::objectOriginWorld,
            entityWorld = view::entityWorld,
            worldToScreen = view::worldToScreen
        )
        val entityNames = diagnostics.entities.map { it.entity::class.displayName() }
        return diagnosticRows(
            "Mode" to diagnostics.mode.name.toDisplayName(),
            "Screen" to "${diagnostics.screen.x.format()}, ${diagnostics.screen.y.format()}",
            "World" to "${diagnostics.world.x.format()}, ${diagnostics.world.y.format()}",
            "Grid" to formatTilePosition(diagnostics.grid),
            "Tile" to debugDiagnosticValue(
                diagnostics.tile?.let(::formatTilePosition)
            ),
            "Object" to display(diagnostics.placedObject?.placeable?.javaClass?.simpleName),
            "Entity" to display(entityNames.takeIf { it.isNotEmpty() }),
            "Alpha" to alphaText(diagnostics.alphaAccepted),
            "Bounds" to display(diagnostics.bounds)
        )
    }

    private fun formatCamera(): List<DebugDiagnosticRow> {
        val camera = view.cameraDebugSnapshot()
        return diagnosticRows(
            "Position" to "${camera.x.format()}, ${camera.y.format()}",
            "Zoom" to camera.zoom.format(),
            "Viewport" to "${camera.viewportWidth.format()} x ${camera.viewportHeight.format()}",
            "Camera view" to camera.visibleArea.toString(),
            "World" to camera.worldBounds.toString(),
            "Clamp" to camera.clampBounds.toString()
        )
    }

    private fun formatCulling(): List<DebugDiagnosticRow> {
        val counts = cullingDebugCounts(view.renderDebugSnapshot)
        return diagnosticRows(
            "Objects" to "${counts.objectsDrawn} drawn, ${counts.objectsCulled} culled",
            "Entities" to "${counts.entitiesDrawn} drawn, ${counts.entitiesCulled} culled",
            "Object color" to "Cyan; dimmed when culled",
            "Entity color" to "Orange; dimmed when culled",
            "Area" to "Renderer bounds-overlap check"
        )
    }

    private fun syncContextFooter(
        toolStatusRows: Map<DebugToolMode, DebugDiagnosticTable>
    ) {
        val displayedMode = toolRailState.settingsMode ?: return
        val status = debugContextStatus(
            DebugContextInputs(
                mode = displayedMode,
                buildObject = buildSelection?.selected?.displayName(),
                placementAvailable = placement != null,
                placementDiagnostic = placement?.currentDiagnostic,
                buildDragging = tools.buildDragging,
                buildPreviewCount = tools.buildPreviewCount,
                paintTerrain = terrainSelection?.selected?.type?.toString()?.toDisplayName(),
                paintLayer = when (painter.target) {
                    DebugPaintTarget.GROUND -> "Ground"
                    DebugPaintTarget.OVERLAY -> "Overlay: ${painter.selectedOverlayLayerId?.toDisplayName()}"
                },
                spawnEntity = spawnSelection?.selected?.type?.displayName(),
                inspection = inspectionSummary(),
                pathWaypoints = pathfinding.waypoints,
                pathEntity = pathfinding.selectedEntity?.entity?.let { it::class.displayName() },
                pathEntityWaiting = settings.worldState.pathfindingEntityWaiting,
                pathResult = pathfinding.result,
                movePreview = settings.worldState.movePreview
            )
        )
        toolStatusRows[displayedMode]?.show(status?.rows.orEmpty())
    }

    private fun inspectionSummary(): String? = when (val selected = inspector.selection) {
        is DebugInspection.EntityTarget -> "${selected.entity.entity::class.displayName()} (Entity)"
        is DebugInspection.ObjectTarget ->
            "${selected.placedObject.placeable::class.displayName()} (Object)"
        is DebugInspection.TileTarget -> "${formatTilePosition(selected.position)} (Tile)"
        null -> null
    }

    private fun renderInspectionRows(
        placedObject: com.mefabc24.strata.world.PlacedObject? = null,
        entity: com.mefabc24.strata.world.WorldEntity? = null
    ): List<Pair<String, String>> {
        val item = view.renderDebugSnapshot?.items?.firstOrNull { snapshot ->
            when {
                placedObject != null -> snapshot.placedObject === placedObject
                entity != null -> snapshot.entity === entity
                else -> false
            }
        } ?: return emptyList()
        val sort = item.sort
        return listOf(
            "Render index" to when (settings.visuals.renderOrder.mode) {
                RenderOrderDebugMode.CALCULATED -> item.index.toString()
                RenderOrderDebugMode.ACTUAL -> item.actualIndex?.toString() ?: "not drawn"
            },
            "Drawn" to item.drawn.toString(),
            "Render priority" to (sort?.renderPriority?.toString() ?: "unavailable"),
            "Sort volume" to if (sort != null) {
                "[${sort.minX.format()}, ${sort.maxX.format()}] x " +
                    "[${sort.minY.format()}, ${sort.maxY.format()}]"
            } else {
                "unavailable"
            },
            "Front Y" to (sort?.projectedFrontY?.format() ?: "unavailable"),
            "Render bounds" to display(item.bounds)
        )
    }

    private fun formatInspection(): List<DebugDiagnosticRow> = when (val selected = inspector.selection) {
        null -> diagnosticRows("Status" to "Click an entity, object, or tile")
        is DebugInspection.EntityTarget -> {
            val entity = selected.entity
            val visual = view.resolvedEntityVisual(entity)
            diagnosticRows(*(listOf(
                "Entity" to entity.entity::class.displayName(),
                "Position" to formatEntityPosition(entity.position),
                "Tile" to formatTilePosition(entity.currentTile),
                "Direction" to entity.direction.toString(),
                "Frozen" to settings.isEntityFrozen(entity).toString(),
                "Animation frozen" to (
                    settings.isEntityFrozen(entity) && settings.tools.inspect.freezeEntityAnimation
                    ).toString(),
                "Moving" to entity.isMoving.toString(),
                "Waypoints" to entity.remainingWaypoints.size.toString(),
                "Path" to formatTilePositions(entity.remainingPath),
                "Animation" to animationText(
                    visual?.state?.toString(), visual?.stateTime, visual?.visual?.sprite
                ),
                "Sprite bounds" to display(view.entitySpriteBounds(entity))
            ) + renderInspectionRows(entity = entity)).toTypedArray())
        }
        is DebugInspection.ObjectTarget -> {
            val placed = selected.placedObject
            val visual = objects.resolve(placed, view.animationTime)
            diagnosticRows(*(listOf(
                "Object" to placed.placeable::class.displayName(),
                "Origin" to "(${placed.x}, ${placed.y})",
                "Footprint" to formatFootprint(placed.placeable.footprint),
                "Occupied" to formatTilePositions(placed.occupiedTiles()),
                "Animation" to animationText(
                    visual?.state?.toString(), visual?.stateTime, visual?.visual?.sprite
                ),
                "Sprite Bounds" to display(view.objectSpriteBounds(placed))
            ) + renderInspectionRows(placedObject = placed)).toTypedArray())
        }
        is DebugInspection.TileTarget -> {
            val position = selected.position
            val tile = world.getTile(position)
            val overlays = world.overlayLayerIds.mapNotNull { id ->
                world.getOverlayTile(id, position.x, position.y)?.let { id to terrainFor(it) }
            }
            val tileEntities = world.getEntities().filter { it.currentTile == position }
            tileInspectionRows(
                position = position,
                terrain = tile?.let(terrainFor),
                overlays = overlays,
                objectName = world.getObjectAt(position)?.placeable?.javaClass?.simpleName,
                entityNames = tileEntities.map { it.entity::class.simpleName }
            )
        }
    }

    private fun animationText(
        state: String?,
        stateTime: Float?,
        sprite: com.mefabc24.strata.render.sprite.SpriteFrames?
    ): String {
        if (sprite == null || stateTime == null) return "unavailable"
        return "${state ?: "default"}, t=${stateTime.format()}, " +
            "frame ${sprite.frameIndexAt(stateTime) + 1}/${sprite.frameCount}, " +
            (sprite.frameDuration?.let { "${it.format()} s" } ?: "static")
    }

    private fun alphaText(value: Boolean?): String = when (value) {
        true -> "accepted"
        false -> "rejected"
        null -> "unavailable"
    }
}

internal fun diagnosticRows(
    vararg rows: Pair<String, String>
): List<DebugDiagnosticRow> = rows.map { (key, value) -> DebugDiagnosticRow(key, value) }

internal fun tileInspectionRows(
    position: TilePosition,
    terrain: Any?,
    overlays: List<*>,
    objectName: String?,
    entityNames: List<String?>
): List<DebugDiagnosticRow> = diagnosticRows(
    "Tile" to formatTilePosition(position),
    "Terrain" to debugDiagnosticValue(terrain),
    "Overlays" to debugDiagnosticValue(overlays.takeIf { it.isNotEmpty() }),
    "Object" to debugDiagnosticValue(objectName),
    "Entities" to debugDiagnosticValue(entityNames.takeIf { it.isNotEmpty() })
)

private fun display(value: Any?): String = debugDiagnosticValue(value)
private fun Float.format() = String.format(Locale.ROOT, "%.2f", this)
private fun Double.format() = String.format(Locale.ROOT, "%.2f", this)
private fun String.toDisplayName() = replace('_', ' ').replace('-', ' ')
    .lowercase().replaceFirstChar(Char::titlecase)
private fun kotlin.reflect.KClass<*>.displayName() = simpleName?.toDisplayName() ?: toString()
private fun ObjectEntry.displayName() = type.displayName()
