package com.mefabc24.strata.pathfinding

import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.contains
import com.mefabc24.strata.world.neighbors
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/** Tile transitions available to path searches. */
enum class PathMovementMode {
    FOUR_WAY,
    EIGHT_WAY
}

private val GRID_MOVEMENT_COST: (TilePosition, TilePosition) -> Float = { from, to ->
    if (from.x != to.x && from.y != to.y) DIAGONAL_MOVEMENT_COST else 1f
}

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
): List<TilePosition>? = findPath(
    start = start,
    goal = goal,
    movementMode = PathMovementMode.FOUR_WAY,
    canEnter = canEnter
)

/**
 * Finds the shortest path using the transitions allowed by [movementMode].
 * Orthogonal steps cost `1`, while diagonal steps cost `sqrt(2)`.
 */
fun World.findPath(
    start: TilePosition,
    goal: TilePosition,
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean = { true }
): List<TilePosition>? = findPathSearch(
    start = start,
    goal = goal,
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = GRID_MOVEMENT_COST,
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
): List<TilePosition>? = findPath(
    start = start,
    goal = goal,
    movementMode = PathMovementMode.FOUR_WAY,
    canEnter = canEnter,
    movementCost = movementCost
)

/**
 * Finds the cheapest path using the transitions allowed by [movementMode].
 *
 * [movementCost] supplies the complete cost of every directed edge, including
 * diagonal edges in [PathMovementMode.EIGHT_WAY]. The pathfinder does not add
 * a diagonal multiplier to custom costs. Every returned cost must be finite and
 * greater than zero. A zero heuristic keeps arbitrary positive costs optimal.
 */
fun World.findPath(
    start: TilePosition,
    goal: TilePosition,
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): List<TilePosition>? = findPathSearch(
    start = start,
    goal = goal,
    movementMode = movementMode,
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
): List<TilePosition>? = findPath(
    waypoints = waypoints,
    movementMode = PathMovementMode.FOUR_WAY,
    canEnter = canEnter
)

/** Finds a path through every waypoint using [movementMode]. */
fun World.findPath(
    waypoints: List<TilePosition>,
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean = { true }
): List<TilePosition>? = findPathThrough(
    waypoints = waypoints,
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = GRID_MOVEMENT_COST,
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
): List<TilePosition>? = findPath(
    waypoints = waypoints,
    movementMode = PathMovementMode.FOUR_WAY,
    canEnter = canEnter,
    movementCost = movementCost
)

/**
 * Finds the cheapest path through every waypoint using [movementMode]. Custom
 * costs define the complete directed-edge cost, including diagonal edges.
 */
fun World.findPath(
    waypoints: List<TilePosition>,
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): List<TilePosition>? = findPathThrough(
    waypoints = waypoints,
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = movementCost,
    minimumStepCost = 0f
).path

/** Diagnostic pathfinding output collected only for explicit debug searches. */
enum class PathfindingSearchStatus { READY, RUNNING, SUCCEEDED, FAILED }

enum class PathfindingNodeStatus { OPEN, CLOSED }

enum class PathfindingTransitionRejection { BLOCKED, DIAGONAL_BLOCKED, CLOSED }

data class PathfindingNodeDiagnostic(
    val position: TilePosition,
    val gCost: Float,
    val hCost: Float,
    val fCost: Float,
    val parent: TilePosition?,
    val status: PathfindingNodeStatus,
    val explorationOrder: Int?
)

data class PathfindingRejectedTransition(
    val from: TilePosition,
    val to: TilePosition,
    val reason: PathfindingTransitionRejection
)

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
    val totalCost: Float? = path?.let { (it.size - 1).coerceAtLeast(0).toFloat() },
    val nodes: List<PathfindingNodeDiagnostic> = emptyList(),
    val rejectedTransitions: List<PathfindingRejectedTransition> = emptyList(),
    val status: PathfindingSearchStatus = if (path != null) {
        PathfindingSearchStatus.SUCCEEDED
    } else {
        PathfindingSearchStatus.FAILED
    }
) {
    val success: Boolean
        get() = path != null

    val goalReached: Boolean
        get() = status == PathfindingSearchStatus.SUCCEEDED

    val openSetSize: Int
        get() = nodes.count { it.status == PathfindingNodeStatus.OPEN }

    val closedSetSize: Int
        get() = nodes.count { it.status == PathfindingNodeStatus.CLOSED }
}

/** Runs the normal two-point pathfinder while collecting diagnostics. */
fun World.findPathDiagnostic(
    start: TilePosition,
    goal: TilePosition,
    canEnter: (TilePosition) -> Boolean = { true }
): PathfindingDiagnosticResult = findPathDiagnostic(
    start = start,
    goal = goal,
    movementMode = PathMovementMode.FOUR_WAY,
    canEnter = canEnter
)

/** Runs a two-point search in [movementMode] while collecting diagnostics. */
fun World.findPathDiagnostic(
    start: TilePosition,
    goal: TilePosition,
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean = { true }
): PathfindingDiagnosticResult = findPathDiagnosticInternal(
    waypoints = listOf(start, goal),
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = GRID_MOVEMENT_COST,
    minimumStepCost = 1f
)

/** Runs a weighted two-point search while collecting diagnostics. */
fun World.findPathDiagnostic(
    start: TilePosition,
    goal: TilePosition,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): PathfindingDiagnosticResult = findPathDiagnostic(
    start = start,
    goal = goal,
    movementMode = PathMovementMode.FOUR_WAY,
    canEnter = canEnter,
    movementCost = movementCost
)

/** Runs a weighted two-point search in [movementMode] with diagnostics. */
fun World.findPathDiagnostic(
    start: TilePosition,
    goal: TilePosition,
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): PathfindingDiagnosticResult = findPathDiagnosticInternal(
    waypoints = listOf(start, goal),
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = movementCost,
    minimumStepCost = 0f
)

/** Creates a resumable diagnostic search using the same engine as [findPath]. */
fun World.createPathfindingDiagnosticSearch(
    waypoints: List<TilePosition>,
    movementMode: PathMovementMode = PathMovementMode.FOUR_WAY,
    canEnter: (TilePosition) -> Boolean = { true },
    maximumRejectedTransitions: Int = 4096
): PathfindingDiagnosticSearch = PathfindingDiagnosticSearch(
    world = this,
    waypoints = waypoints,
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = GRID_MOVEMENT_COST,
    minimumStepCost = 1f,
    maximumRejectedTransitions = maximumRejectedTransitions
)

/** Creates a resumable weighted diagnostic search with a zero heuristic. */
fun World.createPathfindingDiagnosticSearch(
    waypoints: List<TilePosition>,
    movementMode: PathMovementMode = PathMovementMode.FOUR_WAY,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float,
    maximumRejectedTransitions: Int = 4096
): PathfindingDiagnosticSearch = PathfindingDiagnosticSearch(
    world = this,
    waypoints = waypoints,
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = movementCost,
    minimumStepCost = 0f,
    maximumRejectedTransitions = maximumRejectedTransitions
)

fun World.createPathfindingDiagnosticSearch(
    start: TilePosition,
    goal: TilePosition,
    movementMode: PathMovementMode = PathMovementMode.FOUR_WAY,
    canEnter: (TilePosition) -> Boolean = { true },
    maximumRejectedTransitions: Int = 4096
): PathfindingDiagnosticSearch = createPathfindingDiagnosticSearch(
    waypoints = listOf(start, goal),
    movementMode = movementMode,
    canEnter = canEnter,
    maximumRejectedTransitions = maximumRejectedTransitions
)

fun World.createPathfindingDiagnosticSearch(
    start: TilePosition,
    goal: TilePosition,
    movementMode: PathMovementMode = PathMovementMode.FOUR_WAY,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float,
    maximumRejectedTransitions: Int = 4096
): PathfindingDiagnosticSearch = createPathfindingDiagnosticSearch(
    waypoints = listOf(start, goal),
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = movementCost,
    maximumRejectedTransitions = maximumRejectedTransitions
)

/** Runs an ordered multi-waypoint search while collecting combined diagnostics. */
fun World.findPathDiagnostic(
    waypoints: List<TilePosition>,
    canEnter: (TilePosition) -> Boolean = { true }
): PathfindingDiagnosticResult = findPathDiagnostic(
    waypoints = waypoints,
    movementMode = PathMovementMode.FOUR_WAY,
    canEnter = canEnter
)

/** Runs a multi-waypoint search in [movementMode] with diagnostics. */
fun World.findPathDiagnostic(
    waypoints: List<TilePosition>,
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean = { true }
): PathfindingDiagnosticResult = findPathDiagnosticInternal(
    waypoints = waypoints,
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = GRID_MOVEMENT_COST,
    minimumStepCost = 1f
)

/** Runs a weighted ordered multi-waypoint search with combined diagnostics. */
fun World.findPathDiagnostic(
    waypoints: List<TilePosition>,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): PathfindingDiagnosticResult = findPathDiagnostic(
    waypoints = waypoints,
    movementMode = PathMovementMode.FOUR_WAY,
    canEnter = canEnter,
    movementCost = movementCost
)

/** Runs a weighted multi-waypoint search in [movementMode] with diagnostics. */
fun World.findPathDiagnostic(
    waypoints: List<TilePosition>,
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean = { true },
    movementCost: (from: TilePosition, to: TilePosition) -> Float
): PathfindingDiagnosticResult = findPathDiagnosticInternal(
    waypoints = waypoints,
    movementMode = movementMode,
    canEnter = canEnter,
    movementCost = movementCost,
    minimumStepCost = 0f
)

private fun World.findPathDiagnosticInternal(
    waypoints: List<TilePosition>,
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean,
    movementCost: (TilePosition, TilePosition) -> Float,
    minimumStepCost: Float
): PathfindingDiagnosticResult {
    return PathfindingDiagnosticSearch(
        world = this,
        waypoints = waypoints,
        movementMode = movementMode,
        canEnter = canEnter,
        movementCost = movementCost,
        minimumStepCost = minimumStepCost,
        maximumRejectedTransitions = 4096
    ).runToCompletion()
}

private fun World.findPathThrough(
    waypoints: List<TilePosition>,
    movementMode: PathMovementMode,
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
            movementMode,
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
            movementMode,
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
    movementMode: PathMovementMode,
    canEnter: (TilePosition) -> Boolean,
    movementCost: (TilePosition, TilePosition) -> Float,
    minimumStepCost: Float
): PathSearchResult {
    val search = PathSearchMachine(
        world = this,
        start = start,
        goal = goal,
        movementMode = movementMode,
        canEnter = canEnter,
        movementCost = movementCost,
        minimumStepCost = minimumStepCost,
        collectDiagnostics = false,
        maximumRejectedTransitions = 0
    )
    while (!search.complete) search.step()
    return search.result()
}

private data class PathSearchResult(
    val path: List<TilePosition>?,
    val explored: List<TilePosition>,
    val totalCost: Float?,
    val nodes: List<PathfindingNodeDiagnostic> = emptyList(),
    val rejectedTransitions: List<PathfindingRejectedTransition> = emptyList(),
    val status: PathfindingSearchStatus = if (path != null) {
        PathfindingSearchStatus.SUCCEEDED
    } else {
        PathfindingSearchStatus.FAILED
    }
)

private data class PathNode(
    val position: TilePosition,
    val fScore: Float,
    val hScore: Float
)

/** Resumable diagnostic execution over the engine's normal path search state machine. */
class PathfindingDiagnosticSearch internal constructor(
    private val world: World,
    waypoints: List<TilePosition>,
    private val movementMode: PathMovementMode,
    private val canEnter: (TilePosition) -> Boolean,
    private val movementCost: (TilePosition, TilePosition) -> Float,
    private val minimumStepCost: Float,
    private val maximumRejectedTransitions: Int
) {
    val waypoints: List<TilePosition> = waypoints.toList()

    private val completedSegments = mutableListOf<PathSearchResult>()
    private var segmentIndex = 0
    private var current: PathSearchMachine? = null
    private var finalStatus: PathfindingSearchStatus? = null
    private var durationNanos = 0L

    val complete: Boolean
        get() = finalStatus != null

    val status: PathfindingSearchStatus
        get() = finalStatus ?: current?.status ?: PathfindingSearchStatus.READY

    init {
        require(maximumRejectedTransitions >= 0) {
            "Maximum rejected pathfinding transitions must be non-negative."
        }
        initialize()
    }

    /** Advances at most one node expansion. */
    fun step(): PathfindingDiagnosticResult = advance(1)

    /** Advances up to [iterations] node expansions and snapshots once. */
    fun advance(iterations: Int): PathfindingDiagnosticResult {
        require(iterations > 0) { "Pathfinding diagnostic iterations must be positive." }
        if (!complete) {
            val started = System.nanoTime()
            repeat(iterations) {
                if (complete) return@repeat
                current?.step()
                advanceCompletedSegment()
            }
            durationNanos += System.nanoTime() - started
        }
        return snapshot()
    }

    /** Completes this diagnostic search synchronously. */
    fun runToCompletion(): PathfindingDiagnosticResult {
        if (!complete) {
            val started = System.nanoTime()
            while (!complete) {
                current?.step()
                advanceCompletedSegment()
            }
            durationNanos += System.nanoTime() - started
        }
        return snapshot()
    }

    /** Restarts the search with its original inputs. */
    fun reset(): PathfindingDiagnosticResult {
        completedSegments.clear()
        segmentIndex = 0
        current = null
        finalStatus = null
        durationNanos = 0L
        initialize()
        return snapshot()
    }

    /** Returns an immutable view of the current search state. */
    fun snapshot(): PathfindingDiagnosticResult {
        val active = current?.result()
        val all = if (active == null) completedSegments else completedSegments + active
        val explored = all.flatMap(PathSearchResult::explored)
        val globalExplorationOrder = mutableMapOf<TilePosition, Int>()
        explored.forEachIndexed { index, position -> globalExplorationOrder[position] = index }
        val nodesByPosition = linkedMapOf<TilePosition, PathfindingNodeDiagnostic>()
        all.forEach { segment ->
            segment.nodes.forEach { node ->
                nodesByPosition[node.position] = node.copy(
                    explorationOrder = if (node.status == PathfindingNodeStatus.CLOSED) {
                        globalExplorationOrder[node.position]
                    } else {
                        null
                    }
                )
            }
        }
        val rejected = all.asSequence()
            .flatMap { it.rejectedTransitions.asSequence() }
            .take(maximumRejectedTransitions)
            .toList()
        val path = if (finalStatus == PathfindingSearchStatus.SUCCEEDED) {
            combinePaths(completedSegments.mapNotNull(PathSearchResult::path))
        } else {
            null
        }
        val totalCost = if (path != null) {
            completedSegments.sumOf { requireNotNull(it.totalCost).toDouble() }.toFloat()
        } else {
            null
        }
        return PathfindingDiagnosticResult(
            start = waypoints.firstOrNull(),
            goal = waypoints.lastOrNull(),
            path = path,
            explored = explored,
            durationNanos = durationNanos,
            waypoints = waypoints,
            totalCost = totalCost,
            nodes = nodesByPosition.values.toList(),
            rejectedTransitions = rejected,
            status = status
        )
    }

    private fun initialize() {
        if (waypoints.isEmpty()) {
            finalStatus = PathfindingSearchStatus.SUCCEEDED
            return
        }
        createCurrent(waypoints.first(), waypoints.getOrElse(1) { waypoints.first() })
        advanceCompletedSegment()
    }

    private fun createCurrent(start: TilePosition, goal: TilePosition) {
        current = PathSearchMachine(
            world = world,
            start = start,
            goal = goal,
            movementMode = movementMode,
            canEnter = canEnter,
            movementCost = movementCost,
            minimumStepCost = minimumStepCost,
            collectDiagnostics = true,
            maximumRejectedTransitions = maximumRejectedTransitions
        )
    }

    private fun advanceCompletedSegment() {
        while (current?.complete == true) {
            val result = requireNotNull(current).result()
            completedSegments += result
            current = null
            if (result.path == null) {
                finalStatus = PathfindingSearchStatus.FAILED
                return
            }
            if (waypoints.size <= 1 || segmentIndex >= waypoints.lastIndex - 1) {
                finalStatus = PathfindingSearchStatus.SUCCEEDED
                return
            }
            segmentIndex++
            createCurrent(waypoints[segmentIndex], waypoints[segmentIndex + 1])
        }
    }

    private fun combinePaths(paths: List<List<TilePosition>>): List<TilePosition> {
        if (paths.isEmpty()) return emptyList()
        val combined = mutableListOf<TilePosition>()
        paths.forEachIndexed { index, path ->
            if (index == 0) combined += path else combined += path.drop(1)
        }
        return combined
    }
}

private class PathSearchMachine(
    private val world: World,
    private val start: TilePosition,
    private val goal: TilePosition,
    private val movementMode: PathMovementMode,
    private val canEnter: (TilePosition) -> Boolean,
    private val movementCost: (TilePosition, TilePosition) -> Float,
    private val minimumStepCost: Float,
    private val collectDiagnostics: Boolean,
    private val maximumRejectedTransitions: Int
) {
    private val open = PriorityQueue(
        compareBy<PathNode> { it.fScore }.thenBy { it.hScore }
    )
    private val cameFrom = mutableMapOf<TilePosition, TilePosition>()
    private val gScore = mutableMapOf<TilePosition, Float>()
    private val closed = mutableSetOf<TilePosition>()
    private val explored = mutableListOf<TilePosition>()
    private val rejected = mutableListOf<PathfindingRejectedTransition>()

    var status: PathfindingSearchStatus = PathfindingSearchStatus.READY
        private set

    val complete: Boolean
        get() = status == PathfindingSearchStatus.SUCCEEDED ||
            status == PathfindingSearchStatus.FAILED

    init {
        require(world.contains(start)) { "Start position $start is outside the world." }
        require(world.contains(goal)) { "Goal position $goal is outside the world." }
        if (!canEnter(start) || !canEnter(goal)) {
            status = PathfindingSearchStatus.FAILED
        } else {
            gScore[start] = 0f
            if (start == goal) {
                closed += start
                explored += start
                status = PathfindingSearchStatus.SUCCEEDED
            } else {
                val h = heuristic(start, goal, movementMode, minimumStepCost)
                open += PathNode(start, h, h)
            }
        }
    }

    fun step() {
        if (complete) return
        status = PathfindingSearchStatus.RUNNING
        var current: TilePosition? = null
        while (open.isNotEmpty() && current == null) {
            val candidate = open.remove().position
            if (closed.add(candidate)) current = candidate
        }
        if (current == null) {
            status = PathfindingSearchStatus.FAILED
            return
        }
        explored += current
        if (current == goal) {
            status = PathfindingSearchStatus.SUCCEEDED
            return
        }

        val currentScore = gScore[current] ?: return
        for (neighbor in world.neighbors(
            current,
            includeDiagonals = movementMode == PathMovementMode.EIGHT_WAY
        )) {
            if (neighbor in closed) {
                reject(current, neighbor, PathfindingTransitionRejection.CLOSED)
                continue
            }
            if (!canEnter(neighbor)) {
                reject(current, neighbor, PathfindingTransitionRejection.BLOCKED)
                continue
            }
            if (isDiagonal(current, neighbor) && !canTraverseDiagonal(current, neighbor, canEnter)) {
                reject(current, neighbor, PathfindingTransitionRejection.DIAGONAL_BLOCKED)
                continue
            }
            val edgeCost = movementCost(current, neighbor)
            require(edgeCost.isFinite() && edgeCost > 0f) {
                "Movement cost from $current to $neighbor must be finite and positive, but was $edgeCost."
            }
            val tentativeScore = currentScore + edgeCost
            if (tentativeScore >= (gScore[neighbor] ?: Float.POSITIVE_INFINITY)) continue
            cameFrom[neighbor] = current
            gScore[neighbor] = tentativeScore
            val h = heuristic(neighbor, goal, movementMode, minimumStepCost)
            open += PathNode(neighbor, tentativeScore + h, h)
        }
    }

    fun result(): PathSearchResult {
        val path = if (status == PathfindingSearchStatus.SUCCEEDED) {
            reconstructPath(cameFrom, goal)
        } else {
            null
        }
        val order = if (collectDiagnostics) {
            explored.withIndex().associate { (index, position) -> position to index }
        } else {
            emptyMap()
        }
        val nodes = if (collectDiagnostics) {
            gScore.map { (position, g) ->
                val h = heuristic(position, goal, movementMode, minimumStepCost)
                PathfindingNodeDiagnostic(
                    position = position,
                    gCost = g,
                    hCost = h,
                    fCost = g + h,
                    parent = cameFrom[position],
                    status = if (position in closed) {
                        PathfindingNodeStatus.CLOSED
                    } else {
                        PathfindingNodeStatus.OPEN
                    },
                    explorationOrder = order[position]
                )
            }
        } else {
            emptyList()
        }
        return PathSearchResult(
            path = path,
            explored = explored.toList(),
            totalCost = path?.let { requireNotNull(gScore[goal]) },
            nodes = nodes,
            rejectedTransitions = if (collectDiagnostics) rejected.toList() else emptyList(),
            status = status
        )
    }

    private fun reject(
        from: TilePosition,
        to: TilePosition,
        reason: PathfindingTransitionRejection
    ) {
        if (collectDiagnostics && rejected.size < maximumRejectedTransitions) {
            rejected += PathfindingRejectedTransition(from, to, reason)
        }
    }
}

private fun heuristic(
    from: TilePosition,
    to: TilePosition,
    movementMode: PathMovementMode,
    minimumStepCost: Float
): Float {
    val dx = abs(from.x - to.x)
    val dy = abs(from.y - to.y)
    return when (movementMode) {
        PathMovementMode.FOUR_WAY -> (dx + dy).toFloat() * minimumStepCost
        PathMovementMode.EIGHT_WAY -> {
            val diagonalSteps = min(dx, dy)
            val orthogonalSteps = maxOf(dx, dy) - diagonalSteps
            (diagonalSteps * DIAGONAL_MOVEMENT_COST + orthogonalSteps) * minimumStepCost
        }
    }
}

private fun isDiagonal(from: TilePosition, to: TilePosition): Boolean =
    from.x != to.x && from.y != to.y

private fun canTraverseDiagonal(
    from: TilePosition,
    to: TilePosition,
    canEnter: (TilePosition) -> Boolean
): Boolean = canEnter(TilePosition(to.x, from.y)) && canEnter(TilePosition(from.x, to.y))

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

private val DIAGONAL_MOVEMENT_COST = sqrt(2f)
