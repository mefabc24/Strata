package com.mefabc24.strata.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldQueriesTest {

    private class TestTile : Tile

    private fun createWorld(): World {
        return World(5, 5) { _, _ ->
            TestTile()
        }
    }

    @Test
    fun `checks whether positions are inside the world`() {
        val world = createWorld()

        assertTrue(world.contains(TilePosition(0, 0)))
        assertTrue(world.contains(TilePosition(4, 4)))

        assertFalse(world.contains(TilePosition(-1, 0)))
        assertFalse(world.contains(TilePosition(5, 0)))
    }

    @Test
    fun `returns edge connected neighbors`() {
        val world = createWorld()

        assertEquals(
            setOf(
                TilePosition(2, 1),
                TilePosition(1, 2),
                TilePosition(3, 2),
                TilePosition(2, 3)
            ),
            world.neighbors(TilePosition(2, 2)).toSet()
        )
    }

    @Test
    fun `neighbors exclude positions outside the world`() {
        val world = createWorld()

        assertEquals(
            setOf(
                TilePosition(1, 0),
                TilePosition(0, 1)
            ),
            world.neighbors(TilePosition(0, 0)).toSet()
        )
    }

    @Test
    fun `returns positions inside rectangular area`() {
        val world = createWorld()

        assertEquals(
            setOf(
                TilePosition(1, 2),
                TilePosition(2, 2),
                TilePosition(1, 3),
                TilePosition(2, 3)
            ),
            world.positionsIn(
                xRange = 1..2,
                yRange = 2..3
            ).toSet()
        )
    }

    @Test
    fun `returns diagonal neighbors when requested`() {
        val world = createWorld()

        assertEquals(
            setOf(
                TilePosition(1, 1),
                TilePosition(2, 1),
                TilePosition(3, 1),
                TilePosition(1, 2),
                TilePosition(3, 2),
                TilePosition(1, 3),
                TilePosition(2, 3),
                TilePosition(3, 3)
            ),
            world.neighbors(
                position = TilePosition(2, 2),
                includeDiagonals = true
            ).toSet()
        )
    }

    @Test
    fun `positions in area ignore positions outside the world`() {
        val world = createWorld()

        assertEquals(
            setOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                TilePosition(0, 1),
                TilePosition(1, 1)
            ),
            world.positionsIn(
                xRange = -1..1,
                yRange = -1..1
            ).toSet()
        )
    }

    @Test
    fun `detects occupied positions`() {
        val world = createWorld()

        val placeable = object : Placeable {
            override val footprint =
                Footprint.square(1)
        }

        val position = TilePosition(2, 2)

        assertFalse(
            world.isOccupied(position)
        )

        world.place(
            placeable = placeable,
            position = position
        )

        assertTrue(
            world.isOccupied(position)
        )
    }

    @Test
    fun `returns unique objects inside area`() {
        val world = createWorld()

        val building = object : Placeable {
            override val footprint =
                Footprint.square(2)
        }

        val tree = object : Placeable {
            override val footprint =
                Footprint.square(1)
        }

        val placedBuilding = requireNotNull(
            world.place(
                placeable = building,
                position = TilePosition(1, 1)
            )
        )

        val placedTree = requireNotNull(
            world.place(
                placeable = tree,
                position = TilePosition(4, 4)
            )
        )

        assertEquals(
            setOf(placedBuilding),
            world.objectsIn(
                xRange = 0..2,
                yRange = 0..2
            )
        )

        assertEquals(
            setOf(
                placedBuilding,
                placedTree
            ),
            world.objectsIn(
                xRange = 0..4,
                yRange = 0..4
            )
        )
    }
}