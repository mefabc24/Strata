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

        placement.previewAt(placementOrigins(dragStart, position))
        return true
    }

    fun finish(position: TilePosition): List<PlacedObject> {
        val dragStart = start ?: return emptyList()
        val positions = placementOrigins(dragStart, position)

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

    private fun placementOrigins(
        start: TilePosition,
        end: TilePosition
    ): List<TilePosition> {
        if (start == end) return listOf(start)

        val placeable = placement.selectedPlaceable ?: return emptyList()
        val footprint = placeable.footprint
        val minOffsetX = footprint.offsets.minOf { it.x }
        val maxOffsetX = footprint.offsets.maxOf { it.x }
        val minOffsetY = footprint.offsets.minOf { it.y }
        val maxOffsetY = footprint.offsets.maxOf { it.y }
        val stepX = maxOffsetX - minOffsetX + 1
        val stepY = maxOffsetY - minOffsetY + 1
        val area = TileArea.between(start, end)
        val firstOriginX = area.minX + footprint.origin.x - minOffsetX
        val firstOriginY = area.minY + footprint.origin.y - minOffsetY

        return buildList {
            for (y in firstOriginY..area.maxY step stepY) {
                for (x in firstOriginX..area.maxX step stepX) {
                    val position = TilePosition(x, y)
                    val occupiedTiles = PlacedObject(placeable, x, y).occupiedTiles()

                    if (occupiedTiles.all { tile ->
                            tile.x in area.minX..area.maxX &&
                                tile.y in area.minY..area.maxY
                        }
                    ) {
                        add(position)
                    }
                }
            }
        }
    }
}
