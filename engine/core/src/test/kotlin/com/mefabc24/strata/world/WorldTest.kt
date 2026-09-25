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

    @Test
    fun `overlay layers are independent from ground and each other`() {
        val world = World(3, 3) { _, _ -> TestTile(0) }

        world.addOverlayLayer("infrastructure")
        world.addOverlayLayer("decoration")

        assertEquals(
            listOf("infrastructure", "decoration"),
            world.overlayLayerIds
        )

        assertNull(world.getOverlayTile("infrastructure", 1, 1))

        world.setOverlayTile("infrastructure", 1, 1, TestTile(1))
        world.setOverlayTile("decoration", 1, 1, TestTile(2))

        assertEquals(TestTile(0), world.getTile(1, 1))
        assertEquals(
            TestTile(1),
            world.getOverlayTile("infrastructure", 1, 1)
        )
        assertEquals(
            TestTile(2),
            world.getOverlayTile("decoration", 1, 1)
        )

        world.setOverlayTile("infrastructure", 1, 1, null)

        assertNull(world.getOverlayTile("infrastructure", 1, 1))
        assertEquals(
            TestTile(2),
            world.getOverlayTile("decoration", 1, 1)
        )
        assertEquals(TestTile(0), world.getTile(1, 1))
    }

    @Test
    fun `overlay layers reject invalid operations`() {
        val world = World(3, 3) { _, _ -> TestTile(0) }

        assertFailsWith<IllegalArgumentException> {
            world.addOverlayLayer("")
        }

        world.addOverlayLayer("infrastructure")

        assertFailsWith<IllegalArgumentException> {
            world.addOverlayLayer("infrastructure")
        }

        assertFailsWith<IllegalArgumentException> {
            world.getOverlayTile("unknown", 0, 0)
        }

        assertFailsWith<IllegalArgumentException> {
            world.setOverlayTile("infrastructure", 3, 0, TestTile(1))
        }

        assertNull(world.getOverlayTile("infrastructure", -1, 0))
    }

    @Test
    fun `terrain starts at height zero`() {
        val world = World(3, 2) { _, _ -> TestTile(0) }

        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                assertEquals(0, world.getHeight(x, y))
            }
        }
    }

    @Test
    fun `terrain height can be changed independently`() {
        val world = World(3, 3) { _, _ -> TestTile(0) }

        world.setHeight(1, 1, 3)

        assertEquals(3, world.getHeight(1, 1))
        assertEquals(0, world.getHeight(0, 1))
        assertEquals(0, world.getHeight(1, 0))

        world.setTile(1, 1, TestTile(42))

        assertEquals(3, world.getHeight(1, 1))
        assertEquals(TestTile(42), world.getTile(1, 1))
    }

    @Test
    fun `height lookup outside the world returns null`() {
        val world = World(3, 3) { _, _ -> TestTile(0) }

        assertNull(world.getHeight(-1, 0))
        assertNull(world.getHeight(0, -1))
        assertNull(world.getHeight(3, 0))
        assertNull(world.getHeight(0, 3))
    }

    @Test
    fun `invalid terrain heights are rejected`() {
        val world = World(3, 3) { _, _ -> TestTile(0) }

        assertFailsWith<IllegalArgumentException> {
            world.setHeight(1, 1, -1)
        }

        assertFailsWith<IllegalArgumentException> {
            world.setHeight(3, 0, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            world.setHeight(0, -1, 1)
        }

        assertEquals(0, world.getHeight(1, 1))
    }

    @Test
    fun `height version changes only when terrain elevation changes`() {
        val world = World(3, 3) { _, _ ->
            TestTile(0)
        }

        assertEquals(0L, world.heightVersion)

        world.setHeight(1, 1, 2)

        assertEquals(1L, world.heightVersion)

        world.setHeight(1, 1, 2)

        assertEquals(1L, world.heightVersion)

        world.setHeight(1, 1, 1)

        assertEquals(2L, world.heightVersion)
    }

    @Test
    fun `maximum terrain height updates when highest tile is lowered`() {
        val world = World(3, 3) { _, _ ->
            TestTile(0)
        }

        world.setHeight(0, 0, 3)
        world.setHeight(1, 1, 2)

        assertEquals(3, world.maxHeight)

        world.setHeight(0, 0, 1)

        assertEquals(2, world.maxHeight)

        world.setHeight(1, 1, 0)

        assertEquals(1, world.maxHeight)
    }

    @Test
    fun `overlay operations accept tile positions`() {
        val world = World(3, 3) { _, _ ->
            TestTile(0)
        }

        val position = TilePosition(1, 2)

        world.addOverlayLayer("infrastructure")

        world.setOverlayTile(
            layerId = "infrastructure",
            position = position,
            tile = TestTile(42)
        )

        assertEquals(
            TestTile(42),
            world.getOverlayTile(
                layerId = "infrastructure",
                position = position
            )
        )

        world.setOverlayTile(
            layerId = "infrastructure",
            position = position,
            tile = null
        )

        assertNull(
            world.getOverlayTile(
                layerId = "infrastructure",
                position = position
            )
        )
    }
}
