package com.mefabc24.strata.pathfinding

import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.contains
import com.mefabc24.strata.world.neighbors
import java.util.PriorityQueue
import kotlin.math.abs

/**
 * Finds the shortest path between two tile positions.
 *
 * Traversability is defined by the caller so the pathfinder does not
 * depend on game-specific terrain or movement rules.
 *
 * The returned path includes both the start and goal positions.
 * Returns null when no path exists.
 */
fun World.findPath(
    start: TilePosition,
    goal: TilePosition,
    canEnter: (TilePosition) -> Boolean = { true }
): List<TilePosition>? {
    require(contains(start)) {
        "Start position $start is outside the world."
    }

    require(contains(goal)) {
        "Goal position $goal is outside the world."
    }

    if (!canEnter(start) || !canEnter(goal)) {
        return null
    }

    if (start == goal) {
        return listOf(start)
    }

    val open = PriorityQueue(
        compareBy<PathNode> { it.fScore }
            .thenBy { it.hScore }
    )

    val cameFrom =
        mutableMapOf<TilePosition, TilePosition>()

    val gScore =
        mutableMapOf<TilePosition, Int>()

    val closed =
        mutableSetOf<TilePosition>()

    gScore[start] = 0

    val startHeuristic = heuristic(
        start,
        goal
    )

    open.add(
        PathNode(
            position = start,
            fScore = startHeuristic,
            hScore = startHeuristic
        )
    )

    while (open.isNotEmpty()) {
        val current = open.remove().position

        if (!closed.add(current)) {
            continue
        }

        if (current == goal) {
            return reconstructPath(
                cameFrom = cameFrom,
                goal = goal
            )
        }

        val currentScore =
            gScore[current] ?: continue

        for (neighbor in neighbors(current)) {
            if (neighbor in closed) {
                continue
            }

            if (!canEnter(neighbor)) {
                continue
            }

            val tentativeScore =
                currentScore + 1

            val knownScore =
                gScore[neighbor] ?: Int.MAX_VALUE

            if (tentativeScore >= knownScore) {
                continue
            }

            cameFrom[neighbor] = current
            gScore[neighbor] = tentativeScore

            val neighborHeuristic = heuristic(
                neighbor,
                goal
            )

            open.add(
                PathNode(
                    position = neighbor,
                    fScore = tentativeScore + neighborHeuristic,
                    hScore = neighborHeuristic
                )
            )
        }
    }

    return null
}

private data class PathNode(
    val position: TilePosition,
    val fScore: Int,
    val hScore: Int
)

private fun heuristic(
    from: TilePosition,
    to: TilePosition
): Int {
    return abs(from.x - to.x) +
            abs(from.y - to.y)
}

private fun reconstructPath(
    cameFrom: Map<TilePosition, TilePosition>,
    goal: TilePosition
): List<TilePosition> {
    val path = mutableListOf<TilePosition>()

    var current = goal

    path.add(current)

    while (true) {
        val previous =
            cameFrom[current] ?: break

        current = previous
        path.add(current)
    }

    path.reverse()

    return path
}
