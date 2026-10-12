package com.mefabc24.strata.placement

import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition

/**
 * Connects tile-path selection to the owning [PlacementController]'s previews
 * and ordered batch placement. Footprints, bounds, occupancy, and external
 * validation use the same pipeline as other placement operations.
 * Invalid origins are skipped; earlier successful placements remain.
 * Changing selection, disabling placement, or starting another placement
 * operation clears the active path. Games must cancel when leaving their tool.
 */
class PathPlacementController internal constructor(
    private val placement: PlacementController
) {
    private var selection = TilePathSelection()

    /** Resolver for new segments. Assigning one cancels any in-progress path. */
    var resolver: TilePathResolver = DirectTilePathResolver
        set(value) {
            cancel()
            field = value
            selection = TilePathSelection(value)
        }

    /** Whether a path is currently being selected. */
    val active: Boolean get() = selection.active

    /** Confirmed waypoint snapshot, excluding the live endpoint. */
    val waypoints: List<TilePosition> get() = selection.waypoints

    /** Ordered logical path snapshot, including the unfinished segment and both endpoints. */
    val positions: List<TilePosition> get() = selection.positions

    /** Starts a new path and preview, or returns false when placement is unavailable. */
    fun begin(position: TilePosition): Boolean {
        placement.clearPreviewPositions()
        if (!placement.enabled || placement.selectedFactory == null) return false
        selection.start(position)
        refreshPreview()
        return true
    }

    /**
     * Updates the live endpoint and revalidates the complete preview against the
     * current world. Null removes only the unfinished segment.
     */
    fun update(position: TilePosition?): Boolean {
        if (!selection.previewTo(position)) return false
        refreshPreview()
        return true
    }

    /** Confirms the next segment and continues previewing from its endpoint. */
    fun addWaypoint(position: TilePosition): Boolean {
        if (!selection.addWaypoint(position)) return false
        refreshPreview()
        return true
    }

    /**
     * Places the currently resolved path and returns successful objects in order.
     * An optional [end] updates the final segment before placement. Revisited
     * origins are placed once. Selection and previews are cleared even on failure.
     * Factories must produce consistent footprints for previews to predict placement;
     * placement rechecks the current world and validator for every fresh object.
     */
    fun finish(end: TilePosition? = null): List<PlacedObject> {
        if (!active) return emptyList()
        return try {
            if (end != null) update(end)
            val origins = selection.finish()
            placement.clearPreviewPositions()
            placement.placeAt(origins)
        } finally {
            placement.clearPreviewPositions()
        }
    }

    /** Discards this path and its previews. Returns whether it was active. */
    fun cancel(): Boolean {
        if (!active) return false
        placement.clearPreviewPositions()
        return true
    }

    /** Resets this path to idle, as for [cancel]. */
    fun clear(): Boolean = cancel()

    internal fun clearSelection() {
        selection.clear()
    }

    private fun refreshPreview() {
        placement.previewPathAt(selection.positions)
    }
}
