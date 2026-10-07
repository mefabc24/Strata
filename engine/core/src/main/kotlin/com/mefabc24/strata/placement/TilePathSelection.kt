package com.mefabc24.strata.placement

import com.mefabc24.strata.world.TilePosition

/**
 * A reusable multi-waypoint selection with an optional live endpoint.
 * Confirmed segments are resolved once; moving the live endpoint resolves only
 * the latest segment. Paths follow waypoint order and omit shared junction tiles.
 * Nonconsecutive revisits are retained to preserve a connected logical chain.
 * Exposed lists are read-only snapshots, independent of later edits.
 * Finishing, cancelling, or clearing returns the selection to idle.
 */
class TilePathSelection(
    private val resolver: TilePathResolver = DirectTilePathResolver
) {
    private sealed interface State {
        data object Idle : State
        data class Drawing(
            val waypoints: List<TilePosition>,
            val confirmed: List<TilePosition>,
            val endpoint: TilePosition?,
            val segment: List<TilePosition>
        ) : State
    }

    private var state: State = State.Idle

    /** Whether a first point has been selected. */
    val active: Boolean get() = state is State.Drawing

    /** Confirmed waypoint snapshot; the live endpoint is not a waypoint. */
    val waypoints: List<TilePosition>
        get() = (state as? State.Drawing)?.waypoints?.toList().orEmpty()

    /** Complete ordered path, including the unfinished segment if present. */
    val positions: List<TilePosition>
        get() = (state as? State.Drawing)?.let {
            it.confirmed + it.segment.drop(1)
        }.orEmpty()

    /** Starts a new selection, replacing any previous selection. */
    fun start(position: TilePosition) {
        state = State.Drawing(listOf(position), listOf(position), position, listOf(position))
    }

    /** Updates the unfinished segment. Null hides it while retaining confirmed tiles. */
    fun previewTo(position: TilePosition?): Boolean {
        val drawing = state as? State.Drawing ?: return false
        if (drawing.endpoint == position) return true
        val segment = position?.let { resolve(drawing.waypoints.last(), it) }.orEmpty()
        state = drawing.copy(endpoint = position, segment = segment)
        return true
    }

    /** Confirms a segment and continues from its endpoint, without a waypoint limit. */
    fun addWaypoint(position: TilePosition): Boolean {
        val drawing = state as? State.Drawing ?: return false
        if (drawing.waypoints.last() == position) return previewTo(position)
        previewTo(position)
        val updated = state as State.Drawing
        state = State.Drawing(
            updated.waypoints + position,
            updated.confirmed + updated.segment.drop(1),
            position,
            listOf(position)
        )
        return true
    }

    /** Returns the complete path and clears selection. An optional [end] updates the live segment. */
    fun finish(end: TilePosition? = null): List<TilePosition> {
        if (end != null) previewTo(end)
        return positions.also { clear() }
    }

    /** Discards selection. Returns whether there was an active selection. */
    fun cancel(): Boolean = clear()

    /** Resets all waypoints and cached segments to idle. */
    fun clear(): Boolean {
        val wasActive = active
        state = State.Idle
        return wasActive
    }

    private fun resolve(start: TilePosition, end: TilePosition): List<TilePosition> {
        val segment = resolver.resolve(start, end).toList()
        require(segment.firstOrNull() == start && segment.lastOrNull() == end) {
            "A tile path resolver must include both endpoints in order."
        }
        require(segment.zipWithNext().all { (a, b) ->
            a != b && kotlin.math.abs(a.x.toLong() - b.x) <= 1 &&
                kotlin.math.abs(a.y.toLong() - b.y) <= 1
        }) { "A tile path resolver must return an eight-connected chain without consecutive duplicates." }
        return segment
    }
}
