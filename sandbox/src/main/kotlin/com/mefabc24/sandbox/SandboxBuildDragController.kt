package com.mefabc24.sandbox

import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TileArea
import com.mefabc24.strata.world.TilePosition

/**
 * Applies the Sandbox's rectangular drag-building policy to placement.
 */
class SandboxBuildDragController(
    private val placement: PlacementController
) {
    private var start: TilePosition? = null

    val active: Boolean
        get() = start != null

    fun begin(position: TilePosition): Boolean {
        if (!placement.enabled || placement.selectedFactory == null) {
            cancel()
            return false
        }

        start = position
        placement.previewAt(listOf(position))
        return true
    }

    fun dragTo(position: TilePosition): Boolean {
        val dragStart = start ?: return false

        placement.previewAt(
            TileArea.between(dragStart, position).positions
        )
        return true
    }

    fun finish(position: TilePosition): List<PlacedObject> {
        val dragStart = start ?: return emptyList()
        val positions = TileArea.between(dragStart, position).positions

        return try {
            placement.placeAt(positions)
        } finally {
            cancel()
        }
    }

    fun cancel(): Boolean {
        if (start == null) return false

        start = null
        placement.clearPreviewPositions()
        return true
    }
}
