package com.mefabc24.strata.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WorldTest {
    private data class TestTile(val id: Int) : Tile

    private data class GameTile(
        val terrain: String,
        val elevation: Int
    ) : Tile

    @Test
    fun `world is initialized with provided tiles`() {
        val world = World(3, 2) { x, y ->
            TestTile(y * 3 + x)
        }

        assertEquals(3, world.width)
        assertEquals(2, world.height)
        assertEquals(TestTile(0), world.getTile(0, 0))
        assertEquals(TestTile(5), world.getTile(2, 1))
        assertEquals(6, world.groundTileCount)
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
    fun `game specific tile metadata remains opaque to the world`() {
        val world = World(2, 1) { x, _ ->
            GameTile(
                terrain = "grass",
                elevation = x * 5
            )
        }
        val placeable = object : Placeable {
            override val footprint = Footprint.rectangle(2, 1)
        }

        assertEquals(0, (world.getTile(0, 0) as GameTile).elevation)
        assertEquals(5, (world.getTile(1, 0) as GameTile).elevation)
        assertNotNull(world.place(placeable, 0, 0))
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

        assertEquals(2, world.overlayTileCount)

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

        assertEquals(1, world.overlayTileCount)

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

    @Test
    fun `world exposes all ground tiles in row major order`() {
        val world = World(3, 2) { x, y ->
            TestTile(y * 3 + x)
        }

        val visited =
            mutableListOf<Pair<TilePosition, Tile>>()

        world.forEachTile { position, tile ->
            visited += position to tile
        }

        assertEquals(
            listOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                TilePosition(2, 0),
                TilePosition(0, 1),
                TilePosition(1, 1),
                TilePosition(2, 1)
            ),
            visited.map { it.first }
        )

        assertEquals(
            listOf(0, 1, 2, 3, 4, 5),
            visited.map {
                (it.second as TestTile).id
            }
        )
    }

    @Test
    fun `world exposes every overlay cell including empty cells`() {
        val world = World(2, 2) { _, _ ->
            TestTile(0)
        }

        world.addOverlayLayer("roads")

        world.setOverlayTile(
            layerId = "roads",
            position = TilePosition(1, 0),
            tile = TestTile(42)
        )

        val visited =
            mutableListOf<Pair<TilePosition, Tile?>>()

        world.forEachOverlayTile("roads") { position, tile ->
            visited += position to tile
        }

        assertEquals(
            listOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                TilePosition(0, 1),
                TilePosition(1, 1)
            ),
            visited.map { it.first }
        )

        assertNull(visited[0].second)
        assertEquals(TestTile(42), visited[1].second)
        assertNull(visited[2].second)
        assertNull(visited[3].second)
    }
}
