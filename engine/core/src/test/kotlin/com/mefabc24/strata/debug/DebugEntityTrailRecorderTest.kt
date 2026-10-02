package com.mefabc24.strata.debug

import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.WorldEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DebugEntityTrailRecorderTest {
    private data object TestEntity : Entity

    @Test
    fun `unchanged and sub-threshold moving positions are not duplicated`() {
        val entity = entityAt(0.5f, 0.5f)
        val settings = enabledSettings().apply {
            trailMinimumDistance = 0.75f
        }
        val recorder = DebugEntityTrailRecorder()
        val entities = setOf(entity)

        recorder.update(entities, 0f, settings)
        recorder.update(entities, 0.25f, settings)
        entity.followPath(listOf(TilePosition(3, 0)), speed = 1f)
        repeat(2) {
            entity.updateMovement(0.25f)
            recorder.update(entities, 0.25f, settings)
        }
        assertEquals(1, requireNotNull(recorder.trail(entity)).size)

        entity.updateMovement(0.25f)
        recorder.update(entities, 0.25f, settings)
        val trail = requireNotNull(recorder.trail(entity))
        assertEquals(2, trail.size)
        assertEquals(0.5f, trail.xAt(0))
        assertEquals(1.25f, trail.xAt(1))
    }

    @Test
    fun `movement endpoint is retained even below the sampling distance`() {
        val entity = entityAt(0.5f, 0.5f)
        val settings = enabledSettings().apply {
            trailMinimumDistance = 2f
        }
        val recorder = DebugEntityTrailRecorder()
        val entities = setOf(entity)
        recorder.update(entities, 0f, settings)

        entity.followPath(listOf(TilePosition(1, 0)), speed = 1f)
        entity.updateMovement(1f)
        val positionBeforeRecording = entity.position
        recorder.update(entities, 1f, settings)

        val trail = requireNotNull(recorder.trail(entity))
        assertEquals(2, trail.size)
        assertEquals(1.5f, trail.xAt(1))
        assertEquals(positionBeforeRecording, entity.position)
    }

    @Test
    fun `position capacity retains only the newest samples`() {
        val entity = entityAt(0.5f, 0.5f)
        val settings = enabledSettings().apply {
            trailMaxPositions = 3
            trailMinimumDistance = 0f
            trailHistoryDurationSeconds = 100f
        }
        val recorder = DebugEntityTrailRecorder()
        val entities = setOf(entity)
        recorder.update(entities, 0f, settings)
        entity.followPath(listOf(TilePosition(4, 0)), speed = 1f)

        repeat(4) {
            entity.updateMovement(0.5f)
            recorder.update(entities, 0.5f, settings)
        }

        val trail = requireNotNull(recorder.trail(entity))
        assertEquals(3, trail.size)
        assertEquals(listOf(1.5f, 2f, 2.5f), (0 until trail.size).map(trail::xAt))
    }

    @Test
    fun `history duration prunes old segments while retaining a current anchor`() {
        val entity = entityAt(0.5f, 0.5f)
        val settings = enabledSettings().apply {
            trailMinimumDistance = 0f
            trailHistoryDurationSeconds = 1f
        }
        val recorder = DebugEntityTrailRecorder()
        val entities = setOf(entity)
        recorder.update(entities, 0f, settings)
        entity.followPath(listOf(TilePosition(4, 0)), speed = 1f)

        repeat(3) {
            entity.updateMovement(0.5f)
            recorder.update(entities, 0.5f, settings)
        }
        assertEquals(3, requireNotNull(recorder.trail(entity)).size)

        recorder.update(entities, 0.6f, settings)
        assertEquals(1, requireNotNull(recorder.trail(entity)).size)
    }

    @Test
    fun `clear disable and entity removal release recorded history`() {
        val entity = entityAt(0.5f, 0.5f)
        val settings = enabledSettings().apply { trailMinimumDistance = 0f }
        val recorder = DebugEntityTrailRecorder()
        val entities = setOf(entity)
        recorder.update(entities, 0f, settings)
        entity.teleport(EntityPosition(1f, 1f))
        recorder.update(entities, 0f, settings)
        assertEquals(2, requireNotNull(recorder.trail(entity)).size)

        settings.clearMovementTrails()
        recorder.update(entities, 0f, settings)
        assertEquals(1, requireNotNull(recorder.trail(entity)).size)

        recorder.update(emptySet(), 0f, settings)
        assertNull(recorder.trail(entity))

        recorder.update(entities, 0f, settings)
        settings.showMovementTrail = false
        recorder.update(entities, 0f, settings)
        assertNull(recorder.trail(entity))
    }

    @Test
    fun `invalid recording delta is rejected`() {
        val recorder = DebugEntityTrailRecorder()
        val settings = enabledSettings()

        listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                recorder.update(emptySet(), invalid, settings)
            }
        }
    }

    private fun enabledSettings() = DebugEntitySettings().apply {
        enabled = true
        showMovementTrail = true
    }

    private fun entityAt(x: Float, y: Float) =
        WorldEntity(TestEntity, EntityPosition(x, y))
}
