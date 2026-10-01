package com.mefabc24.strata.world

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorldEntityMovementTest {

    @Test
    fun `tile paths become centers and movement interpolates by speed`() {
        val entity = entityAt(0.5f, 0.5f)
        entity.followPath(
            path = listOf(TilePosition(0, 0), TilePosition(2, 0)),
            speed = 2f
        )

        assertEquals(
            listOf(TilePosition(2, 0)),
            entity.remainingPath
        )

        assertEquals(listOf(EntityPosition(2.5f, 0.5f)), entity.remainingWaypoints)
        entity.updateMovement(0.25f)

        assertEquals(EntityPosition(1f, 0.5f), entity.position)
        assertTrue(entity.isMoving)
        assertEquals(2f, entity.movementSpeed)
    }

    @Test
    fun `large delta crosses waypoints and stops exactly at destination`() {
        val entity = entityAt(0.5f, 0.5f)
        entity.followPath(
            path = listOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                TilePosition(1, 1),
                TilePosition(2, 1)
            ),
            speed = 2f
        )

        entity.updateMovement(10f)

        assertEquals(EntityPosition(2.5f, 1.5f), entity.position)
        assertFalse(entity.isMoving)
        assertNull(entity.movementSpeed)
        assertTrue(entity.remainingWaypoints.isEmpty())
        assertTrue(entity.remainingPath.isEmpty())
    }

    @Test
    fun `large delta carries remaining distance across multiple waypoints`() {
        val entity = entityAt(0.5f, 0.5f)
        entity.followPath(
            path = listOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                TilePosition(1, 1),
                TilePosition(3, 1)
            ),
            speed = 2f
        )

        entity.updateMovement(1.25f)

        assertEquals(EntityPosition(2f, 1.5f), entity.position)
        assertEquals(
            listOf(EntityPosition(3.5f, 1.5f)),
            entity.remainingWaypoints
        )
        assertTrue(entity.isMoving)
        assertEquals(2f, entity.movementSpeed)
    }

    @Test
    fun `movement reaches a waypoint without overshoot`() {
        val entity = entityAt(0.5f, 0.5f)
        entity.followPath(listOf(TilePosition(1, 0)), speed = 1f)

        entity.updateMovement(1f)

        assertEquals(EntityPosition(1.5f, 0.5f), entity.position)
        assertFalse(entity.isMoving)
    }

    @Test
    fun `one node and empty paths behave predictably`() {
        val entity = entityAt(0.5f, 0.5f)

        entity.followPath(listOf(TilePosition(0, 0)), speed = 1f)
        assertFalse(entity.isMoving)

        entity.followPath(listOf(TilePosition(2, 0)), speed = 1f)
        assertTrue(entity.isMoving)
        entity.followPath(emptyList(), speed = 1f)

        assertFalse(entity.isMoving)
        assertNull(entity.movementSpeed)
        assertTrue(entity.remainingWaypoints.isEmpty())
        assertTrue(entity.remainingPath.isEmpty())
        assertEquals(EntityPosition(0.5f, 0.5f), entity.position)
    }

    @Test
    fun `new path replaces old route and cancel preserves current position`() {
        val entity = entityAt(0.5f, 0.5f)
        entity.followPath(listOf(TilePosition(3, 0)), speed = 1f)
        entity.updateMovement(0.5f)
        assertEquals(EntityPosition(1f, 0.5f), entity.position)

        entity.followPath(listOf(TilePosition(1, 2)), speed = 2f)
        assertEquals(
            listOf(EntityPosition(1.5f, 2.5f)),
            entity.remainingWaypoints
        )
        assertEquals(2f, entity.movementSpeed)
        entity.updateMovement(0.25f)
        val cancelledAt = entity.position
        entity.cancelMovement()
        entity.updateMovement(10f)

        assertEquals(cancelledAt, entity.position)
        assertFalse(entity.isMoving)
        assertNull(entity.movementSpeed)
        assertTrue(entity.remainingWaypoints.isEmpty())
        assertTrue(entity.remainingPath.isEmpty())
    }

    @Test
    fun `teleport during movement relocates immediately and clears route`() {
        val entity = entityAt(0.5f, 0.5f)
        entity.followPath(
            path = listOf(TilePosition(2, 0), TilePosition(2, 2)),
            speed = 1f
        )
        entity.updateMovement(0.5f)

        val destination = EntityPosition(3.25f, 2.75f)
        entity.teleport(destination)
        entity.updateMovement(10f)

        assertEquals(destination, entity.position)
        assertEquals(TilePosition(3, 2), entity.currentTile)
        assertFalse(entity.isMoving)
        assertNull(entity.movementSpeed)
        assertTrue(entity.remainingWaypoints.isEmpty())
        assertTrue(entity.remainingPath.isEmpty())
    }

    @Test
    fun `invalid speeds and deltas are rejected`() {
        val entity = entityAt(0.5f, 0.5f)

        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { speed ->
            assertFailsWith<IllegalArgumentException> {
                entity.followPath(emptyList(), speed)
            }
        }

        entity.followPath(listOf(TilePosition(1, 0)), 1f)
        listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { delta ->
            assertFailsWith<IllegalArgumentException> {
                entity.updateMovement(delta)
            }
        }
    }

    @Test
    fun `deterministic delta sequences produce deterministic positions`() {
        val first = entityAt(0.5f, 0.5f)
        val second = entityAt(0.5f, 0.5f)
        val path = listOf(
            TilePosition(0, 0),
            TilePosition(1, 0),
            TilePosition(1, 1),
            TilePosition(2, 1)
        )
        first.followPath(path, 1.25f)
        second.followPath(path, 1.25f)

        listOf(0.1f, 0.25f, 0.4f, 0.05f, 0.8f).forEach { delta ->
            first.updateMovement(delta)
            second.updateMovement(delta)
            assertEquals(first.position, second.position)
            assertEquals(first.remainingWaypoints, second.remainingWaypoints)
        }
    }

    @Test
    fun `movement updates entity direction along isometric grid axes`() {
        val entity = entityAt(2.5f, 2.5f)

        entity.followPath(
            listOf(TilePosition(3, 2)),
            speed = 1f
        )
        assertEquals(
            EntityDirection.SOUTH_EAST,
            entity.direction
        )

        entity.teleport(EntityPosition(2.5f, 2.5f))
        entity.followPath(
            listOf(TilePosition(1, 2)),
            speed = 1f
        )
        assertEquals(
            EntityDirection.NORTH_WEST,
            entity.direction
        )

        entity.teleport(EntityPosition(2.5f, 2.5f))
        entity.followPath(
            listOf(TilePosition(2, 3)),
            speed = 1f
        )
        assertEquals(
            EntityDirection.SOUTH_WEST,
            entity.direction
        )

        entity.teleport(EntityPosition(2.5f, 2.5f))
        entity.followPath(
            listOf(TilePosition(2, 1)),
            speed = 1f
        )
        assertEquals(
            EntityDirection.NORTH_EAST,
            entity.direction
        )
    }

    @Test
    fun `movement vectors resolve all eight screen directions`() {
        val origin = EntityPosition(2.5f, 2.5f)
        val expectedDirections = mapOf(
            TilePosition(1, 1) to EntityDirection.NORTH,
            TilePosition(2, 1) to EntityDirection.NORTH_EAST,
            TilePosition(3, 1) to EntityDirection.EAST,
            TilePosition(3, 2) to EntityDirection.SOUTH_EAST,
            TilePosition(3, 3) to EntityDirection.SOUTH,
            TilePosition(2, 3) to EntityDirection.SOUTH_WEST,
            TilePosition(1, 3) to EntityDirection.WEST,
            TilePosition(1, 2) to EntityDirection.NORTH_WEST
        )
        val entity = entityAt(origin.x, origin.y)

        expectedDirections.forEach { (target, direction) ->
            entity.teleport(origin)
            entity.followPath(listOf(target), speed = 1f)
            assertEquals(direction, entity.direction, target.toString())
        }
    }

    @Test
    fun `diagonal movement uses the full segment distance`() {
        val entity = entityAt(0.5f, 0.5f)
        entity.followPath(listOf(TilePosition(1, 1)), speed = 1f)

        entity.updateMovement(1f)

        val component = 0.5f + 1f / sqrt(2f)
        assertEquals(component, entity.position.x, absoluteTolerance = 0.00001f)
        assertEquals(component, entity.position.y, absoluteTolerance = 0.00001f)
        assertTrue(entity.isMoving)

        entity.updateMovement(sqrt(2f) - 1f)

        assertEquals(EntityPosition(1.5f, 1.5f), entity.position)
        assertFalse(entity.isMoving)
    }

    @Test
    fun `entity retains its last direction after movement ends`() {
        val entity = entityAt(0.5f, 0.5f)

        entity.followPath(
            listOf(TilePosition(1, 0)),
            speed = 1f
        )

        entity.updateMovement(1f)

        assertFalse(entity.isMoving)
        assertEquals(
            EntityDirection.SOUTH_EAST,
            entity.direction
        )
    }

    @Test
    fun `entity can face a direction without moving`() {
        val entity = entityAt(0.5f, 0.5f)

        entity.face(EntityDirection.NORTH_EAST)

        assertEquals(
            EntityDirection.NORTH_EAST,
            entity.direction
        )
        assertEquals(
            EntityPosition(0.5f, 0.5f),
            entity.position
        )
        assertFalse(entity.isMoving)
    }

    private fun entityAt(x: Float, y: Float): WorldEntity {
        return WorldEntity(TestEntity, EntityPosition(x, y))
    }

    private data object TestEntity : Entity
}
