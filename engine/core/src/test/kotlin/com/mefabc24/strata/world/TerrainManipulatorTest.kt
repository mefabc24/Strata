package com.mefabc24.strata.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TerrainManipulatorTest {

    private data class TestTile(val id: Int = 0) : Tile

    @Test
    fun `replaces a single ground tile`() {
        val world = createWorld()

        world.terrain.setTile(2, 3, TestTile(42))

        assertEquals(TestTile(42), world.getTile(2, 3))
    }

    @Test
    fun `single tile operations accept tile positions`() {
        val world = createWorld()
        val position = TilePosition(2, 3)

        world.terrain.setTile(position, TestTile(42))

        assertEquals(TestTile(42), world.getTile(position))
    }

    @Test
    fun `fills a rectangular ground area`() {
        val world = createWorld()

        world.terrain.fill(2..4, 3..5, TestTile(42))

        for (y in 3..5) {
            for (x in 2..4) {
                assertEquals(TestTile(42), world.getTile(x, y))
            }
        }
        assertEquals(TestTile(), world.getTile(1, 3))
    }

    @Test
    fun `fills terrain using world coordinates`() {
        val world = createWorld()

        world.terrain.fill(2..3, 4..5) { x, y ->
            TestTile(x + y * 10)
        }

        assertEquals(TestTile(42), world.getTile(2, 4))
        assertEquals(TestTile(53), world.getTile(3, 5))
    }

    @Test
    fun `terrain changes reject positions outside the world`() {
        val world = createWorld()

        assertFailsWith<IllegalArgumentException> {
            world.terrain.setTile(-1, 0, TestTile(1))
        }
        assertFailsWith<IllegalArgumentException> {
            world.terrain.fill(0..10, 0..1, TestTile(1))
        }
    }

    private fun createWorld(): World {
        return World(10, 10) { _, _ -> TestTile() }
    }
}
