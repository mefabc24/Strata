package com.mefabc24.strata.debug

import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugEntityFreezeStateTest {
    @Test
    fun `frozen entity remains still while another entity advances`() {
        val world = world()
        val frozen = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val moving = world.addEntity(TestEntity, EntityPosition(0.5f, 1.5f))
        frozen.followPath(listOf(TilePosition(3, 0)), speed = 1f)
        moving.followPath(listOf(TilePosition(3, 1)), speed = 1f)
        val settings = DebugSettings()
        settings.setEntityFrozen(frozen, true)

        world.updateEntities(1f) { !settings.isEntityFrozen(it) }

        assertEquals(EntityPosition(0.5f, 0.5f), frozen.position)
        assertEquals(EntityPosition(1.5f, 1.5f), moving.position)
        assertTrue(frozen.isMoving)
        assertEquals(listOf(TilePosition(3, 0)), frozen.remainingPath)
    }

    @Test
    fun `unfreezing resumes preserved route from current position`() {
        val world = world()
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        entity.followPath(listOf(TilePosition(3, 0)), speed = 1f)
        val settings = DebugSettings()
        settings.setEntityFrozen(entity, true)
        world.updateEntities(2f) { !settings.isEntityFrozen(it) }

        settings.setEntityFrozen(entity, false)
        world.updateEntities(1f) { !settings.isEntityFrozen(it) }

        assertEquals(EntityPosition(1.5f, 0.5f), entity.position)
        assertTrue(entity.isMoving)
    }

    @Test
    fun `freeze query and unfreeze all use entity identity`() {
        val world = world()
        val first = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val second = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val settings = DebugSettings()

        settings.setEntityFrozen(first, true)

        assertTrue(settings.isEntityFrozen(first))
        assertFalse(settings.isEntityFrozen(second))
        settings.unfreezeAllEntities()
        assertFalse(settings.isEntityFrozen(first))
    }

    @Test
    fun `changing inspect selection does not unfreeze prior entity`() {
        val world = world()
        val first = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val second = world.addEntity(TestEntity, EntityPosition(1.5f, 0.5f))
        val inspector = DebugInspector()
        val settings = DebugSettings()
        inspector.selectFrontmost(first, null, null)
        settings.setEntityFrozen(first, true)

        inspector.selectFrontmost(second, null, null)

        assertTrue(settings.isEntityFrozen(first))
        assertFalse(settings.isEntityFrozen(second))
        assertSame(second, (inspector.selection as com.mefabc24.strata.debug.inspector.DebugInspection.EntityTarget).entity)
    }

    @Test
    fun `freeze animation option defaults to enabled`() {
        assertTrue(DebugSettings().inspect.freezeEntityAnimation)
    }

    @Test
    fun `frozen entity playback is held while other entity advances`() {
        val world = world()
        val frozen = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val other = world.addEntity(TestEntity, EntityPosition(1.5f, 0.5f))
        val state = DebugEntityFreezeState()
        state.setFrozen(frozen, true)

        assertEquals(2f, state.resolveAnimation(frozen, 2f, true) { it })
        assertEquals(2f, state.resolveAnimation(frozen, 5f, true) { it })
        assertEquals(5f, state.resolveAnimation(other, 5f, true) { it })
    }

    @Test
    fun `animation continues when freeze animation option is disabled`() {
        val entity = world().addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val state = DebugEntityFreezeState()
        state.setFrozen(entity, true)

        assertEquals(2f, state.resolveAnimation(entity, 2f, false) { it })
        assertEquals(5f, state.resolveAnimation(entity, 5f, false) { it })
        assertTrue(state.isFrozen(entity))
    }

    @Test
    fun `unfreezing resumes playback from held time`() {
        val entity = world().addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val state = DebugEntityFreezeState()
        state.setFrozen(entity, true)
        state.resolveAnimation(entity, 2f, true) { it }
        assertEquals(2f, state.resolveAnimation(entity, 5f, true) { it })

        state.setFrozen(entity, false)

        assertEquals(2f, state.resolveAnimation(entity, 5f, true) { it })
        assertEquals(3f, state.resolveAnimation(entity, 6f, true) { it })
    }

    @Test
    fun `frozen playback preserves resolved state and direction value`() {
        val entity = world().addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val state = DebugEntityFreezeState()
        state.setFrozen(entity, true)
        var currentVisual = "walk-south-east"

        val held = state.resolveAnimation(entity, 1f, true) { currentVisual }
        currentVisual = "idle-north-west"

        assertEquals(held, state.resolveAnimation(entity, 4f, true) { currentVisual })
        assertEquals("walk-south-east", held)
    }

    private fun world() = World(5, 5) { _, _ -> TestTile }

    private data object TestTile : Tile
    private data object TestEntity : Entity
}
