package com.mefabc24.strata.debug.inspector

import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugInspectorTest {
    private data object TestTile : Tile
    private class TestEntity : Entity
    private class TestObject : Placeable { override val footprint = Footprint.square(1) }

    @Test
    fun `selection priority is entity then object then tile and can be cleared`() {
        val world = World(2, 2) { _, _ -> TestTile }
        val entity = world.addEntity(TestEntity(), EntityPosition.centerOf(TilePosition(0, 0)))
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val inspector = DebugInspector(DebugWorldState())
        assertTrue(inspector.selectFrontmost(entity, placed, TilePosition(0, 0)))
        assertIs<DebugInspection.EntityTarget>(inspector.selection)
        inspector.selectFrontmost(null, placed, TilePosition(0, 0))
        assertIs<DebugInspection.ObjectTarget>(inspector.selection)
        inspector.selectFrontmost(null, null, TilePosition(0, 0))
        assertIs<DebugInspection.TileTarget>(inspector.selection)
        assertTrue(inspector.clear())
        assertNull(inspector.selection)
    }
}
