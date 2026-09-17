package com.mefabc24.strata.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WorldTest {
    private data class TestTile(val id: Int) : Tile

    @Test
    fun `world is initialized with provided tiles`() {
        val world = World(3, 2) { x, y ->
            TestTile(y * 3 + x)
        }

        assertEquals(3, world.width)
        assertEquals(2, world.height)
        assertEquals(TestTile(0), world.getTile(0, 0))
        assertEquals(TestTile(5), world.getTile(2, 1))
    }

    @Test
    fun `tile can be replaced`() {
        val world = World(3, 3) { _, _ ->
            TestTile(0)
        }

        world.setTile(1, 2, TestTile(42))

        assertEquals(TestTile(42), world.getTile(1, 2))
    }

    @Test
    fun `getting tile outside world returns null`() {
        val world = World(3, 3) { _, _ ->
            TestTile(0)
        }

        assertNull(world.getTile(-1, 0))
        assertNull(world.getTile(0, -1))
        assertNull(world.getTile(3, 0))
        assertNull(world.getTile(0, 3))
    }

    @Test
    fun `world dimensions must be positive`() {
        assertFailsWith<IllegalArgumentException> {
            World(0, 10) { _, _ -> TestTile(0) }
        }

        assertFailsWith<IllegalArgumentException> {
            World(10, 0) { _, _ -> TestTile(0) }
        }
    }
}