package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import kotlin.math.abs

/** Applies rectangular, drag-anchored packing through normal placement rules. */
class DebugBuildDragController(
    private val placement: PlacementController
) {
    private var start: TilePosition? = null
    private var previewEnd: TilePosition? = null
    private var previewOrigins: List<TilePosition> = emptyList()

    val active: Boolean get() = start != null
    val previewCount: Int get() = previewOrigins.size

    fun begin(position: TilePosition): Boolean {
        if (!placement.enabled || placement.selectedFactory == null) {
            cancel()
            return false
        }
        start = position
        previewEnd = position
        previewOrigins = listOf(position)
        placement.previewAt(previewOrigins)
        return true
    }

    fun dragTo(position: TilePosition): Boolean {
        val dragStart = start ?: return false
        if (previewEnd == position) return true
        previewEnd = position
        previewOrigins = placementOrigins(dragStart, position)
        placement.previewAt(previewOrigins)
        return true
    }

    fun finish(position: TilePosition): List<PlacedObject> {
        val dragStart = start ?: return emptyList()
        val positions = if (previewEnd == position) {
            previewOrigins
        } else {
            placementOrigins(dragStart, position)
        }
        return try {
            placement.placeAt(positions)
        } finally {
            cancel()
        }
    }

    fun cancel(): Boolean {
        if (start == null) return false
        start = null
        previewEnd = null
        previewOrigins = emptyList()
        placement.clearPreviewPositions()
        return true
    }

    private fun placementOrigins(
        start: TilePosition,
        end: TilePosition
    ): List<TilePosition> {
        val footprint = placement.selectedPlaceable?.footprint ?: return emptyList()
        val stepX = footprint.offsets.maxOf { it.x } -
            footprint.offsets.minOf { it.x } + 1
        val stepY = footprint.offsets.maxOf { it.y } -
            footprint.offsets.minOf { it.y } + 1
        val xs = dragOrigins(start.x, end.x, stepX)
        val ys = dragOrigins(start.y, end.y, stepY)
        return buildList {
            for (y in ys) for (x in xs) add(TilePosition(x, y))
        }
    }

    private fun dragOrigins(start: Int, end: Int, step: Int): List<Int> {
        val direction = if (end >= start) 1 else -1
        return List(abs(end - start) / step + 1) { index ->
            start + index * step * direction
        }
    }
}
