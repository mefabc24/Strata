package com.mefabc24.strata.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class WorldEntityTest {

    @Test
    fun `entity positions use tile centers and validate coordinates`() {
        assertEquals(
            EntityPosition(1.5f, 2.5f),
            EntityPosition.centerOf(TilePosition(1, 2))
        )
        assertEquals(
            TilePosition(1, 2),
            EntityPosition(1.75f, 2.1f).tile
        )

        listOf(
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                EntityPosition(invalid, 0f)
            }
            assertFailsWith<IllegalArgumentException> {
                EntityPosition(0f, invalid)
            }
        }
    }

    @Test
    fun `world owns independent runtime entities`() {
        val world = world()
        val firstState = TestEntity("first")
        val secondState = TestEntity("second")

        val first = world.addEntity(
            firstState,
            EntityPosition.centerOf(TilePosition(1, 1))
        )
        val second = world.addEntity(
            secondState,
            EntityPosition.centerOf(TilePosition(1, 1))
        )

        assertNotSame(first, second)
        assertEquals(firstState, first.entity)
        assertEquals(secondState, second.entity)

        first.teleport(EntityPosition(2.25f, 3.75f))

        assertEquals(EntityPosition(2.25f, 3.75f), first.position)
        assertEquals(TilePosition(2, 3), first.currentTile)
        assertEquals(EntityPosition(1.5f, 1.5f), second.position)
    }

    @Test
    fun `entity storage and structural version update on add and remove`() {
        val world = world()
        assertEquals(0L, world.entityVersion)

        val entity = world.addEntity(
            TestEntity("walker"),
            EntityPosition(0.5f, 0.5f)
        )

        assertEquals(1L, world.entityVersion)
        assertEquals(setOf(entity), world.getEntities())

        entity.position = EntityPosition(1.5f, 0.5f)
        assertEquals(1L, world.entityVersion)

        assertTrue(world.removeEntity(entity))
        assertEquals(2L, world.entityVersion)
        assertTrue(world.getEntities().isEmpty())

        assertFalse(world.removeEntity(entity))
        assertEquals(2L, world.entityVersion)
    }

    @Test
    fun `entity collection cannot mutate world state`() {
        val world = world()
        val entity = world.addEntity(
            TestEntity("walker"),
            EntityPosition(0.5f, 0.5f)
        )

        @Suppress("UNCHECKED_CAST")
        val exposed = world.getEntities() as MutableSet<WorldEntity>

        assertFailsWith<UnsupportedOperationException> {
            exposed.remove(entity)
        }
        assertTrue(entity in world.getEntities())
    }

    @Test
    fun `entities neither occupy nor block tiles`() {
        val world = world()
        world.addEntity(
            TestEntity("walker"),
            EntityPosition.centerOf(TilePosition(1, 1))
        )

        assertEquals(null, world.getObjectAt(1, 1))
        assertTrue(world.canPlace(TestPlaceable, 1, 1))
        assertTrue(world.place(TestPlaceable, 1, 1) != null)
        assertEquals(1, world.getEntities().size)
    }

    private fun world() = World(4, 4) { _, _ ->
        TestTile
    }

    private data class TestEntity(val name: String) : Entity

    private object TestPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }

    private data object TestTile : Tile
}
