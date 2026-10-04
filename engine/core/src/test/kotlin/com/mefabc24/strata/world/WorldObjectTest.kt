package com.mefabc24.strata.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WorldObjectTest {

    private class TestTile : Tile

    private fun createWorld(
        width: Int = 10,
        height: Int = 10
    ): World {
        return World(width, height) { _, _ -> TestTile() }
    }

    private fun createObject(
        footprint: Footprint,
        x: Int,
        y: Int
    ): PlacedObject {
        val placeable = object : Placeable {
            override val footprint = footprint
        }

        return PlacedObject(placeable, x, y)
    }

    @Test
    fun `places an object on free tiles`() {
        val world = createWorld()
        val house = createObject(Footprint.square(2), 4, 4)

        assertTrue(world.canPlaceObject(house))
        assertTrue(world.placeObject(house))

        for (x in 4..5) {
            for (y in 4..5) {
                assertSame(house, world.getObjectAt(x, y))
            }
        }

        assertEquals(1, world.getObjects().size)
        assertEquals(1, world.placedObjectCount)
    }

    @Test
    fun `rejects overlapping objects without changing occupancy`() {
        val world = createWorld()

        val house = createObject(Footprint.square(2), 4, 4)
        val tree = createObject(Footprint.square(1), 5, 5)

        assertTrue(world.placeObject(house))

        assertFalse(world.canPlaceObject(tree))
        assertFalse(world.placeObject(tree))

        assertSame(house, world.getObjectAt(5, 5))
        assertEquals(1, world.getObjects().size)
    }

    @Test
    fun `rejects objects extending beyond world bounds`() {
        val world = createWorld(width = 5, height = 5)

        val house = createObject(Footprint.square(2), 4, 4)

        assertFalse(world.canPlaceObject(house))
        assertFalse(world.placeObject(house))

        assertNull(world.getObjectAt(4, 4))
        assertTrue(world.getObjects().isEmpty())
    }

    @Test
    fun `removes an entire object without removing terrain`() {
        val world = createWorld()
        val house = createObject(Footprint.square(2), 4, 4)

        assertTrue(world.placeObject(house))

        val terrainBefore = world.getTile(4, 4)

        assertSame(house, world.removeObjectAt(5, 5))

        for (x in 4..5) {
            for (y in 4..5) {
                assertNull(world.getObjectAt(x, y))
            }
        }

        assertSame(terrainBefore, world.getTile(4, 4))
        assertTrue(world.getObjects().isEmpty())
        assertEquals(0, world.placedObjectCount)
    }

    @Test
    fun `respects a custom footprint and its origin`() {
        val world = createWorld()

        val footprint = Footprint.custom(
            TileOffset(0, 0),
            TileOffset(0, 1),
            TileOffset(0, 2),
            TileOffset(1, 2),
            origin = TileOffset(0, 2)
        )

        val objectToPlace = createObject(footprint, 4, 4)

        assertEquals(
            setOf(
                TilePosition(4, 2),
                TilePosition(4, 3),
                TilePosition(4, 4),
                TilePosition(5, 4)
            ),
            objectToPlace.occupiedTiles()
        )

        assertTrue(world.placeObject(objectToPlace))

        assertSame(objectToPlace, world.getObjectAt(4, 2))
        assertSame(objectToPlace, world.getObjectAt(5, 4))
        assertNull(world.getObjectAt(5, 3))
    }

    @Test
    fun `removes an object by reference`() {
        val world = createWorld()
        val house = createObject(Footprint.square(2), 4, 4)

        assertTrue(world.placeObject(house))
        assertTrue(world.removeObject(house))

        for (x in 4..5) {
            for (y in 4..5) {
                assertNull(world.getObjectAt(x, y))
            }
        }

        assertTrue(world.getObjects().isEmpty())

        // An object cannot be removed twice.
        assertFalse(world.removeObject(house))
    }

    @Test
    fun `object collection reflects placement and removal`() {
        val world = createWorld()
        val house = createObject(Footprint.square(2), 4, 4)

        val objects = world.getObjects()

        assertTrue(objects.isEmpty())

        assertTrue(world.placeObject(house))
        assertEquals(1, objects.size)
        assertTrue(house in objects)

        assertFalse(world.placeObject(house))
        assertEquals(1, objects.size)

        assertTrue(world.removeObject(house))
        assertTrue(objects.isEmpty())
    }

    @Test
    fun `cannot remove an object that is not registered`() {
        val world = createWorld()

        val house = createObject(Footprint.square(2), 4, 4)
        val otherHouse = createObject(Footprint.square(2), 4, 4)

        assertTrue(world.placeObject(house))

        assertFalse(world.removeObject(otherHouse))

        assertSame(house, world.getObjectAt(4, 4))
        assertEquals(1, world.getObjects().size)
    }

    @Test
    fun `places rectangular footprints with all origins`() {
        val expectedPositions = mapOf(
            FootprintOrigin.NORTH to setOf(
                TilePosition(2, 2),
                TilePosition(3, 2),
                TilePosition(2, 3),
                TilePosition(3, 3),
                TilePosition(2, 4),
                TilePosition(3, 4)
            ),
            FootprintOrigin.EAST to setOf(
                TilePosition(1, 2),
                TilePosition(2, 2),
                TilePosition(1, 3),
                TilePosition(2, 3),
                TilePosition(1, 4),
                TilePosition(2, 4)
            ),
            FootprintOrigin.SOUTH to setOf(
                TilePosition(1, 0),
                TilePosition(2, 0),
                TilePosition(1, 1),
                TilePosition(2, 1),
                TilePosition(1, 2),
                TilePosition(2, 2)
            ),
            FootprintOrigin.WEST to setOf(
                TilePosition(2, 0),
                TilePosition(3, 0),
                TilePosition(2, 1),
                TilePosition(3, 1),
                TilePosition(2, 2),
                TilePosition(3, 2)
            )
        )

        for ((origin, expected) in expectedPositions) {
            val world = createWorld()

            val placed = createObject(
                footprint = Footprint.rectangle(2, 3, origin),
                x = 2,
                y = 2
            )

            assertEquals(expected, placed.occupiedTiles())
            assertTrue(world.placeObject(placed))

            for ((x, y) in expected) {
                assertSame(placed, world.getObjectAt(x, y))
            }

            assertEquals(1, world.getObjects().size)
        }
    }

    @Test
    fun `removes custom footprint without affecting neighboring objects`() {
        val world = createWorld()

        val footprint = Footprint.custom(
            TileOffset(0, 0),
            TileOffset(1, 0),
            TileOffset(1, 1)
        )

        val building = createObject(footprint, 2, 2)

        val neighbor = createObject(
            Footprint.square(1),
            2,
            3
        )

        assertTrue(world.placeObject(building))
        assertTrue(world.placeObject(neighbor))

        assertSame(building, world.removeObjectAt(3, 3))

        assertNull(world.getObjectAt(2, 2))
        assertNull(world.getObjectAt(3, 2))
        assertNull(world.getObjectAt(3, 3))

        assertSame(neighbor, world.getObjectAt(2, 3))

        assertEquals(setOf(neighbor), world.getObjects())

        assertTrue(world.placeObject(building))
        assertEquals(2, world.getObjects().size)
    }

    @Test
    fun `object version changes only after successful modifications`() {
        val world = createWorld()

        val house = createObject(Footprint.square(2), 4, 4)
        val overlapping = createObject(Footprint.square(1), 5, 5)

        assertEquals(0L, world.objectVersion)

        assertTrue(world.placeObject(house))
        assertEquals(1L, world.objectVersion)

        assertFalse(world.placeObject(house))
        assertFalse(world.placeObject(overlapping))
        assertEquals(1L, world.objectVersion)

        assertTrue(world.removeObject(house))
        assertEquals(2L, world.objectVersion)

        assertFalse(world.removeObject(house))
        assertEquals(2L, world.objectVersion)

        assertTrue(world.placeObject(overlapping))
        assertEquals(3L, world.objectVersion)

        assertSame(overlapping, world.removeObjectAt(5, 5))
        assertEquals(4L, world.objectVersion)
    }

    @Test
    fun `places object through public placement api`() {
        val world = createWorld()

        val placeable = object : Placeable {
            override val footprint = Footprint.square(2)
        }

        assertTrue(
            world.canPlace(
                placeable = placeable,
                x = 4,
                y = 4
            )
        )

        val placed = world.place(
            placeable = placeable,
            x = 4,
            y = 4
        )

        assertTrue(placed != null)
        assertSame(placed, world.getObjectAt(4, 4))
        assertSame(placed, world.getObjectAt(5, 5))
    }

    @Test
    fun `removes object through public placement api`() {
        val world = createWorld()

        val placeable = object : Placeable {
            override val footprint = Footprint.square(1)
        }

        val placed = world.place(
            placeable = placeable,
            x = 4,
            y = 4
        )!!

        assertTrue(world.remove(placed))
        assertNull(world.getObjectAt(4, 4))
    }

    @Test
    fun `relocation atomically moves an object and preserves identity`() {
        val world = createWorld()
        val house = requireNotNull(world.place(
            object : Placeable { override val footprint = Footprint.square(2) },
            2,
            2
        ))

        assertTrue(world.relocate(house, TilePosition(5, 4)))

        assertEquals(5, house.x)
        assertEquals(4, house.y)
        assertNull(world.getObjectAt(2, 2))
        assertNull(world.getObjectAt(3, 3))
        assertSame(house, world.getObjectAt(5, 4))
        assertSame(house, world.getObjectAt(6, 5))
        assertEquals(setOf(house), world.getObjects())
        assertEquals(2L, world.objectVersion)
    }

    @Test
    fun `relocation may overlap the objects own previous footprint`() {
        val world = createWorld()
        val house = requireNotNull(world.place(
            object : Placeable { override val footprint = Footprint.square(3) },
            2,
            2
        ))

        assertNull(world.relocationFailure(house, TilePosition(3, 2)))
        assertTrue(world.relocate(house, TilePosition(3, 2)))

        assertNull(world.getObjectAt(2, 2))
        assertSame(house, world.getObjectAt(3, 2))
        assertSame(house, world.getObjectAt(5, 4))
    }

    @Test
    fun `occupied relocation fails without clearing either object`() {
        val world = createWorld()
        val footprint = Footprint.square(2)
        val first = requireNotNull(world.place(
            object : Placeable { override val footprint = footprint }, 1, 1
        ))
        val second = requireNotNull(world.place(
            object : Placeable { override val footprint = footprint }, 4, 1
        ))
        val version = world.objectVersion

        assertEquals(
            WorldPlacementFailure.OCCUPIED_TILE,
            world.relocationFailure(first, TilePosition(3, 1))
        )
        assertFalse(world.relocate(first, TilePosition(3, 1)))

        assertEquals(version, world.objectVersion)
        assertSame(first, world.getObjectAt(1, 1))
        assertSame(first, world.getObjectAt(2, 2))
        assertSame(second, world.getObjectAt(4, 1))
        assertSame(second, world.getObjectAt(5, 2))
    }

    @Test
    fun `out of bounds relocation fails without changing occupancy`() {
        val world = createWorld(width = 4, height = 4)
        val placed = requireNotNull(world.place(
            object : Placeable { override val footprint = Footprint.square(2) },
            1,
            1
        ))

        assertEquals(
            WorldPlacementFailure.FOOTPRINT_OUTSIDE_WORLD,
            world.relocationFailure(placed, TilePosition(3, 3))
        )
        assertFalse(world.relocate(placed, TilePosition(3, 3)))

        assertEquals(TilePosition(1, 1), TilePosition(placed.x, placed.y))
        assertSame(placed, world.getObjectAt(1, 1))
        assertSame(placed, world.getObjectAt(2, 2))
        assertEquals(1L, world.objectVersion)
    }

    @Test
    fun `relocating to the same origin is successful without version change`() {
        val world = createWorld()
        val placed = requireNotNull(world.place(
            object : Placeable { override val footprint = Footprint.square(1) },
            3,
            4
        ))

        assertTrue(world.relocate(placed, TilePosition(3, 4)))

        assertEquals(1L, world.objectVersion)
        assertSame(placed, world.getObjectAt(3, 4))
    }

    @Test
    fun `foreign object cannot be relocated into this world`() {
        val world = createWorld()
        val foreign = createObject(Footprint.square(1), 2, 2)

        assertEquals(
            WorldPlacementFailure.OCCUPIED_TILE,
            world.relocationFailure(foreign, TilePosition(3, 3))
        )
        assertFalse(world.relocate(foreign, TilePosition(3, 3)))
        assertEquals(0L, world.objectVersion)
    }

    @Test
    fun `custom origin remains anchored after relocation`() {
        val world = createWorld()
        val footprint = Footprint.custom(
            TileOffset(0, 0),
            TileOffset(0, 1),
            TileOffset(1, 1),
            origin = TileOffset(0, 1)
        )
        val placed = requireNotNull(world.place(
            object : Placeable { override val footprint = footprint },
            2,
            2
        ))

        assertTrue(world.relocate(placed, TilePosition(6, 7)))

        assertEquals(
            setOf(TilePosition(6, 6), TilePosition(6, 7), TilePosition(7, 7)),
            placed.occupiedTiles()
        )
        assertTrue(placed.occupiedTiles().all { world.getObjectAt(it) === placed })
    }

    @Test
    fun `placement failure reports bounds before occupancy`() {
        val world = createWorld(width = 3, height = 3)
        val single = object : Placeable { override val footprint = Footprint.square(1) }
        requireNotNull(world.place(single, 2, 2))
        val large = object : Placeable { override val footprint = Footprint.square(2) }

        assertEquals(
            WorldPlacementFailure.FOOTPRINT_OUTSIDE_WORLD,
            world.placementFailure(large, TilePosition(2, 2))
        )
    }
}
