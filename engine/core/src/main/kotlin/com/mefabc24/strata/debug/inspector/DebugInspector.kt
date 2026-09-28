package com.mefabc24.strata.debug.inspector

import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.WorldEntity
import com.mefabc24.strata.debug.DebugWorldState

sealed interface DebugInspection {
    data class EntityTarget(val entity: WorldEntity) : DebugInspection
    data class ObjectTarget(val placedObject: PlacedObject) : DebugInspection
    data class TileTarget(val position: TilePosition) : DebugInspection
}

/** Holds the current generic inspection selection. */
class DebugInspector internal constructor(
    private val state: DebugWorldState
) {
    constructor() : this(DebugWorldState())

    val selection: DebugInspection?
        get() = state.inspection

    fun selectFrontmost(
        entity: WorldEntity?,
        placedObject: PlacedObject?,
        tile: TilePosition?
    ): Boolean {
        state.inspection = when {
            entity != null -> DebugInspection.EntityTarget(entity)
            placedObject != null -> DebugInspection.ObjectTarget(placedObject)
            tile != null -> DebugInspection.TileTarget(tile)
            else -> null
        }
        return state.inspection != null
    }

    fun clear(): Boolean {
        if (state.inspection == null) return false
        state.inspection = null
        return true
    }
}
