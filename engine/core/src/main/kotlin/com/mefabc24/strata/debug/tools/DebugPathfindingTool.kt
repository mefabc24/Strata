package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.debug.DebugEntityFreezeState
import com.mefabc24.strata.pathfinding.PathMovementMode
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.pathfinding.findPathDiagnostic
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

internal const val DEFAULT_DEBUG_ENTITY_SPEED = 2f

/** Persistent standalone and entity-route pathfinding state for debug tooling. */
class DebugPathfindingTool internal constructor(
    private val world: World,
    private val state: DebugWorldState,
    private val canEnter: (TilePosition) -> Boolean = { true },
    private val movementCost: ((TilePosition, TilePosition) -> Float)? = null,
    private val movementMode: () -> PathMovementMode = { PathMovementMode.FOUR_WAY },
    private val entitySpeedMultiplier: () -> Float = { 1f },
    private val consumeReachedWaypoints: () -> Boolean = { true },
    private val entityFreezeState: DebugEntityFreezeState = DebugEntityFreezeState()
) {
    constructor(
        world: World,
        canEnter: (TilePosition) -> Boolean = { true }
    ) : this(world, DebugWorldState(), canEnter)

    private var committedEntityResult: PathfindingDiagnosticResult? = null
    private val committedEntitySegments = mutableListOf<PathfindingDiagnosticResult>()

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
        releaseSelectedEntity()
        entityFreezeState.setPathfindingHeld(entity, true)
        state.pathfindingEntity = entity
        state.pathfindingWaypoints = listOf(entity.currentTile)
        state.pathfinding = null
        state.pathfindingEntityWaiting = false
        committedEntityResult = null
        committedEntitySegments.clear()
        return true
    }

    /** Keeps an uncommitted entity route start aligned with the entity's live tile. */
    fun update() {
        if (committedEntityResult == null) {
            syncPendingEntityStart()
        } else {
            syncCommittedEntityRoute()
        }
    }

    fun click(position: TilePosition): Boolean {
        if (world.getTile(position) == null) return false
        syncPendingEntityStart()
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
            findPathDiagnostic(requested)
        }
    }

    private fun appendEntityDestination(entity: WorldEntity, position: TilePosition) {
        val committedWaypoints = committedEntityResult?.waypoints ?: waypoints
        val segment = findPathDiagnostic(committedWaypoints.last(), position)
        if (!segment.success) {
            state.pathfinding = failedCombinedResult(
                committedEntityResult,
                segment,
                committedWaypoints + position
            )
            return
        }

        val speed = entity.movementSpeed
            ?: DEFAULT_DEBUG_ENTITY_SPEED * entitySpeedMultiplier()
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
        committedEntityResult = combined
        committedEntitySegments += segment
        state.pathfindingEntityWaiting = false
        syncCommittedEntityRoute()
    }

    private fun syncPendingEntityStart() {
        val entity = selectedEntity ?: return
        if (committedEntityResult != null) return
        val liveStart = entity.currentTile
        if (waypoints.single() == liveStart) return
        state.pathfindingWaypoints = listOf(liveStart)
        state.pathfinding = null
    }

    private fun syncCommittedEntityRoute() {
        val entity = selectedEntity ?: return
        val committed = committedEntityResult ?: return
        val path = requireNotNull(committed.path)
        val completedPathIndex = completedPathIndex(path, entity.remainingPath)
        state.pathfindingEntityWaiting = !entity.isMoving

        if (!consumeReachedWaypoints()) {
            state.pathfindingWaypoints = committed.waypoints
            state.pathfinding = committed
            return
        }

        val waypointIndices = waypointIndices(committed.waypoints, path)
        val reachedWaypointIndex = waypointIndices.indexOfLast { it <= completedPathIndex }
            .coerceAtLeast(0)
        val remainingWaypoints = committed.waypoints.drop(reachedWaypointIndex)
        val remainingPath = path.drop(completedPathIndex)
        val remainingSegments = committedEntitySegments.drop(reachedWaypointIndex)
        state.pathfindingWaypoints = remainingWaypoints
        state.pathfinding = PathfindingDiagnosticResult(
            start = remainingWaypoints.firstOrNull(),
            goal = remainingWaypoints.lastOrNull(),
            path = remainingPath,
            explored = remainingSegments.flatMap(PathfindingDiagnosticResult::explored),
            durationNanos = remainingSegments.sumOf(PathfindingDiagnosticResult::durationNanos),
            waypoints = remainingWaypoints,
            totalCost = when {
                remainingPath.size <= 1 -> 0f
                completedPathIndex == 0 -> committed.totalCost
                else -> null
            }
        )
    }

    private fun completedPathIndex(
        committedPath: List<TilePosition>,
        remainingPath: List<TilePosition>
    ): Int {
        if (remainingPath.isEmpty()) return committedPath.lastIndex
        val suffixStart = committedPath.size - remainingPath.size
        return if (
            suffixStart > 0 && committedPath.subList(suffixStart, committedPath.size) == remainingPath
        ) {
            suffixStart - 1
        } else {
            committedPath.indexOfLast { it == selectedEntity?.currentTile }.coerceAtLeast(0)
        }
    }

    private fun waypointIndices(
        waypoints: List<TilePosition>,
        path: List<TilePosition>
    ): List<Int> {
        var pathIndex = 0
        return waypoints.map { waypoint ->
            val index = (pathIndex until path.size).firstOrNull { path[it] == waypoint }
                ?: pathIndex.coerceAtMost(path.lastIndex)
            pathIndex = index
            index
        }
    }

    fun clear(): Boolean {
        val changed = waypoints.isNotEmpty() || result != null || selectedEntity != null
        releaseSelectedEntity()
        state.pathfindingWaypoints = emptyList()
        state.pathfindingEntity = null
        state.pathfindingEntityWaiting = false
        state.pathfinding = null
        committedEntityResult = null
        committedEntitySegments.clear()
        return changed
    }

    private fun releaseSelectedEntity() {
        selectedEntity?.let { entityFreezeState.setPathfindingHeld(it, false) }
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

    private fun findPathDiagnostic(
        waypoints: List<TilePosition>
    ): PathfindingDiagnosticResult {
        val cost = movementCost
        return if (cost == null) {
            world.findPathDiagnostic(waypoints, movementMode(), canEnter)
        } else {
            world.findPathDiagnostic(waypoints, movementMode(), canEnter, cost)
        }
    }

    private fun findPathDiagnostic(
        start: TilePosition,
        goal: TilePosition
    ): PathfindingDiagnosticResult {
        val cost = movementCost
        return if (cost == null) {
            world.findPathDiagnostic(start, goal, movementMode(), canEnter)
        } else {
            world.findPathDiagnostic(start, goal, movementMode(), canEnter, cost)
        }
    }
}
