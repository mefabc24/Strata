package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.input.WorldInputProcessor
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.placement.PlacementDiagnostic
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.*
import kotlin.test.*

class DebugBuildBindingsTest {
    private data object TestTile : Tile
    private class TestObject : Placeable { override val footprint = Footprint.square(1) }

    @BeforeTest
    fun installEnvironment() { TestGdxEnvironment.install() }

    @Test
    fun `path clicks confirm waypoints hover previews and Enter places without release committing`() {
        val fixture = Fixture()
        fixture.tools.buildShape = DebugBuildShape.PATH
        assertTrue(fixture.click(1, 1))
        fixture.tools.updateBuild(TilePosition(4, 2))
        assertEquals(0, fixture.world.getObjects().size)
        val firstSegment = fixture.previewPositions()
        assertTrue(fixture.click(4, 2))
        fixture.tools.updateBuild(TilePosition(5, 5))
        assertEquals(firstSegment, fixture.previewPositions().take(firstSegment.size))
        assertTrue(fixture.click(5, 5))
        fixture.tools.updateBuild(TilePosition(7, 6))
        val expected = fixture.previewPositions()
        assertEquals(3, fixture.placement.path.waypoints.size)
        assertEquals(expected.size, fixture.tools.buildPreviewCount)
        assertTrue(fixture.input.keyDown(Input.Keys.ENTER))
        assertEquals(expected, fixture.finished.single().map { TilePosition(it.x, it.y) })
        assertFalse(fixture.tools.buildPathActive)
        assertTrue(fixture.placement.previews.isEmpty())
        assertTrue(fixture.click(8, 8))
        assertTrue(fixture.tools.buildPathActive)
    }

    @Test
    fun `escape cancels path and lets later idle Escape propagate`() {
        val fixture = Fixture()
        fixture.tools.buildShape = DebugBuildShape.PATH
        fixture.click(1, 1)
        fixture.tools.updateBuild(TilePosition(3, 3))
        assertTrue(fixture.input.keyDown(Input.Keys.ESCAPE))
        assertFalse(fixture.tools.buildPathActive)
        assertTrue(fixture.placement.previews.isEmpty())
        assertTrue(fixture.world.getObjects().isEmpty())
        assertFalse(fixture.input.keyDown(Input.Keys.ESCAPE))
        assertFalse(fixture.input.keyDown(Input.Keys.ENTER))
        assertTrue(fixture.click(4, 4))
    }

    @Test
    fun `path must start inside world but active waypoints and dragging may leave bounds`() {
        val fixture = Fixture()
        fixture.tools.buildShape = DebugBuildShape.PATH
        assertFalse(fixture.click(-1, 1))
        assertFalse(fixture.tools.buildPathActive)
        assertTrue(fixture.click(8, 1))
        assertTrue(fixture.input.touchDown(12, 1, 0, Input.Buttons.LEFT))
        assertTrue(fixture.input.touchDragged(13, 1, 0))
        assertTrue(fixture.input.touchUp(13, 1, 0, Input.Buttons.LEFT))
        assertEquals(listOf(TilePosition(8, 1), TilePosition(12, 1)), fixture.placement.path.waypoints)
        assertEquals(TilePosition(13, 1), fixture.placement.path.positions.last())
        fixture.input.keyDown(Input.Keys.ENTER)
        assertEquals(listOf(TilePosition(8, 1), TilePosition(9, 1)),
            fixture.finished.single().map { TilePosition(it.x, it.y) })
    }

    @Test
    fun `default rectangle keeps single click and drag release behavior`() {
        val fixture = Fixture()
        assertEquals(DebugBuildShape.RECTANGLE, fixture.tools.buildShape)
        assertTrue(fixture.click(1, 1))
        assertEquals(1, fixture.finished.single().size)
        assertTrue(fixture.input.touchDown(4, 4, 0, Input.Buttons.LEFT))
        assertTrue(fixture.input.touchDragged(5, 5, 0))
        assertEquals(4, fixture.tools.buildPreviewCount)
        val expected = fixture.previewPositions()
        assertTrue(fixture.input.touchUp(5, 5, 0, Input.Buttons.LEFT))
        assertEquals(expected, fixture.finished.last().map { TilePosition(it.x, it.y) })
        assertFalse(fixture.tools.buildDragging)
        assertEquals(5, fixture.world.getObjects().size)
    }

    @Test
    fun `path bindings leave right click to existing removal policy`() {
        val fixture = Fixture()
        fixture.tools.buildShape = DebugBuildShape.PATH
        fixture.click(1, 1)
        assertFalse(fixture.input.touchDown(2, 2, 0, Input.Buttons.RIGHT))
        assertFalse(fixture.input.touchUp(2, 2, 0, Input.Buttons.RIGHT))
        assertTrue(fixture.tools.buildPathActive)
        assertEquals(listOf(TilePosition(1, 1)), fixture.placement.path.waypoints)
        assertTrue(fixture.finished.isEmpty())
    }

    @Test
    fun `shape object and tool changes cancel paths and allow another clean gesture`() {
        val fixture = Fixture()
        fixture.input.touchDown(1, 1, 0, Input.Buttons.LEFT)
        fixture.tools.buildShape = DebugBuildShape.PATH
        assertFalse(fixture.tools.buildDragging)
        fixture.click(2, 2)
        fixture.tools.buildShape = DebugBuildShape.RECTANGLE
        assertFalse(fixture.tools.buildPathActive)
        assertTrue(fixture.placement.previews.isEmpty())
        fixture.tools.buildShape = DebugBuildShape.PATH
        fixture.click(3, 3)
        fixture.tools.selectBuildEntry(null)
        assertFalse(fixture.tools.buildPathActive)
        assertTrue(fixture.placement.previews.isEmpty())
        fixture.tools.selectBuildEntry(fixture.entry)
        fixture.click(4, 4)
        fixture.tools.select(DebugToolMode.NONE)
        assertFalse(fixture.tools.buildPathActive)
        assertTrue(fixture.placement.previews.isEmpty())
        assertFalse(fixture.click(5, 5))
        assertFalse(fixture.tools.dragBuild(TilePosition(5, 5)))
        fixture.tools.select(DebugToolMode.BUILD)
        assertTrue(fixture.click(6, 6))
        assertEquals(listOf(TilePosition(6, 6)), fixture.placement.path.positions)
        assertTrue(fixture.tools.cancelBuildPath())
        assertFalse(fixture.tools.cancelBuildPath())
    }

    @Test
    fun `completion retains rejection diagnostic before clearing previews`() {
        val fixture = Fixture()
        fixture.world.place(TestObject(), 1, 1)
        fixture.tools.buildShape = DebugBuildShape.PATH
        fixture.click(1, 1)
        fixture.input.keyDown(Input.Keys.ENTER)
        assertTrue(fixture.finished.single().isEmpty())
        assertEquals(false, fixture.diagnostics.single()?.valid)
        assertNull(fixture.placement.currentDiagnostic)
    }

    private class Fixture {
        val world = World(10, 10) { _, _ -> TestTile }
        val placement = PlacementController(world)
        val entry = ObjectRegistry("", {}, { TextureRegion() }, { null }).apply {
            register<TestObject>("test", factory = ::TestObject)
        }.constructibleEntries.single()
        private val state = DebugWorldState()
        val tools = DebugToolController(
            DebugTerrainPainter(world, emptyList()), placement, DebugBuildDragController(placement),
            DebugInspector(state), DebugPathfindingTool(world, state)
        ).apply {
            selectBuildEntry(entry)
            select(DebugToolMode.BUILD)
        }
        val finished = mutableListOf<List<PlacedObject>>()
        val diagnostics = mutableListOf<PlacementDiagnostic?>()
        val input = WorldInputProcessor(
            debugBuildBindings(tools, placement) { placed, diagnostic ->
                finished += placed
                diagnostics += diagnostic
            },
            pickTile = { x, y -> TilePosition(x.toInt(), y.toInt()).takeIf(world::contains) },
            pickGrid = { x, y -> TilePosition(x.toInt(), y.toInt()) },
            pickObject = { _, _, _ -> null }
        )
        fun click(x: Int, y: Int): Boolean {
            val handled = input.touchDown(x, y, 0, Input.Buttons.LEFT)
            input.touchUp(x, y, 0, Input.Buttons.LEFT)
            return handled
        }
        fun previewPositions() = placement.previews.map { TilePosition(it.placedObject.x, it.placedObject.y) }
    }
}
