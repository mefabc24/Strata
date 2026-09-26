package com.mefabc24.strata.world

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
        entity.updateMovement(0.25f)
        val cancelledAt = entity.position
        entity.cancelMovement()
        entity.updateMovement(10f)

        assertEquals(cancelledAt, entity.position)
        assertFalse(entity.isMoving)
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

    private fun entityAt(x: Float, y: Float): WorldEntity {
        return WorldEntity(TestEntity, EntityPosition(x, y))
    }

    private data object TestEntity : Entity
}
