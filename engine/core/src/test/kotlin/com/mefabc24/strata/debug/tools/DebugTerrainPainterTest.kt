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
        val painter = painter(world).apply { layerId = "roads" }
        painter.beginPaint(0, 0)
        painter.dragPaint(3, 0)
        assertTrue(painter.beginErase(0, 0))
        assertTrue(painter.dragErase(3, 0))
        (0..3).forEach { assertNull(world.getOverlayTile("roads", it, 0)) }

        painter.beginPaint(0, 0)
        painter.layerId = null
        assertFalse(painter.dragPaint(1, 0))
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
