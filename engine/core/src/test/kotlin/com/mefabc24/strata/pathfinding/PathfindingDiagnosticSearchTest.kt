package com.mefabc24.strata.pathfinding

import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathfindingDiagnosticSearchTest {
    @Test
    fun `stepped and immediate searches match normal four and eight way paths`() {
        val world = world(6, 5)
        val start = TilePosition(0, 2)
        val goal = TilePosition(5, 2)
        val blocked = setOf(TilePosition(2, 1), TilePosition(2, 2), TilePosition(2, 3))

        PathMovementMode.entries.forEach { mode ->
            val normal = world.findPath(start, goal, mode) { it !in blocked }
            val immediate = world.findPathDiagnostic(start, goal, mode) { it !in blocked }
            val search = world.createPathfindingDiagnosticSearch(
                start,
                goal,
                mode,
                canEnter = { it !in blocked }
            )

            assertEquals(PathfindingSearchStatus.READY, search.status)
            while (!search.complete) search.step()
            val stepped = search.snapshot()

            assertEquals(normal, immediate.path, mode.name)
            assertEquals(normal, stepped.path, mode.name)
            assertEquals(immediate.totalCost, stepped.totalCost, mode.name)
            assertEquals(immediate.explored, stepped.explored, mode.name)
            assertEquals(PathfindingSearchStatus.SUCCEEDED, stepped.status)
            assertTrue(stepped.goalReached)
            assertEquals(stepped.explored.size, stepped.closedSetSize)
            assertTrue(stepped.nodes.all { node ->
                node.fCost == node.gCost + node.hCost
            })
        }
    }

    @Test
    fun `stepped weighted searches match normal searches in both movement modes`() {
        val world = world(5, 4)
        val start = TilePosition(0, 1)
        val goal = TilePosition(4, 1)
        val edgeCost = { from: TilePosition, to: TilePosition ->
            if (to.y == 1 && to.x in 1..3) 4f
            else 0.5f + ((from.x + from.y + to.x * 2 + to.y) % 3)
        }

        PathMovementMode.entries.forEach { mode ->
            val normal = world.findPath(start, goal, mode, { true }, edgeCost)
            val diagnostic = world.findPathDiagnostic(start, goal, mode, { true }, edgeCost)
            val stepped = world.createPathfindingDiagnosticSearch(
                start,
                goal,
                mode,
                canEnter = { true },
                movementCost = edgeCost
            ).runToCompletion()

            assertEquals(normal, diagnostic.path, mode.name)
            assertEquals(normal, stepped.path, mode.name)
            assertEquals(diagnostic.totalCost, stepped.totalCost, mode.name)
            assertTrue(requireNotNull(stepped.path).none {
                it.y == 1 && it.x in 1..3
            })
        }
    }

    @Test
    fun `multi waypoint stepping uses the same segment behavior`() {
        val world = world(5, 4)
        val waypoints = listOf(
            TilePosition(0, 0),
            TilePosition(2, 2),
            TilePosition(4, 0)
        )
        val normal = world.findPath(waypoints, PathMovementMode.EIGHT_WAY)
        val search = world.createPathfindingDiagnosticSearch(
            waypoints,
            PathMovementMode.EIGHT_WAY
        )

        val result = search.runToCompletion()

        assertEquals(normal, result.path)
        assertEquals(waypoints, result.waypoints)
        assertEquals(1, result.path?.count { it == waypoints[1] })
        assertTrue(result.nodes.isNotEmpty())
    }

    @Test
    fun `rejected transitions are classified and bounded`() {
        val blocked = TilePosition(1, 0)
        val search = world(3, 3).createPathfindingDiagnosticSearch(
            start = TilePosition(0, 0),
            goal = TilePosition(2, 2),
            movementMode = PathMovementMode.EIGHT_WAY,
            canEnter = { it != blocked },
            maximumRejectedTransitions = 2
        )

        val result = search.runToCompletion()

        assertTrue(result.success)
        assertEquals(2, result.rejectedTransitions.size)
        assertTrue(result.rejectedTransitions.any {
            it.to == blocked && it.reason == PathfindingTransitionRejection.BLOCKED
        })
        assertTrue(result.rejectedTransitions.any {
            it.reason == PathfindingTransitionRejection.DIAGONAL_BLOCKED
        })
    }

    @Test
    fun `reset restores the initial open set and clears completed state`() {
        val search = world(4, 1).createPathfindingDiagnosticSearch(
            TilePosition(0, 0),
            TilePosition(3, 0)
        )
        search.advance(2)
        assertEquals(PathfindingSearchStatus.RUNNING, search.status)
        assertTrue(search.snapshot().closedSetSize > 0)

        val reset = search.reset()

        assertEquals(PathfindingSearchStatus.READY, reset.status)
        assertEquals(1, reset.openSetSize)
        assertEquals(0, reset.closedSetSize)
        assertTrue(reset.explored.isEmpty())
        assertNull(reset.path)
        assertFalse(reset.goalReached)
    }

    @Test
    fun `failed stepped search reports closed nodes without a path`() {
        val wall = setOf(TilePosition(1, 0), TilePosition(1, 1), TilePosition(1, 2))
        val result = world(3, 3).createPathfindingDiagnosticSearch(
            TilePosition(0, 1),
            TilePosition(2, 1),
            canEnter = { it !in wall }
        ).runToCompletion()

        assertEquals(PathfindingSearchStatus.FAILED, result.status)
        assertFalse(result.goalReached)
        assertNull(result.path)
        assertNull(result.totalCost)
        assertTrue(result.closedSetSize > 0)
    }

    private fun world(width: Int, height: Int) = World(width, height) { _, _ -> TestTile }

    private data object TestTile : Tile
}
