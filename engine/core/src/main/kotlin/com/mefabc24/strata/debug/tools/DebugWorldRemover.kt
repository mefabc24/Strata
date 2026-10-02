package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

enum class DebugRemovalKind { OBJECT, ENTITY, TERRAIN_OVERLAY }

/** Removes the single frontmost removable world element selected by a debug tool. */
class DebugWorldRemover(private val world: World) {
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

    private fun removeTopOverlay(position: TilePosition): DebugRemovalKind? {
        if (world.getTile(position) == null) return null
        val layer = world.overlayLayerIds.asReversed().firstOrNull { layerId ->
            world.getOverlayTile(layerId, position) != null
        } ?: return null
        world.setOverlayTile(layer, position, null)
        return DebugRemovalKind.TERRAIN_OVERLAY
    }
}
