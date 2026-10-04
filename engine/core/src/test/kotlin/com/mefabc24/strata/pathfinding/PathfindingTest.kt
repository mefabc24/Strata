package com.mefabc24.strata.pathfinding

import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.neighbors
import kotlin.math.sqrt
import java.util.PriorityQueue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathfindingTest {

    private class TestTile : Tile

    private fun createWorld(
        width: Int = 5,
        height: Int = 5
    ): World {
        return World(width, height) { _, _ ->
            TestTile()
        }
    }

    @Test
    fun `finds shortest path through open world`() {
        val world = createWorld()

        val path = world.findPath(
            start = TilePosition(0, 0),
            goal = TilePosition(3, 2)
        )

        requireNotNull(path)

        assertEquals(
            TilePosition(0, 0),
            path.first()
        )

        assertEquals(
            TilePosition(3, 2),
            path.last()
        )

        assertEquals(
            6,
            path.size
        )
    }

    @Test
    fun `routes around blocked tiles`() {
        val world = createWorld()

        val blocked = setOf(
            TilePosition(2, 1),
            TilePosition(2, 2),
            TilePosition(2, 3)
        )

        val path = world.findPath(
            start = TilePosition(0, 2),
            goal = TilePosition(4, 2)
        ) { position ->
            position !in blocked
        }

        requireNotNull(path)

        assertEquals(
            TilePosition(0, 2),
            path.first()
        )

        assertEquals(
            TilePosition(4, 2),
            path.last()
        )

        assertEquals(
            9,
            path.size
        )

        assertTrue(
            path.none { it in blocked }
        )
    }

    @Test
    fun `returns null when no path exists`() {
        val world = createWorld()

        val blocked = setOf(
            TilePosition(2, 0),
            TilePosition(2, 1),
            TilePosition(2, 2),
            TilePosition(2, 3),
            TilePosition(2, 4)
        )

        val path = world.findPath(
            start = TilePosition(0, 2),
            goal = TilePosition(4, 2)
        ) { position ->
            position !in blocked
        }

        assertNull(path)
    }

    @Test
    fun `returns start when start equals goal`() {
        val world = createWorld()

        val position = TilePosition(2, 2)

        assertEquals(
            listOf(position),
            world.findPath(
                start = position,
                goal = position
            )
        )
    }

    @Test
    fun `returns null when goal cannot be entered`() {
        val world = createWorld()

        val goal = TilePosition(4, 4)

        val path = world.findPath(
            start = TilePosition(0, 0),
            goal = goal
        ) { position ->
            position != goal
        }

        assertNull(path)
    }

    @Test
    fun `rejects positions outside the world`() {
        val world = createWorld()

        assertFailsWith<IllegalArgumentException> {
            world.findPath(
                start = TilePosition(-1, 0),
                goal = TilePosition(4, 4)
            )
        }

        assertFailsWith<IllegalArgumentException> {
            world.findPath(
                start = TilePosition(0, 0),
                goal = TilePosition(5, 4)
            )
        }
    }

    @Test
    fun `weighted path prefers a cheaper longer route`() {
        val world = createWorld(width = 5, height = 3)
        val start = TilePosition(0, 1)
        val goal = TilePosition(4, 1)

        val path = world.findPath(
            start = start,
            goal = goal,
            canEnter = { true },
            movementCost = { _, to ->
                if (to.y == 1 && to.x in 1..3) 3f else 1f
            }
        )

        requireNotNull(path)
        assertTrue(path.size > 5)
        assertTrue(path.none { it.y == 1 && it.x in 1..3 })
    }

    @Test
    fun `default movement cost remains one`() {
        val result = createWorld().findPathDiagnostic(
            start = TilePosition(0, 0),
            goal = TilePosition(3, 0)
        )

        assertEquals(3f, result.totalCost)
        assertEquals(4, result.path?.size)
    }

    @Test
    fun `invalid movement costs are rejected`() {
        val world = createWorld()
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                world.findPath(
                    start = TilePosition(0, 0),
                    goal = TilePosition(1, 0),
                    canEnter = { true },
                    movementCost = { _, _ -> invalid }
                )
            }
        }
    }

    @Test
    fun `multi-waypoint path is continuous without duplicate boundaries`() {
        val waypoints = listOf(
            TilePosition(0, 0),
            TilePosition(2, 0),
            TilePosition(2, 2)
        )

        val path = requireNotNull(createWorld().findPath(waypoints))

        assertEquals(
            listOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                TilePosition(2, 0),
                TilePosition(2, 1),
                TilePosition(2, 2)
            ),
            path
        )
        assertEquals(1, path.count { it == waypoints[1] })
    }

    @Test
    fun `multi-waypoint path visits mandatory waypoints in order`() {
        val waypoints = listOf(
            TilePosition(0, 1),
            TilePosition(3, 1),
            TilePosition(1, 1)
        )

        val path = requireNotNull(createWorld().findPath(waypoints))
        var previousIndex = -1
        val indices = waypoints.map { waypoint ->
            path.withIndex().first {
                it.index > previousIndex && it.value == waypoint
            }.index.also { previousIndex = it }
        }

        assertTrue(indices.zipWithNext().all { (first, second) -> first < second })
    }

    @Test
    fun `multi-waypoint path fails when one segment is blocked`() {
        val world = createWorld()
        val blocked = setOf(
            TilePosition(2, 0),
            TilePosition(2, 1),
            TilePosition(2, 2),
            TilePosition(2, 3),
            TilePosition(2, 4)
        )

        assertNull(
            world.findPath(
                waypoints = listOf(
                    TilePosition(0, 0),
                    TilePosition(1, 0),
                    TilePosition(4, 0)
                ),
                canEnter = { it !in blocked }
            )
        )
    }

    @Test
    fun `multi-waypoint diagnostics combine path and cost`() {
        val waypoints = listOf(
            TilePosition(0, 0),
            TilePosition(2, 0),
            TilePosition(2, 2)
        )

        val result = createWorld().findPathDiagnostic(
            waypoints = waypoints,
            canEnter = { true },
            movementCost = { _, _ -> 2f }
        )

        assertTrue(result.success)
        assertEquals(waypoints, result.waypoints)
        assertEquals(5, result.path?.size)
        assertEquals(8f, result.totalCost)
        assertTrue(result.explored.isNotEmpty())
    }

    @Test
    fun `empty and single waypoint searches are handled cleanly`() {
        val world = createWorld()
        val point = TilePosition(1, 1)

        assertEquals(emptyList(), world.findPath(emptyList()))
        assertEquals(listOf(point), world.findPath(listOf(point)))
        assertEquals(0f, world.findPathDiagnostic(emptyList()).totalCost)
    }

    @Test
    fun `empty waypoint list returns empty path`() {
        val world = createWorld()

        val path = world.findPath(
            waypoints = emptyList()
        )

        assertEquals(
            emptyList(),
            path
        )
    }

    @Test
    fun `single waypoint returns that waypoint`() {
        val world = createWorld()
        val waypoint = TilePosition(2, 2)

        val path = world.findPath(
            waypoints = listOf(waypoint)
        )

        assertEquals(
            listOf(waypoint),
            path
        )
    }

    @Test
    fun `consecutive duplicate waypoints do not duplicate path tiles`() {
        val world = createWorld(
            width = 5,
            height = 1
        )

        val path = world.findPath(
            waypoints = listOf(
                TilePosition(0, 0),
                TilePosition(0, 0),
                TilePosition(2, 0),
                TilePosition(2, 0),
                TilePosition(4, 0)
            )
        )

        assertEquals(
            listOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                TilePosition(2, 0),
                TilePosition(3, 0),
                TilePosition(4, 0)
            ),
            path
        )
    }

    @Test
    fun `multi waypoint path may revisit an earlier waypoint`() {
        val world = createWorld(
            width = 3,
            height = 1
        )

        val start = TilePosition(0, 0)
        val end = TilePosition(2, 0)

        val path = world.findPath(
            waypoints = listOf(
                start,
                end,
                start
            )
        )

        assertEquals(
            listOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                TilePosition(2, 0),
                TilePosition(1, 0),
                TilePosition(0, 0)
            ),
            path
        )
    }

    @Test
    fun `movement costs may differ by traversal direction`() {
        val world = createWorld(
            width = 3,
            height = 2
        )

        val left = TilePosition(0, 0)
        val right = TilePosition(2, 0)
        val expensiveFrom = TilePosition(0, 0)
        val expensiveTo = TilePosition(1, 0)

        val movementCost = { from: TilePosition, to: TilePosition ->
            if (from == expensiveFrom && to == expensiveTo) {
                10f
            } else {
                1f
            }
        }

        val forward = requireNotNull(
            world.findPath(
                start = left,
                goal = right,
                movementCost = movementCost
            )
        )

        val backward = requireNotNull(
            world.findPath(
                start = right,
                goal = left,
                movementCost = movementCost
            )
        )

        assertEquals(left, forward.first())
        assertEquals(right, forward.last())

        assertTrue(
            forward.zipWithNext().none { (from, to) ->
                from == expensiveFrom && to == expensiveTo
            }
        )

        assertEquals(
            listOf(
                TilePosition(2, 0),
                TilePosition(1, 0),
                TilePosition(0, 0)
            ),
            backward
        )
    }

    @Test
    fun `movement cost is not evaluated for blocked neighbors`() {
        val world = createWorld(
            width = 3,
            height = 3
        )

        val blocked = TilePosition(1, 1)

        val path = world.findPath(
            start = TilePosition(0, 1),
            goal = TilePosition(2, 1),
            canEnter = { position ->
                position != blocked
            },
            movementCost = { _, to ->
                check(to != blocked) {
                    "Movement cost must not be evaluated for blocked tiles."
                }

                1f
            }
        )

        requireNotNull(path)

        assertTrue(
            blocked !in path
        )
    }

    @Test
    fun `four way movement remains the default`() {
        val world = createWorld(width = 3, height = 3)
        val start = TilePosition(0, 0)
        val goal = TilePosition(2, 2)

        val defaultPath = world.findPath(start, goal)
        val fourWayPath = world.findPath(start, goal, PathMovementMode.FOUR_WAY)

        assertEquals(fourWayPath, defaultPath)
        assertEquals(5, defaultPath?.size)
    }

    @Test
    fun `eight way movement finds a shorter open diagonal route`() {
        val path = createWorld(width = 3, height = 3).findPath(
            start = TilePosition(0, 0),
            goal = TilePosition(2, 2),
            movementMode = PathMovementMode.EIGHT_WAY
        )

        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(1, 1), TilePosition(2, 2)),
            path
        )
    }

    @Test
    fun `eight way movement respects world boundaries`() {
        val world = createWorld(width = 2, height = 3)
        val path = requireNotNull(
            world.findPath(
                start = TilePosition(0, 0),
                goal = TilePosition(1, 2),
                movementMode = PathMovementMode.EIGHT_WAY
            )
        )

        assertTrue(path.all { it.x in 0 until world.width && it.y in 0 until world.height })
        assertEquals(3, path.size)
    }

    @Test
    fun `eight way movement cannot cut through a blocked corner`() {
        val blocked = TilePosition(1, 0)
        val path = createWorld(width = 2, height = 2).findPath(
            start = TilePosition(0, 0),
            goal = TilePosition(1, 1),
            movementMode = PathMovementMode.EIGHT_WAY,
            canEnter = { it != blocked }
        )

        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(0, 1), TilePosition(1, 1)),
            path
        )
    }

    @Test
    fun `default diagonal movement cost is square root of two`() {
        val result = createWorld(width = 2, height = 2).findPathDiagnostic(
            start = TilePosition(0, 0),
            goal = TilePosition(1, 1),
            movementMode = PathMovementMode.EIGHT_WAY
        )

        assertEquals(sqrt(2f), result.totalCost)
    }

    @Test
    fun `weighted eight way movement remains optimal for arbitrary costs`() {
        val result = createWorld(width = 3, height = 3).findPathDiagnostic(
            start = TilePosition(0, 0),
            goal = TilePosition(2, 2),
            movementMode = PathMovementMode.EIGHT_WAY,
            movementCost = { from, to ->
                if (from.x != to.x && from.y != to.y) 10f else 1f
            }
        )

        assertEquals(4f, result.totalCost)
        assertTrue(
            requireNotNull(result.path).zipWithNext().all { (from, to) ->
                from.x == to.x || from.y == to.y
            }
        )
    }

    @Test
    fun `multi waypoint and diagnostic searches respect eight way movement`() {
        val waypoints = listOf(
            TilePosition(0, 0),
            TilePosition(1, 1),
            TilePosition(2, 0)
        )
        val world = createWorld(width = 3, height = 2)

        val path = world.findPath(waypoints, PathMovementMode.EIGHT_WAY)
        val diagnostic = world.findPathDiagnostic(
            waypoints,
            PathMovementMode.EIGHT_WAY
        )

        assertEquals(waypoints, path)
        assertEquals(waypoints, diagnostic.path)
        assertEquals(2f * sqrt(2f), diagnostic.totalCost)
        assertTrue(diagnostic.explored.isNotEmpty())
    }

    @Test
    fun `fixed seed blocked maps match independent shortest path oracle`() {
        val random = Random(246813579)
        val world = createWorld(width = 8, height = 7)

        repeat(60) { case ->
            val start = TilePosition(random.nextInt(world.width), random.nextInt(world.height))
            val goal = TilePosition(random.nextInt(world.width), random.nextInt(world.height))
            val blocked = buildSet {
                for (y in 0 until world.height) {
                    for (x in 0 until world.width) {
                        val position = TilePosition(x, y)
                        if (position != start && position != goal && random.nextFloat() < 0.28f) {
                            add(position)
                        }
                    }
                }
            }

            for (mode in PathMovementMode.entries) {
                val result = world.findPathDiagnostic(start, goal, mode) { it !in blocked }
                val expected = oracleCost(world, start, goal, mode, blocked) { from, to ->
                    if (from.x != to.x && from.y != to.y) sqrt(2f) else 1f
                }

                assertEquals(expected == null, result.path == null, "case=$case mode=$mode")
                if (expected != null) {
                    assertEquals(expected, result.totalCost!!, 0.0001f, "case=$case mode=$mode")
                    assertValidPath(requireNotNull(result.path), start, goal, mode, blocked)
                }
            }
        }
    }

    @Test
    fun `fixed seed weighted searches match independent dijkstra oracle`() {
        val random = Random(97531)
        val world = createWorld(width = 7, height = 6)

        repeat(40) { case ->
            val start = TilePosition(random.nextInt(world.width), random.nextInt(world.height))
            val goal = TilePosition(random.nextInt(world.width), random.nextInt(world.height))
            val blocked = buildSet {
                world.forEachTile { position, _ ->
                    if (position != start && position != goal && random.nextFloat() < 0.2f) {
                        add(position)
                    }
                }
            }
            val edgeCost = { from: TilePosition, to: TilePosition ->
                0.25f + ((from.x * 13 + from.y * 7 + to.x * 5 + to.y * 3) and 7) / 4f
            }
            val result = world.findPathDiagnostic(
                start,
                goal,
                PathMovementMode.EIGHT_WAY,
                canEnter = { it !in blocked },
                movementCost = edgeCost
            )
            val expected = oracleCost(
                world,
                start,
                goal,
                PathMovementMode.EIGHT_WAY,
                blocked,
                edgeCost
            )

            assertEquals(expected == null, result.path == null, "case=$case")
            if (expected != null) {
                assertEquals(expected, result.totalCost!!, 0.0001f, "case=$case")
            }
        }
    }

    @Test
    fun `blocked start and blocked degenerate search return no path`() {
        val world = createWorld()
        val start = TilePosition(2, 2)

        assertNull(world.findPath(start, TilePosition(3, 2)) { it != start })
        assertNull(world.findPath(start, start) { false })
    }

    @Test
    fun `single waypoint still validates bounds and traversability`() {
        val world = createWorld()
        val point = TilePosition(2, 2)

        assertNull(world.findPath(listOf(point)) { false })
        assertFailsWith<IllegalArgumentException> {
            world.findPath(listOf(TilePosition(-1, 2)))
        }
    }

    @Test
    fun `failed diagnostics report explored nodes without a cost`() {
        val world = createWorld(width = 4, height = 3)
        val wall = (0 until world.height).map { TilePosition(2, it) }.toSet()

        val result = world.findPathDiagnostic(
            TilePosition(0, 1),
            TilePosition(3, 1),
            canEnter = { it !in wall }
        )

        assertTrue(!result.success)
        assertNull(result.path)
        assertNull(result.totalCost)
        assertTrue(result.explored.isNotEmpty())
        assertTrue(result.explored.none { it in wall })
    }

    @Test
    fun `diagnostics snapshot mutable waypoint input`() {
        val waypoints = mutableListOf(TilePosition(0, 0), TilePosition(2, 0))
        val result = createWorld().findPathDiagnostic(waypoints)

        waypoints += TilePosition(4, 4)

        assertEquals(listOf(TilePosition(0, 0), TilePosition(2, 0)), result.waypoints)
        assertEquals(TilePosition(2, 0), result.goal)
    }

    private fun assertValidPath(
        path: List<TilePosition>,
        start: TilePosition,
        goal: TilePosition,
        mode: PathMovementMode,
        blocked: Set<TilePosition>
    ) {
        assertEquals(start, path.first())
        assertEquals(goal, path.last())
        assertTrue(path.none { it in blocked })
        path.zipWithNext().forEach { (from, to) ->
            val dx = kotlin.math.abs(from.x - to.x)
            val dy = kotlin.math.abs(from.y - to.y)
            assertTrue(dx <= 1 && dy <= 1 && dx + dy > 0)
            if (mode == PathMovementMode.FOUR_WAY) assertEquals(1, dx + dy)
            if (dx == 1 && dy == 1) {
                assertTrue(TilePosition(to.x, from.y) !in blocked)
                assertTrue(TilePosition(from.x, to.y) !in blocked)
            }
        }
    }

    private fun oracleCost(
        world: World,
        start: TilePosition,
        goal: TilePosition,
        mode: PathMovementMode,
        blocked: Set<TilePosition>,
        edgeCost: (TilePosition, TilePosition) -> Float
    ): Float? {
        val costs = mutableMapOf(start to 0f)
        val queue = PriorityQueue<Pair<TilePosition, Float>>(compareBy { it.second })
        queue += start to 0f
        while (queue.isNotEmpty()) {
            val (current, cost) = queue.remove()
            if (cost != costs[current]) continue
            if (current == goal) return cost
            for (next in world.neighbors(current, mode == PathMovementMode.EIGHT_WAY)) {
                if (next in blocked) continue
                val diagonal = current.x != next.x && current.y != next.y
                if (diagonal && (
                        TilePosition(next.x, current.y) in blocked ||
                            TilePosition(current.x, next.y) in blocked
                        )
                ) continue
                val candidate = cost + edgeCost(current, next)
                if (candidate < (costs[next] ?: Float.POSITIVE_INFINITY)) {
                    costs[next] = candidate
                    queue += next to candidate
                }
            }
        }
        return null
    }
}
