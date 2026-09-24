package com.mefabc24.strata.pathfinding

import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
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
}