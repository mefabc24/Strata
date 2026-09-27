package com.mefabc24.sandbox

import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SandboxToolControllerTest {
    @Test
    fun `none is the initial safe mode`() {
        val fixture = fixture()

        assertEquals(SandboxMode.NONE, fixture.tools.mode)
        assertFalse(fixture.painter.enabled)
        assertFalse(fixture.placement.enabled)
    }

    @Test
    fun `each mode configures only its editing system`() {
        val fixture = fixture()

        fixture.tools.select(SandboxMode.BUILD)
        assertTrue(fixture.placement.enabled)
        assertFalse(fixture.painter.enabled)

        fixture.tools.select(SandboxMode.PAINT)
        assertFalse(fixture.placement.enabled)
        assertTrue(fixture.painter.enabled)

        fixture.tools.select(SandboxMode.SPAWN)
        assertFalse(fixture.placement.enabled)
        assertFalse(fixture.painter.enabled)

        fixture.tools.select(SandboxMode.NONE)
        assertFalse(fixture.placement.enabled)
        assertFalse(fixture.painter.enabled)
    }

    @Test
    fun `leaving build cancels drag and clears previews`() {
        val fixture = fixture()
        fixture.tools.select(SandboxMode.BUILD)
        fixture.buildDrag.begin(TilePosition(1, 1))

        assertTrue(fixture.buildDrag.active)
        assertTrue(fixture.placement.previews.isNotEmpty())

        fixture.tools.select(SandboxMode.SPAWN)

        assertFalse(fixture.buildDrag.active)
        assertTrue(fixture.placement.previews.isEmpty())
    }

    @Test
    fun `leaving paint ends an active stroke`() {
        val fixture = fixture()
        fixture.tools.select(SandboxMode.PAINT)
        assertTrue(fixture.painter.beginPaint(1, 1))

        fixture.tools.select(SandboxMode.NONE)

        assertFalse(fixture.painter.dragPaint(2, 1))
        assertFalse(fixture.painter.endPaint())
    }

    private fun fixture(): Fixture {
        val world = World(5, 5) { _, _ -> TestTile }
        val painter = SandboxTerrainPainter(world)
        val placement = PlacementController(world).apply {
            selectedFactory = ::TestPlaceable
        }
        val buildDrag = SandboxBuildDragController(placement)
        val tools = SandboxToolController(painter, placement, buildDrag)
        return Fixture(painter, placement, buildDrag, tools)
    }

    private data class Fixture(
        val painter: SandboxTerrainPainter,
        val placement: PlacementController,
        val buildDrag: SandboxBuildDragController,
        val tools: SandboxToolController
    )

    private data object TestTile : Tile

    private class TestPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }
}
