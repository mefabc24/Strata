package com.mefabc24.strata.debug

import com.mefabc24.strata.placement.PlacementFailureReason
import com.mefabc24.strata.world.TilePosition

internal object DebugActionMessages {
    fun spawned(type: String, position: TilePosition): String =
        "$type spawned at (${position.x}, ${position.y})"

    fun placedObjects(count: Int): String =
        "Placed $count ${if (count == 1) "object" else "objects"}"

    fun pathFound(length: Int): String = "Path found: $length tiles"

    fun placementRejected(failure: PlacementFailureReason?): String =
        "Object placement rejected: " + when (failure) {
            PlacementFailureReason.FOOTPRINT_OUTSIDE_WORLD -> "outside world"
            PlacementFailureReason.OCCUPIED_TILE -> "occupied tile"
            PlacementFailureReason.RESERVED_TILE_CONFLICT -> "overlapping preview"
            PlacementFailureReason.EXTERNAL_VALIDATOR_REJECTED -> "validator rejected"
            null -> "no valid target"
        }
}
