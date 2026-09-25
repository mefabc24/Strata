package com.mefabc24.sandbox

import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxTerrainPainterTest {

    @Test
    fun `ground painting replaces the selected terrain`() {
        val world = world()
        val painter = painter(world).apply {
            terrain = TerrainType.WATER
        }

        assertTrue(painter.beginPaint(1, 1))

        assertEquals(
            TerrainType.WATER,
            (world.getTile(1, 1) as SandboxTile).terrain
        )
    }

    @Test
    fun `overlay painting leaves ground terrain unchanged`() {
        val world = world().apply {
            addOverlayLayer("demo")
        }
        val painter = painter(world).apply {
            layerId = "demo"
            terrain = TerrainType.WATER
        }

        assertTrue(painter.beginPaint(1, 1))

        assertEquals(
            TerrainType.GRASS,
            (world.getTile(1, 1) as SandboxTile).terrain
        )
        assertEquals(
            TerrainType.WATER,
            (world.getOverlayTile("demo", 1, 1) as SandboxTile).terrain
        )
    }

    @Test
    fun `overlay layer choices reflect world registration order`() {
        val painter = painter(world().apply {
            addOverlayLayer("demo")
            addOverlayLayer("roads")
        })

        assertEquals(listOf("demo", "roads"), painter.overlayLayerIds)

        painter.layerId = "roads"
        assertEquals("roads", painter.layerId)
    }

    @Test
    fun `layer cycling follows dynamic world order`() {
        val painter = painter(world().apply {
            addOverlayLayer("demo")
            addOverlayLayer("roads")
        })

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
    fun `continuous ground painting fills every traversed tile`() {
        val world = world()
        val painter = painter(world).apply {
            terrain = TerrainType.WATER
        }

        assertTrue(painter.beginPaint(0, 0))
        assertTrue(painter.dragPaint(3, 0))

        for (x in 0..3) {
            assertEquals(
                TerrainType.WATER,
                (world.getTile(x, 0) as SandboxTile).terrain
            )
        }
    }

    @Test
    fun `overlay erase clears every traversed tile`() {
        val world = world().apply {
            addOverlayLayer("demo")
            for (x in 0..3) {
                setOverlayTile(
                    "demo",
                    x,
                    0,
                    SandboxTile(TerrainType.WATER)
                )
            }
        }
        val painter = painter(world).apply {
            layerId = "demo"
        }

        assertTrue(painter.beginErase(0, 0))
        assertTrue(painter.dragErase(3, 0))

        for (x in 0..3) {
            assertNull(world.getOverlayTile("demo", x, 0))
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
