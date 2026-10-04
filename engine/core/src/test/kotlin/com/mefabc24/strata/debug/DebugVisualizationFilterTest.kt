package com.mefabc24.strata.debug

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.render.RenderDebugSnapshot
import com.mefabc24.strata.render.RenderItemDebugSnapshot
import com.mefabc24.strata.render.TerrainRenderDebugSnapshot
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugVisualizationFilterTest {
    private data object TestTile : Tile
    private data object TestEntity : Entity
    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `shared context filters objects entities and terrain from the same targets`() {
        val world = World(3, 2) { _, _ -> TestTile }
        val selectedObject = requireNotNull(world.place(TestObject(), 0, 0))
        val visibleObject = requireNotNull(world.place(TestObject(), 1, 0))
        val visibleEntity = world.addEntity(TestEntity, EntityPosition(0.5f, 1.5f))
        val hoveredEntity = world.addEntity(TestEntity, EntityPosition(2.5f, 1.5f))
        val terrainPosition = TilePosition(2, 0)
        val terrainItem = RenderItemDebugSnapshot(
            index = 0,
            terrain = TerrainRenderDebugSnapshot(terrainPosition, rank = 0),
            drawn = true
        )
        val snapshot = RenderDebugSnapshot(
            visibleArea = Rectangle(),
            items = listOf(
                terrainItem,
                RenderItemDebugSnapshot(
                    index = 1,
                    placedObject = selectedObject,
                    drawn = false
                ),
                RenderItemDebugSnapshot(
                    index = 2,
                    placedObject = visibleObject,
                    drawn = true
                ),
                RenderItemDebugSnapshot(
                    index = 3,
                    entity = visibleEntity,
                    drawn = true
                ),
                RenderItemDebugSnapshot(
                    index = 4,
                    entity = hoveredEntity,
                    drawn = false
                )
            )
        )
        val context = DebugVisualizationFilterContext()
        context.update(
            inspection = DebugInspection.ObjectTarget(selectedObject),
            selectedTarget = PickedTarget.Tile(terrainPosition),
            hoveredTarget = PickedTarget.Entity(hoveredEntity, bounds = null),
            renderSnapshot = snapshot
        )

        assertTrue(context.matches(DebugVisualizationFilter.ALL, selectedObject))
        assertTrue(context.matches(DebugVisualizationFilter.SELECTED, selectedObject))
        assertTrue(context.matches(DebugVisualizationFilter.SELECTED, terrainItem))
        assertFalse(context.matches(DebugVisualizationFilter.SELECTED, visibleObject))

        assertTrue(context.matches(DebugVisualizationFilter.HOVERED, hoveredEntity))
        assertFalse(context.matches(DebugVisualizationFilter.HOVERED, visibleEntity))

        assertTrue(context.matches(DebugVisualizationFilter.VISIBLE, visibleObject))
        assertTrue(context.matches(DebugVisualizationFilter.VISIBLE, visibleEntity))
        assertTrue(context.matches(DebugVisualizationFilter.VISIBLE, terrainItem))
        assertFalse(context.matches(DebugVisualizationFilter.VISIBLE, selectedObject))
        assertFalse(context.matches(DebugVisualizationFilter.VISIBLE, hoveredEntity))
        assertTrue(context.matches(DebugVisualizationFilter.SELECTED, 2, 0))
        assertTrue(context.matches(DebugVisualizationFilter.HOVERED, 2, 1))
        assertFalse(context.matches(DebugVisualizationFilter.SELECTED, 0, 0))
        assertTrue(context.matches(DebugVisualizationFilter.VISIBLE, 1, 0))
    }

    @Test
    fun `missing selection hover and snapshot produce an empty filtered result`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val context = DebugVisualizationFilterContext()
        context.update(null, null, null, null)

        assertFalse(context.matches(DebugVisualizationFilter.SELECTED, placed))
        assertFalse(context.matches(DebugVisualizationFilter.HOVERED, entity))
        assertFalse(context.matches(DebugVisualizationFilter.VISIBLE, placed))
    }

    @Test
    fun `hover picking activates only for active target diagnostics`() {
        val settings = DebugSettings().apply {
            visualizationFilter = DebugVisualizationFilter.HOVERED
        }

        assertFalse(settings.needsHoveredVisualizationTarget())
        settings.objects.showOccupiedTiles = true
        assertTrue(settings.needsHoveredVisualizationTarget())
        settings.objects.showOccupiedTiles = false
        settings.worldInfo.showOrigin = true
        assertTrue(settings.needsHoveredVisualizationTarget())
        settings.visualizationFilter = DebugVisualizationFilter.SELECTED
        assertFalse(settings.needsHoveredVisualizationTarget())
    }
}
