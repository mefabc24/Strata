package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.debug.DebugDeleteToolSettings
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

enum class DebugRemovalKind { OBJECT, ENTITY, TERRAIN_OVERLAY }

/** Removes the single frontmost removable world element selected by a debug tool. */
class DebugWorldRemover(
    private val world: World,
    private val settings: DebugDeleteToolSettings = DebugDeleteToolSettings()
) {
    private val brushStroke = DebugBrushStroke()

    fun remove(target: PickedTarget?): DebugRemovalKind? = when (target) {
        is PickedTarget.Object -> removeObject(target.placedObject)
        is PickedTarget.Entity -> removeEntity(target.worldEntity)
        is PickedTarget.Tile -> removeTopOverlay(target.position)
        null -> null
    }

    fun removeEntity(entity: WorldEntity): DebugRemovalKind? =
        DebugRemovalKind.ENTITY.takeIf { world.removeEntity(entity) }

    fun removeObject(placedObject: PlacedObject): DebugRemovalKind? =
        DebugRemovalKind.OBJECT.takeIf { world.remove(placedObject) }

    fun beginDelete(position: TilePosition): Set<DebugRemovalKind> {
        val removed = mutableSetOf<DebugRemovalKind>()
        brushStroke.begin(position, settings.brushSize, world.width, world.height) { tile ->
            removeAtTile(tile, removed)
        }
        return removed
    }

    fun dragDelete(position: TilePosition): Set<DebugRemovalKind> {
        if (!settings.dragEnabled) return emptySet()
        val removed = mutableSetOf<DebugRemovalKind>()
        brushStroke.drag(position, settings.brushSize, world.width, world.height) { tile ->
            removeAtTile(tile, removed)
        }
        return removed
    }

    fun endDelete(): Boolean {
        if (!brushStroke.active) return false
        brushStroke.cancel()
        return true
    }

    fun cancel() = brushStroke.cancel()

    private fun removeAtTile(position: TilePosition, removed: MutableSet<DebugRemovalKind>) {
        world.getEntities().filter { it.currentTile == position }.forEach { entity ->
            if (world.removeEntity(entity)) removed += DebugRemovalKind.ENTITY
        }
        world.getObjectAt(position)?.let { placed ->
            if (world.remove(placed)) removed += DebugRemovalKind.OBJECT
        }
        if (removeTopOverlay(position) != null) removed += DebugRemovalKind.TERRAIN_OVERLAY
    }

    private fun removeTopOverlay(position: TilePosition): DebugRemovalKind? {
        if (world.getTile(position) == null) return null
        val layer = world.overlayLayerIds.asReversed().firstOrNull { layerId ->
            world.getOverlayTile(layerId, position) != null
        } ?: return null
        world.setOverlayTile(layer, position, null)
        return DebugRemovalKind.TERRAIN_OVERLAY
    }
}
