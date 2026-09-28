package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugToolControllerTest {
    private enum class Terrain : TerrainId { GRASS }
    private data object TestTile : Tile
    private class TestPlaceable : Placeable { override val footprint = Footprint.square(1) }

    @Test
    fun `switching tools cancels interactions and restores placement state`() {
        val world = World(4, 4) { _, _ -> TestTile }
        val placement = PlacementController(world)
        val gameFactory: () -> Placeable = ::TestPlaceable
        placement.selectedFactory = gameFactory
        placement.enabled = false
        val terrain = TerrainRegistry("", {}, { TextureRegion() }).apply {
            register(Terrain.GRASS, "grass", factory = { TestTile })
        }
        val painter = DebugTerrainPainter(world, terrain.paintableEntries)
        val drag = DebugBuildDragController(placement)
        val state = DebugWorldState()
        val tools = DebugToolController(
            painter, placement, drag, DebugInspector(state),
            DebugPathfindingTool(world, state)
        )

        tools.selectBuildEntry(null)
        tools.select(DebugToolMode.BUILD)
        assertTrue(placement.enabled)
        tools.selectBuildEntry(objectEntryFor(world))
        assertTrue(drag.begin(TilePosition(1, 1)))
        tools.select(DebugToolMode.PAINT)
        assertFalse(drag.active)
        assertTrue(painter.enabled)
        painter.beginPaint(0, 0)
        tools.select(DebugToolMode.NONE)
        assertFalse(painter.enabled)
        assertFalse(painter.dragPaint(1, 0))
        assertFalse(placement.enabled)
        assertSame(gameFactory, placement.selectedFactory)
    }

    private fun objectEntryFor(world: World): com.mefabc24.strata.render.`object`.ObjectEntry {
        val registry = com.mefabc24.strata.render.`object`.ObjectRegistry(
            directory = "", queueTexture = {}, regionFor = { TextureRegion() }, loadAlphaMask = { null }
        )
        registry.register<TestPlaceable>("test", factory = ::TestPlaceable)
        return registry.constructibleEntries.single()
    }
}
