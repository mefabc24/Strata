package com.mefabc24.strata.render.debug

import com.mefabc24.strata.debug.DebugInspectSettings
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugInspectionVisualizationTest {
    private data object TestTile : Tile
    private data object TestEntity : Entity
    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `inspection visualization is inactive outside inspect mode`() {
        val settings = DebugInspectSettings()
        val selection = DebugInspection.TileTarget(TilePosition(0, 0))

        assertTrue(inspectionVisuals(selection, false, settings).isEmpty())
    }

    @Test
    fun `object inspection options independently control object visuals`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val settings = DebugInspectSettings().apply {
            showObjectFootprint = true
            showObjectOrigin = false
            showObjectSpriteBounds = true
        }

        assertEquals(
            setOf(
                DebugInspectionVisual.OBJECT_FOOTPRINT,
                DebugInspectionVisual.OBJECT_SPRITE_BOUNDS
            ),
            inspectionVisuals(DebugInspection.ObjectTarget(placed), true, settings)
        )
    }

    @Test
    fun `entity inspection options independently control entity visuals`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val settings = DebugInspectSettings().apply {
            showEntityTile = false
            showEntityPosition = true
            showEntityPath = true
            showEntityDirection = false
            showEntitySpriteBounds = true
        }

        assertEquals(
            setOf(
                DebugInspectionVisual.ENTITY_POSITION,
                DebugInspectionVisual.ENTITY_PATH,
                DebugInspectionVisual.ENTITY_SPRITE_BOUNDS
            ),
            inspectionVisuals(DebugInspection.EntityTarget(entity), true, settings)
        )
    }
}
