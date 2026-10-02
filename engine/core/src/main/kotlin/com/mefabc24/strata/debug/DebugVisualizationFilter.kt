package com.mefabc24.strata.debug

import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.render.RenderDebugSnapshot
import com.mefabc24.strata.render.RenderItemDebugSnapshot
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.WorldEntity
import java.util.IdentityHashMap

/** Shared target filter for world-space debug visualizations. */
enum class DebugVisualizationFilter { ALL, SELECTED, HOVERED, VISIBLE }

/**
 * Resolves target identity once per frame for every filtered debug renderer.
 * Reused identity maps keep VISIBLE checks constant-time without frame allocations.
 */
internal class DebugVisualizationFilterContext {
    private var selectedObject: PlacedObject? = null
    private var selectedEntity: WorldEntity? = null
    private var selectedTile: TilePosition? = null
    private var hoveredObject: PlacedObject? = null
    private var hoveredEntity: WorldEntity? = null
    private var hoveredTile: TilePosition? = null
    private val visibleObjects = IdentityHashMap<PlacedObject, Boolean>()
    private val visibleEntities = IdentityHashMap<WorldEntity, Boolean>()

    fun update(
        inspection: DebugInspection?,
        selectedTarget: PickedTarget?,
        hoveredTarget: PickedTarget?,
        renderSnapshot: RenderDebugSnapshot?
    ) {
        selectedObject = (inspection as? DebugInspection.ObjectTarget)?.placedObject
            ?: (selectedTarget as? PickedTarget.Object)?.placedObject
        selectedEntity = (inspection as? DebugInspection.EntityTarget)?.entity
            ?: (selectedTarget as? PickedTarget.Entity)?.worldEntity
        selectedTile = (inspection as? DebugInspection.TileTarget)?.position
            ?: (selectedTarget as? PickedTarget.Tile)?.position
        hoveredObject = (hoveredTarget as? PickedTarget.Object)?.placedObject
        hoveredEntity = (hoveredTarget as? PickedTarget.Entity)?.worldEntity
        hoveredTile = (hoveredTarget as? PickedTarget.Tile)?.position

        visibleObjects.clear()
        visibleEntities.clear()
        renderSnapshot?.items?.forEach { item ->
            if (!item.drawn) return@forEach
            item.placedObject?.let { visibleObjects[it] = true }
            item.entity?.let { visibleEntities[it] = true }
        }
    }

    fun matches(filter: DebugVisualizationFilter, placed: PlacedObject): Boolean =
        when (filter) {
            DebugVisualizationFilter.ALL -> true
            DebugVisualizationFilter.SELECTED -> placed === selectedObject
            DebugVisualizationFilter.HOVERED -> placed === hoveredObject
            DebugVisualizationFilter.VISIBLE -> visibleObjects.containsKey(placed)
        }

    fun matches(filter: DebugVisualizationFilter, entity: WorldEntity): Boolean =
        when (filter) {
            DebugVisualizationFilter.ALL -> true
            DebugVisualizationFilter.SELECTED -> entity === selectedEntity
            DebugVisualizationFilter.HOVERED -> entity === hoveredEntity
            DebugVisualizationFilter.VISIBLE -> visibleEntities.containsKey(entity)
        }

    fun matches(filter: DebugVisualizationFilter, item: RenderItemDebugSnapshot): Boolean =
        when (filter) {
            DebugVisualizationFilter.ALL -> true
            DebugVisualizationFilter.VISIBLE -> item.drawn
            DebugVisualizationFilter.SELECTED -> matchesTarget(
                item,
                selectedObject,
                selectedEntity,
                selectedTile
            )
            DebugVisualizationFilter.HOVERED -> matchesTarget(
                item,
                hoveredObject,
                hoveredEntity,
                hoveredTile
            )
        }

    private fun matchesTarget(
        item: RenderItemDebugSnapshot,
        placed: PlacedObject?,
        entity: WorldEntity?,
        tile: TilePosition?
    ): Boolean = when {
        item.placedObject != null -> item.placedObject === placed
        item.entity != null -> item.entity === entity
        item.terrain != null -> item.terrain.position == tile
        else -> false
    }
}

internal fun DebugSettings.needsHoveredVisualizationTarget(): Boolean =
    visualizationFilter == DebugVisualizationFilter.HOVERED &&
        (objects.enabled || entities.enabled || renderOrder.enabled || culling.enabled)
