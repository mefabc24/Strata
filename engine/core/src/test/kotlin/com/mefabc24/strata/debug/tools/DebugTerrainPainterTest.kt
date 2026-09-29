package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugTerrainPainterTest {
    private enum class Terrain : TerrainId { GRASS, WATER }
    private data class TestTile(val terrain: Terrain) : Tile

    @Test
    fun `continuous strokes create fresh tiles and fill gaps`() {
        val world = world()
        val painter = painter(world).apply { selectedEntry = entries.last() }
        assertTrue(painter.beginPaint(0, 0))
        assertTrue(painter.dragPaint(3, 0))
        assertEquals(List(4) { Terrain.WATER }, (0..3).map {
            (world.getTile(it, 0) as TestTile).terrain
        })
        val tiles = (0..3).map { world.getTile(it, 0) }
        assertTrue(tiles.indices.all { first ->
            tiles.indices.all { second -> first == second || tiles[first] !== tiles[second] }
        })
    }

    @Test
    fun `overlay erase follows the stroke and layer changes cancel it`() {
        val world = world().apply { addOverlayLayer("roads") }
        val painter = painter(world).apply { target = DebugPaintTarget.OVERLAY }
        painter.beginPaint(0, 0)
        painter.dragPaint(3, 0)
        assertTrue(painter.beginErase(0, 0))
        assertTrue(painter.dragErase(3, 0))
        (0..3).forEach { assertNull(world.getOverlayTile("roads", it, 0)) }

        painter.beginPaint(0, 0)
        painter.target = DebugPaintTarget.GROUND
        assertFalse(painter.dragPaint(1, 0))
    }

    @Test
    fun `ground and overlay are distinct paint targets`() {
        val world = world().apply { addOverlayLayer("roads") }
        val painter = painter(world)

        assertEquals(DebugPaintTarget.GROUND, painter.target)
        assertNull(painter.activeOverlayLayerId)
        assertFalse(painter.beginErase(0, 0))

        painter.target = DebugPaintTarget.OVERLAY
        assertEquals("roads", painter.activeOverlayLayerId)
        assertTrue(painter.beginErase(0, 0))
    }

    @Test
    fun `switching target or overlay cancels active stroke`() {
        val world = world().apply {
            addOverlayLayer("roads")
            addOverlayLayer("details")
        }
        val painter = painter(world)
        assertTrue(painter.beginPaint(0, 0))

        painter.target = DebugPaintTarget.OVERLAY
        assertFalse(painter.dragPaint(1, 0))
        assertTrue(painter.beginPaint(0, 0))

        painter.selectedOverlayLayerId = "details"
        assertFalse(painter.dragPaint(1, 0))
        assertEquals("details", painter.activeOverlayLayerId)
    }

    @Test
    fun `multiple overlay layers can be selected independently`() {
        val world = world().apply {
            addOverlayLayer("roads")
            addOverlayLayer("details")
        }
        val painter = painter(world).apply { target = DebugPaintTarget.OVERLAY }
        painter.beginPaint(0, 0)
        painter.endPaint()

        painter.selectedOverlayLayerId = "details"
        painter.beginPaint(1, 0)

        assertEquals(Terrain.GRASS, (world.getOverlayTile("roads", 0, 0) as TestTile).terrain)
        assertEquals(Terrain.GRASS, (world.getOverlayTile("details", 1, 0) as TestTile).terrain)
        assertNull(world.getOverlayTile("roads", 1, 0))
    }

    @Test
    fun `world without overlays remains ground only`() {
        val painter = painter(world())

        assertTrue(painter.overlayLayerIds.isEmpty())
        assertEquals(DebugPaintTarget.GROUND, painter.target)
        assertNull(painter.selectedOverlayLayerId)
        assertFalse(painter.beginErase(0, 0))
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            painter.target = DebugPaintTarget.OVERLAY
        }
    }

    @Test
    fun `painting outside the world is safely ignored`() {
        val painter = painter(world())
        assertTrue(painter.beginPaint(-20, -20))
        assertTrue(painter.dragPaint(-10, -10))
    }

    private fun painter(world: World): DebugTerrainPainter {
        val registry = TerrainRegistry("", {}, { TextureRegion() })
        registry.register(Terrain.GRASS, "grass", factory = { TestTile(Terrain.GRASS) })
        registry.register(Terrain.WATER, "water", factory = { TestTile(Terrain.WATER) })
        return DebugTerrainPainter(world, registry.paintableEntries).apply { enabled = true }
    }

    private fun world() = World(4, 4) { _, _ -> TestTile(Terrain.GRASS) }
}
