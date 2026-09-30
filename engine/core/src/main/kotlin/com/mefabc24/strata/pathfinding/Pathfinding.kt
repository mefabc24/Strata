package com.mefabc24.strata.pathfinding

import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.contains
import com.mefabc24.strata.world.neighbors
import java.util.PriorityQueue
import kotlin.math.abs

private val UNIT_MOVEMENT_COST: (TilePosition, TilePosition) -> Float = { _, _ -> 1f }

/**
 * Finds the shortest path between two tile positions.
 *
 * Traversability is defined by the caller so the pathfinder does not depend on
 * game-specific terrain or movement rules. The returned path includes both the
 * start and goal positions, or is null when no path exists.
 */
fun World.findPath(
    start: TilePosition,
    goal: TilePosition,
    canEnter: (TilePosition) -> Boolean = { true }
): List<TilePosition>? = findPathSearch(
    start = start,
    goal = goal,
    canEnter = canEnter,
    movementCost = UNIT_MOVEMENT_COST,
    minimumStepCost = 1f
).path

/**
 * Finds the cheapest path using the caller-supplied cost of traversing each
 * directed edge. Every returned movement cost must be finite and greater than
 * zero. Weighted searches use a zero heuristic so arbitrary costs remain
 * optimal without requiring a caller-supplied lower bound.
 */
fun World.findPath(
    start: TilePosition,
    goal: TilePosition,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): List<TilePosition>? = findPathSearch(
    start = start,
    goal = goal,
    canEnter = canEnter,
    movementCost = movementCost,
    minimumStepCost = 0f
).path

/**
 * Finds a path through every waypoint in order. Segment boundary tiles appear
 * only once in the returned path. An empty waypoint list returns an empty path,
 * and one waypoint returns that waypoint when it is inside and enterable.
 */
fun World.findPath(
    waypoints: List<TilePosition>,
    canEnter: (TilePosition) -> Boolean = { true }
): List<TilePosition>? = findPathThrough(
    waypoints = waypoints,
    canEnter = canEnter,
    movementCost = UNIT_MOVEMENT_COST,
    minimumStepCost = 1f
).path

/**
 * Finds the cheapest path through every waypoint in order using caller-supplied
 * directed edge costs. Every returned cost must be finite and greater than zero.
 * Weighted segments use a zero heuristic so arbitrary costs remain optimal.
 */
fun World.findPath(
    waypoints: List<TilePosition>,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): List<TilePosition>? = findPathThrough(
    waypoints = waypoints,
    canEnter = canEnter,
    movementCost = movementCost,
    minimumStepCost = 0f
).path

/** Diagnostic pathfinding output collected only for explicit debug searches. */
data class PathfindingDiagnosticResult(
    val start: TilePosition?,
    val goal: TilePosition?,
    val path: List<TilePosition>?,
    val explored: List<TilePosition>,
    val durationNanos: Long,
    val waypoints: List<TilePosition> = when {
        start == null -> emptyList()
        goal == null || goal == start -> listOf(start)
        else -> listOf(start, goal)
    },
    val totalCost: Float? = path?.let { (it.size - 1).coerceAtLeast(0).toFloat() }
) {
    val success: Boolean
        get() = path != null
}

/** Runs the normal two-point pathfinder while collecting diagnostics. */
fun World.findPathDiagnostic(
    start: TilePosition,
    goal: TilePosition,
    canEnter: (TilePosition) -> Boolean = { true }
): PathfindingDiagnosticResult = findPathDiagnosticInternal(
    waypoints = listOf(start, goal),
    canEnter = canEnter,
    movementCost = UNIT_MOVEMENT_COST,
    minimumStepCost = 1f
)

/** Runs a weighted two-point search while collecting diagnostics. */
fun World.findPathDiagnostic(
    start: TilePosition,
    goal: TilePosition,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): PathfindingDiagnosticResult = findPathDiagnosticInternal(
    waypoints = listOf(start, goal),
    canEnter = canEnter,
    movementCost = movementCost,
    minimumStepCost = 0f
)

/** Runs an ordered multi-waypoint search while collecting combined diagnostics. */
fun World.findPathDiagnostic(
    waypoints: List<TilePosition>,
    canEnter: (TilePosition) -> Boolean = { true }
): PathfindingDiagnosticResult = findPathDiagnosticInternal(
    waypoints = waypoints,
    canEnter = canEnter,
    movementCost = UNIT_MOVEMENT_COST,
    minimumStepCost = 1f
)

/** Runs a weighted ordered multi-waypoint search with combined diagnostics. */
fun World.findPathDiagnostic(
    waypoints: List<TilePosition>,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): PathfindingDiagnosticResult = findPathDiagnosticInternal(
    waypoints = waypoints,
    canEnter = canEnter,
    movementCost = movementCost,
    minimumStepCost = 0f
)

private fun World.findPathDiagnosticInternal(
    waypoints: List<TilePosition>,
    canEnter: (TilePosition) -> Boolean,
    movementCost: (TilePosition, TilePosition) -> Float,
    minimumStepCost: Float
): PathfindingDiagnosticResult {
    val started = System.nanoTime()
    val search = findPathThrough(
        waypoints = waypoints,
        canEnter = canEnter,
        movementCost = movementCost,
        minimumStepCost = minimumStepCost
    )
    return PathfindingDiagnosticResult(
        start = waypoints.firstOrNull(),
        goal = waypoints.lastOrNull(),
        path = search.path,
        explored = search.explored,
        durationNanos = System.nanoTime() - started,
        waypoints = waypoints.toList(),
        totalCost = search.totalCost
    )
}

private fun World.findPathThrough(
    waypoints: List<TilePosition>,
    canEnter: (TilePosition) -> Boolean,
    movementCost: (TilePosition, TilePosition) -> Float,
    minimumStepCost: Float
): PathSearchResult {
    if (waypoints.isEmpty()) {
        return PathSearchResult(emptyList(), emptyList(), 0f)
    }

    if (waypoints.size == 1) {
        return findPathSearch(
            waypoints.single(),
            waypoints.single(),
            canEnter,
            movementCost,
            minimumStepCost
        )
    }

    val combinedPath = mutableListOf<TilePosition>()
    val combinedExplored = mutableListOf<TilePosition>()
    var combinedCost = 0f

    waypoints.zipWithNext().forEachIndexed { segmentIndex, (start, goal) ->
        val segment = findPathSearch(
            start,
            goal,
            canEnter,
            movementCost,
            minimumStepCost
        )
        combinedExplored += segment.explored
        val segmentPath = segment.path
            ?: return PathSearchResult(null, combinedExplored, null)
        if (segmentIndex == 0) {
            combinedPath += segmentPath
        } else {
            combinedPath += segmentPath.drop(1)
        }
        combinedCost += requireNotNull(segment.totalCost)
    }

    return PathSearchResult(combinedPath, combinedExplored, combinedCost)
}

private fun World.findPathSearch(
    start: TilePosition,
    goal: TilePosition,
    canEnter: (TilePosition) -> Boolean,
    movementCost: (TilePosition, TilePosition) -> Float,
    minimumStepCost: Float
): PathSearchResult {
    require(contains(start)) {
        "Start position $start is outside the world."
    }
    require(contains(goal)) {
        "Goal position $goal is outside the world."
    }

    if (!canEnter(start) || !canEnter(goal)) {
        return PathSearchResult(null, emptyList(), null)
    }
    if (start == goal) {
        return PathSearchResult(listOf(start), listOf(start), 0f)
    }

    val open = PriorityQueue(
        compareBy<PathNode> { it.fScore }
            .thenBy { it.hScore }
    )
    val cameFrom = mutableMapOf<TilePosition, TilePosition>()
    val gScore = mutableMapOf<TilePosition, Float>()
    val closed = mutableSetOf<TilePosition>()
    val explored = mutableListOf<TilePosition>()

    gScore[start] = 0f
    val startHeuristic = heuristic(start, goal, minimumStepCost)
    open.add(PathNode(start, startHeuristic, startHeuristic))

    while (open.isNotEmpty()) {
        val current = open.remove().position
        if (!closed.add(current)) continue
        explored += current

        if (current == goal) {
            return PathSearchResult(
                path = reconstructPath(cameFrom, goal),
                explored = explored,
                totalCost = requireNotNull(gScore[goal])
            )
        }

        val currentScore = gScore[current] ?: continue
        for (neighbor in neighbors(current)) {
            if (neighbor in closed || !canEnter(neighbor)) continue

            val edgeCost = movementCost(current, neighbor)
            require(edgeCost.isFinite() && edgeCost > 0f) {
                "Movement cost from $current to $neighbor must be finite and positive, but was $edgeCost."
            }
            val tentativeScore = currentScore + edgeCost
            val knownScore = gScore[neighbor] ?: Float.POSITIVE_INFINITY
            if (tentativeScore >= knownScore) continue

            cameFrom[neighbor] = current
            gScore[neighbor] = tentativeScore
            val neighborHeuristic = heuristic(neighbor, goal, minimumStepCost)
            open.add(
                PathNode(
                    position = neighbor,
                    fScore = tentativeScore + neighborHeuristic,
                    hScore = neighborHeuristic
                )
            )
        }
    }

    return PathSearchResult(null, explored, null)
}

private data class PathSearchResult(
    val path: List<TilePosition>?,
    val explored: List<TilePosition>,
    val totalCost: Float?
)

private data class PathNode(
    val position: TilePosition,
    val fScore: Float,
    val hScore: Float
)

private fun heuristic(
    from: TilePosition,
    to: TilePosition,
    minimumStepCost: Float
): Float = (
    abs(from.x - to.x) + abs(from.y - to.y)
).toFloat() * minimumStepCost

private fun reconstructPath(
    cameFrom: Map<TilePosition, TilePosition>,
    goal: TilePosition
): List<TilePosition> {
    val path = mutableListOf(goal)
    var current = goal
    while (true) {
        current = cameFrom[current] ?: break
        path += current
    }
    path.reverse()
    return path
}
