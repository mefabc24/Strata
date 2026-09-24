package com.mefabc24.strata.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerrainManipulatorTest {

    private data class TestTile(
        val id: Int = 0
    ) : Tile

    private fun createWorld(): World {
        return World(10, 10) { _, _ ->
            TestTile()
        }
    }

    private fun createObject(
        footprint: Footprint,
        x: Int,
        y: Int
    ): PlacedObject {
        val placeable = object : Placeable {
            override val footprint = footprint
        }

        return PlacedObject(
            placeable = placeable,
            x = x,
            y = y
        )
    }

    @Test
    fun `raises and lowers a single tile`() {
        val world = createWorld()

        assertTrue(world.terrain.raise(2, 3))
        assertEquals(1, world.getHeight(2, 3))

        assertTrue(world.terrain.raise(2, 3, amount = 2))
        assertEquals(3, world.getHeight(2, 3))

        assertTrue(world.terrain.lower(2, 3))
        assertEquals(2, world.getHeight(2, 3))
    }

    @Test
    fun `cannot lower terrain below zero`() {
        val world = createWorld()

        assertFalse(world.terrain.lower(2, 3))

        assertEquals(0, world.getHeight(2, 3))
        assertEquals(0L, world.heightVersion)
    }

    @Test
    fun `modifies rectangular terrain atomically`() {
        val world = createWorld()

        assertTrue(
            world.terrain.raise(
                xRange = 2..4,
                yRange = 3..5
            )
        )

        for (y in 3..5) {
            for (x in 2..4) {
                assertEquals(
                    1,
                    world.getHeight(x, y)
                )
            }
        }

        assertEquals(1L, world.heightVersion)
        assertEquals(1, world.maxHeight)
    }

    @Test
    fun `rejects partial elevation changes below an object footprint`() {
        val world = createWorld()

        val building = createObject(
            footprint = Footprint.square(2),
            x = 4,
            y = 4
        )

        assertTrue(world.placeObject(building))

        assertFalse(
            world.terrain.raise(
                x = 4,
                y = 4
            )
        )

        for ((x, y) in building.occupiedTiles()) {
            assertEquals(
                0,
                world.getHeight(x, y)
            )
        }

        assertEquals(0L, world.heightVersion)
    }

    @Test
    fun `moves an object with its complete terrain footprint`() {
        val world = createWorld()

        val building = createObject(
            footprint = Footprint.square(2),
            x = 4,
            y = 4
        )

        assertTrue(world.placeObject(building))

        assertTrue(
            world.terrain.raise(
                xRange = 4..5,
                yRange = 4..5
            )
        )

        for ((x, y) in building.occupiedTiles()) {
            assertEquals(
                1,
                world.getHeight(x, y)
            )
        }

        assertEquals(1L, world.heightVersion)
    }

    @Test
    fun `rejects an entire area when one object would become uneven`() {
        val world = createWorld()

        val building = createObject(
            footprint = Footprint.square(2),
            x = 4,
            y = 4
        )

        assertTrue(world.placeObject(building))

        assertFalse(
            world.terrain.raise(
                xRange = 3..4,
                yRange = 3..4
            )
        )

        for (y in 3..4) {
            for (x in 3..4) {
                assertEquals(
                    0,
                    world.getHeight(x, y)
                )
            }
        }

        assertEquals(0L, world.heightVersion)
    }

    @Test
    fun `replaces a single ground tile`() {
        val world = createWorld()

        world.terrain.setTile(
            x = 2,
            y = 3,
            tile = TestTile(42)
        )

        assertEquals(
            TestTile(42),
            world.getTile(2, 3)
        )
    }

    @Test
    fun `fills a rectangular ground area`() {
        val world = createWorld()

        world.terrain.fill(
            xRange = 2..4,
            yRange = 3..5,
            tile = TestTile(42)
        )

        for (y in 3..5) {
            for (x in 2..4) {
                assertEquals(
                    TestTile(42),
                    world.getTile(x, y)
                )
            }
        }

        assertEquals(
            TestTile(),
            world.getTile(1, 3)
        )
    }

    @Test
    fun `fills terrain using world coordinates`() {
        val world = createWorld()

        world.terrain.fill(
            xRange = 2..3,
            yRange = 4..5
        ) { x, y ->
            TestTile(x + y * 10)
        }

        assertEquals(
            TestTile(42),
            world.getTile(2, 4)
        )

        assertEquals(
            TestTile(53),
            world.getTile(3, 5)
        )
    }

    @Test
    fun `single tile operations accept tile positions`() {
        val world = createWorld()
        val position = TilePosition(2, 3)

        world.terrain.setTile(
            position = position,
            tile = TestTile(42)
        )

        assertEquals(
            TestTile(42),
            world.getTile(position)
        )

        assertTrue(
            world.terrain.setHeight(
                position = position,
                level = 2
            )
        )

        assertEquals(
            2,
            world.getHeight(position)
        )

        assertTrue(
            world.terrain.raise(position)
        )

        assertEquals(
            3,
            world.getHeight(position)
        )

        assertTrue(
            world.terrain.lower(position)
        )

        assertEquals(
            2,
            world.getHeight(position)
        )
    }
}