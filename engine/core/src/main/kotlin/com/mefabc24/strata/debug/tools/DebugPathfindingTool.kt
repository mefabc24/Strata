package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.pathfinding.findPathDiagnostic
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

/** Persistent standalone and entity-route pathfinding state for debug tooling. */
class DebugPathfindingTool internal constructor(
    private val world: World,
    private val state: DebugWorldState,
    private val canEnter: (TilePosition) -> Boolean = { true },
    private val movementCost: (TilePosition, TilePosition) -> Float = { _, _ -> 1f },
    private val idleEntitySpeed: () -> Float = { 1f }
) {
    constructor(
        world: World,
        canEnter: (TilePosition) -> Boolean = { true }
    ) : this(world, DebugWorldState(), canEnter)

    private var committedEntityResult: PathfindingDiagnosticResult? = null

    val waypoints: List<TilePosition>
        get() = state.pathfindingWaypoints

    val start: TilePosition?
        get() = waypoints.firstOrNull()

    val selectedEntity: WorldEntity?
        get() = state.pathfindingEntity

    val result: PathfindingDiagnosticResult?
        get() = state.pathfinding

    /** Entity picking is available before standalone path editing or during an entity route. */
    val acceptsEntitySelection: Boolean
        get() = selectedEntity != null || waypoints.isEmpty()

    fun selectEntity(entity: WorldEntity): Boolean {
        if (selectedEntity === entity) return true
        state.pathfindingEntity = entity
        state.pathfindingWaypoints = listOf(entity.currentTile)
        state.pathfinding = null
        committedEntityResult = null
        return true
    }

    fun click(position: TilePosition): Boolean {
        if (world.getTile(position) == null) return false
        val entity = selectedEntity
        if (entity != null) {
            appendEntityDestination(entity, position)
        } else {
            appendStandaloneWaypoint(position)
        }
        return true
    }

    private fun appendStandaloneWaypoint(position: TilePosition) {
        val requested = waypoints + position
        state.pathfindingWaypoints = requested
        state.pathfinding = if (requested.size == 1) {
            null
        } else {
            world.findPathDiagnostic(requested, canEnter, movementCost)
        }
    }

    private fun appendEntityDestination(entity: WorldEntity, position: TilePosition) {
        var committedWaypoints = waypoints
        if (committedEntityResult == null && committedWaypoints.single() != entity.currentTile) {
            committedWaypoints = listOf(entity.currentTile)
            state.pathfindingWaypoints = committedWaypoints
        }
        val segment = world.findPathDiagnostic(
            start = committedWaypoints.last(),
            goal = position,
            canEnter = canEnter,
            movementCost = movementCost
        )
        if (!segment.success) {
            state.pathfinding = failedCombinedResult(
                committedEntityResult,
                segment,
                committedWaypoints + position
            )
            return
        }

        val speed = entity.movementSpeed ?: idleEntitySpeed()
        val segmentPath = requireNotNull(segment.path)
        val assignment = if (committedWaypoints.size == 1) {
            segmentPath
        } else {
            entity.remainingPath + segmentPath.drop(1)
        }
        entity.followPath(assignment, speed)

        val combined = successfulCombinedResult(
            committedEntityResult,
            segment,
            committedWaypoints + position
        )
        state.pathfindingWaypoints = combined.waypoints
        state.pathfinding = combined
        committedEntityResult = combined
    }

    fun clear(): Boolean {
        val changed = waypoints.isNotEmpty() || result != null || selectedEntity != null
        state.pathfindingWaypoints = emptyList()
        state.pathfindingEntity = null
        state.pathfinding = null
        committedEntityResult = null
        return changed
    }

    private fun successfulCombinedResult(
        previous: PathfindingDiagnosticResult?,
        segment: PathfindingDiagnosticResult,
        requestedWaypoints: List<TilePosition>
    ): PathfindingDiagnosticResult {
        if (previous == null) {
            return segment.copy(waypoints = requestedWaypoints)
        }
        return PathfindingDiagnosticResult(
            start = requestedWaypoints.first(),
            goal = requestedWaypoints.last(),
            path = requireNotNull(previous.path) + requireNotNull(segment.path).drop(1),
            explored = previous.explored + segment.explored,
            durationNanos = previous.durationNanos + segment.durationNanos,
            waypoints = requestedWaypoints,
            totalCost = requireNotNull(previous.totalCost) + requireNotNull(segment.totalCost)
        )
    }

    private fun failedCombinedResult(
        previous: PathfindingDiagnosticResult?,
        segment: PathfindingDiagnosticResult,
        requestedWaypoints: List<TilePosition>
    ): PathfindingDiagnosticResult = PathfindingDiagnosticResult(
        start = requestedWaypoints.first(),
        goal = requestedWaypoints.last(),
        path = null,
        explored = previous?.explored.orEmpty() + segment.explored,
        durationNanos = (previous?.durationNanos ?: 0L) + segment.durationNanos,
        waypoints = requestedWaypoints,
        totalCost = null
    )
}
