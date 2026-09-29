package com.mefabc24.strata.render.debug

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.debug.DebugCullingSettings
import com.mefabc24.strata.render.RenderDebugSnapshot
import com.mefabc24.strata.render.RenderItemDebugSnapshot
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CullingDebugClassificationTest {
    private data object TestTile : Tile
    private data object TestEntity : Entity
    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `object and entity items retain renderer drawn classification`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val snapshot = RenderDebugSnapshot(
            visibleArea = Rectangle(0f, 0f, 10f, 10f),
            items = listOf(
                RenderItemDebugSnapshot(0, placedObject = placed, bounds = Rectangle(), drawn = true),
                RenderItemDebugSnapshot(1, placedObject = placed, bounds = Rectangle(), drawn = false),
                RenderItemDebugSnapshot(2, entity = entity, bounds = Rectangle(), drawn = true),
                RenderItemDebugSnapshot(3, entity = entity, bounds = Rectangle(), drawn = false)
            )
        )

        assertEquals(
            CullingDebugCounts(1, 1, 1, 1),
            cullingDebugCounts(snapshot)
        )
        assertEquals(
            CullingDebugClassification(CullingDebugItemKind.OBJECT, true),
            classifyCullingItem(snapshot.items[0])
        )
        assertEquals(
            CullingDebugClassification(CullingDebugItemKind.ENTITY, false),
            classifyCullingItem(snapshot.items[3])
        )
    }

    @Test
    fun `default culling colors distinguish type and visibility`() {
        val settings = DebugCullingSettings()

        assertNotEquals(settings.objectDrawnColor, settings.entityDrawnColor)
        assertNotEquals(settings.objectDrawnColor, settings.objectCulledColor)
        assertNotEquals(settings.entityDrawnColor, settings.entityCulledColor)
    }
}
