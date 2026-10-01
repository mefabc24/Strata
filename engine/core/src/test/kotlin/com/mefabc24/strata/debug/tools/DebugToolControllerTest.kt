package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import com.mefabc24.strata.iso.PickedTarget

class DebugToolControllerTest {
    private enum class Terrain : TerrainId { GRASS }
    private data object TestTile : Tile
    private class TestPlaceable : Placeable { override val footprint = Footprint.square(1) }
    private data object TestEntity : Entity

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

    @Test
    fun `inspection highlight follows mode without clearing selection`() {
        val world = World(2, 2) { _, _ -> TestTile }
        val state = DebugWorldState()
        val inspector = DebugInspector(state)
        val painter = DebugTerrainPainter(world, emptyList())
        val tools = DebugToolController(
            painter = painter,
            placement = null,
            buildDrag = null,
            inspector = inspector,
            pathfinding = DebugPathfindingTool(world, state)
        )
        val selected = TilePosition(1, 1)
        inspector.selectFrontmost(null, null, selected)

        tools.select(DebugToolMode.INSPECT)
        assertTrue(inspector.highlightVisible)

        tools.select(DebugToolMode.NONE)
        assertFalse(inspector.highlightVisible)
        assertEquals(selected, (inspector.selection as DebugInspection.TileTarget).position)

        tools.select(DebugToolMode.INSPECT)
        assertTrue(inspector.highlightVisible)
        assertEquals(selected, (inspector.selection as DebugInspection.TileTarget).position)
    }

    @Test
    fun `switching pathfinding to none cancels its active interaction`() {
        val world = World(2, 2) { _, _ -> TestTile }
        val state = DebugWorldState()
        val pathfinding = DebugPathfindingTool(world, state)
        val tools = DebugToolController(
            painter = DebugTerrainPainter(world, emptyList()),
            placement = null,
            buildDrag = null,
            inspector = DebugInspector(state),
            pathfinding = pathfinding
        )

        tools.select(DebugToolMode.PATHFINDING)
        assertTrue(pathfinding.click(TilePosition(0, 0)))
        assertEquals(TilePosition(0, 0), pathfinding.start)

        tools.select(DebugToolMode.NONE)

        assertEquals(DebugToolMode.NONE, tools.mode)
        assertEquals(null, pathfinding.start)
        assertEquals(null, pathfinding.result)
    }

    @Test
    fun `switching away from pathfinding releases its entity hold`() {
        val world = World(2, 2) { _, _ -> TestTile }
        val settings = DebugSettings()
        val pathfinding = DebugPathfindingTool(
            world = world,
            state = settings.worldState,
            entityFreezeState = settings.entityFreezeState
        )
        val tools = DebugToolController(
            painter = DebugTerrainPainter(world, emptyList()),
            placement = null,
            buildDrag = null,
            inspector = DebugInspector(settings.worldState),
            pathfinding = pathfinding
        )
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        tools.select(DebugToolMode.PATHFINDING)
        pathfinding.selectEntity(entity)
        assertTrue(settings.isEntityHeld(entity))

        tools.select(DebugToolMode.INSPECT)

        assertFalse(settings.isEntityHeld(entity))
    }

    @Test
    fun `free camera override follows tool mode`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val state = DebugWorldState()
        var freeCamera = false
        val tools = DebugToolController(
            painter = DebugTerrainPainter(world, emptyList()),
            placement = null,
            buildDrag = null,
            inspector = DebugInspector(state),
            pathfinding = DebugPathfindingTool(world, state),
            setFreeCamera = { freeCamera = it }
        )

        tools.select(DebugToolMode.FREE_CAMERA)
        assertTrue(freeCamera)

        tools.select(DebugToolMode.NONE)
        assertFalse(freeCamera)
    }

    @Test
    fun `leaving move mode cancels drag and clears preview`() {
        val world = World(2, 2) { _, _ -> TestTile }
        val state = DebugWorldState()
        val move = DebugMoveTool(world, state)
        val placed = requireNotNull(world.place(TestPlaceable(), 0, 0))
        val tools = DebugToolController(
            painter = DebugTerrainPainter(world, emptyList()),
            placement = null,
            buildDrag = null,
            inspector = DebugInspector(state),
            pathfinding = DebugPathfindingTool(world, state),
            move = move
        )
        tools.select(DebugToolMode.MOVE)
        move.begin(PickedTarget.Object(placed, null), TilePosition(0, 0))
        move.dragTo(TilePosition(1, 1))

        tools.select(DebugToolMode.NONE)

        assertFalse(move.active)
        assertEquals(null, state.movePreview)
        assertSame(placed, world.getObjectAt(0, 0))
    }

    private fun objectEntryFor(world: World): com.mefabc24.strata.render.`object`.ObjectEntry {
        val registry = com.mefabc24.strata.render.`object`.ObjectRegistry(
            directory = "", queueTexture = {}, regionFor = { TextureRegion() }, loadAlphaMask = { null }
        )
        registry.register<TestPlaceable>("test", factory = ::TestPlaceable)
        return registry.constructibleEntries.single()
    }
}
