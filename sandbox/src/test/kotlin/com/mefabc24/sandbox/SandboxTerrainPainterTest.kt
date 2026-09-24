package com.mefabc24.sandbox

import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxTerrainPainterTest {

    private class MultiTileObject : Placeable {
        override val footprint = Footprint.square(2)
    }

    @Test
    fun `paint elevation cannot be negative`() {
        val painter = painter(world())

        assertFailsWith<IllegalArgumentException> {
            painter.elevation = -1
        }

        assertEquals(0, painter.elevation)
    }

    @Test
    fun `ground painting applies terrain and elevation`() {
        val world = world()
        val painter = painter(world).apply {
            terrain = TerrainType.WATER
            elevation = 2
        }

        assertTrue(painter.beginPaint(1, 1))

        assertEquals(
            TerrainType.WATER,
            (world.getTile(1, 1) as SandboxTile).terrain
        )
        assertEquals(2, world.getHeight(1, 1))
    }

    @Test
    fun `rejected height change leaves ground terrain unchanged`() {
        val world = world()
        assertTrue(world.place(MultiTileObject(), 0, 0) != null)

        val painter = painter(world).apply {
            terrain = TerrainType.WATER
            elevation = 1
        }

        assertTrue(painter.beginPaint(0, 0))

        assertEquals(
            TerrainType.GRASS,
            (world.getTile(0, 0) as SandboxTile).terrain
        )
        assertEquals(0, world.getHeight(0, 0))
    }

    @Test
    fun `overlay painting retains world elevation`() {
        val world = world().apply {
            addOverlayLayer("demo")
        }
        assertTrue(world.terrain.setHeight(1, 1, 1))

        val painter = painter(world).apply {
            layerId = "demo"
            terrain = TerrainType.WATER
            elevation = 3
        }

        assertTrue(painter.beginPaint(1, 1))

        assertEquals(1, world.getHeight(1, 1))
        assertEquals(
            TerrainType.WATER,
            (world.getOverlayTile("demo", 1, 1) as SandboxTile).terrain
        )
    }

    @Test
    fun `overlay layer choices reflect world registration order`() {
        val world = world().apply {
            addOverlayLayer("demo")
            addOverlayLayer("roads")
        }
        val painter = painter(world)

        assertEquals(listOf("demo", "roads"), painter.overlayLayerIds)

        painter.layerId = "roads"
        assertEquals("roads", painter.layerId)
    }

    @Test
    fun `layer cycling follows dynamic world order`() {
        val world = world().apply {
            addOverlayLayer("demo")
            addOverlayLayer("roads")
        }
        val painter = painter(world)

        painter.cycleLayer()
        assertEquals("demo", painter.layerId)

        painter.cycleLayer()
        assertEquals("roads", painter.layerId)

        painter.cycleLayer()
        assertNull(painter.layerId)
    }

    @Test
    fun `changing layer cancels the active stroke`() {
        val world = world().apply {
            addOverlayLayer("demo")
        }
        val painter = painter(world)

        assertTrue(painter.beginPaint(0, 0))
        painter.layerId = "demo"

        assertFalse(painter.dragPaint(1, 0))
        assertNull(world.getOverlayTile("demo", 1, 0))
    }

    @Test
    fun `continuous ground painting applies elevation along the stroke`() {
        val world = world()
        val painter = painter(world).apply {
            terrain = TerrainType.WATER
            elevation = 2
        }

        assertTrue(painter.beginPaint(0, 0))
        assertTrue(painter.dragPaint(3, 0))

        for (x in 0..3) {
            assertEquals(
                TerrainType.WATER,
                (world.getTile(x, 0) as SandboxTile).terrain
            )
            assertEquals(2, world.getHeight(x, 0))
        }
    }

    private fun painter(world: World): SandboxTerrainPainter {
        return SandboxTerrainPainter(world).apply {
            enabled = true
        }
    }

    private fun world(): World {
        return World(4, 4) { _, _ ->
            SandboxTile(TerrainType.GRASS)
        }
    }
}
