package com.mefabc24.sandbox

import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.placement.PlacementPreviewBoundsPolicy
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.*

class SandboxPathPlacementTest {
    @Test
    fun `existing one tile sandbox object previews and places multiple segments`() {
        val placement = placement().apply { selectedFactory = ::Flower1 }
        placement.path.begin(TilePosition(1, 1))
        placement.path.addWaypoint(TilePosition(4, 2))
        placement.path.addWaypoint(TilePosition(5, 5))
        placement.path.update(TilePosition(8, 6))
        val expected = placement.previews.map { TilePosition(it.placedObject.x, it.placedObject.y) }
        val placed = placement.path.finish()
        assertEquals(expected, placed.map { TilePosition(it.x, it.y) })
        assertTrue(placed.all { it.placeable is Flower1 })
        assertFalse(placement.path.active)
        assertTrue(placement.previews.isEmpty())
        placement.path.begin(TilePosition(9, 9))
        placement.path.cancel()
        assertTrue(placement.path.positions.isEmpty())
        assertTrue(placement.path.begin(TilePosition(10, 10)))
        assertEquals(1, placement.path.finish().size)
    }

    @Test
    fun `sandbox house footprint still shows conflicts and skips invalid origins`() {
        val placement = placement().apply { selectedFactory = ::House }
        placement.path.begin(TilePosition(1, 1))
        placement.path.update(TilePosition(4, 1))
        assertEquals(listOf(true, false, true, false), placement.previews.map { it.valid })
        assertEquals(listOf(TilePosition(1, 1), TilePosition(3, 1)),
            placement.path.finish().map { TilePosition(it.x, it.y) })
    }

    private fun placement() = PlacementController(
        World(20, 20) { _, _ -> SandboxTile(TerrainType.LOW_GRASS) },
        previewBoundsPolicy = PlacementPreviewBoundsPolicy.ORIGIN_INSIDE
    )
}
