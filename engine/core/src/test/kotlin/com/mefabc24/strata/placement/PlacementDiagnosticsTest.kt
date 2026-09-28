package com.mefabc24.strata.placement

import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlacementDiagnosticsTest {
    private data object TestTile : Tile
    private class TestObject(override val footprint: Footprint = Footprint.square(1)) : Placeable

    @Test
    fun `diagnostics report authoritative engine failures`() {
        val world = World(3, 3) { _, _ -> TestTile }
        val placement = PlacementController(world, placementValidator = { _, position -> position.x != 2 })
        assertNull(placement.diagnose(TestObject(), TilePosition(0, 0)).reason)
        assertEquals(
            PlacementFailureReason.FOOTPRINT_OUTSIDE_WORLD,
            placement.diagnose(TestObject(Footprint.square(2)), TilePosition(2, 2)).reason
        )
        world.place(TestObject(), 1, 1)
        assertEquals(
            PlacementFailureReason.OCCUPIED_TILE,
            placement.diagnose(TestObject(), TilePosition(1, 1)).reason
        )
        assertEquals(
            PlacementFailureReason.EXTERNAL_VALIDATOR_REJECTED,
            placement.diagnose(TestObject(), TilePosition(2, 0)).reason
        )
        assertEquals(
            PlacementFailureReason.RESERVED_TILE_CONFLICT,
            placement.diagnose(TestObject(), TilePosition(0, 0), setOf(TilePosition(0, 0))).reason
        )
    }

    @Test
    fun `preview diagnostics align with previews`() {
        val controller = PlacementController(World(2, 2) { _, _ -> TestTile }).apply {
            selectedFactory = ::TestObject
        }
        controller.previewAt(listOf(TilePosition(0, 0), TilePosition(0, 0), TilePosition(3, 3)))
        assertEquals(controller.previews.size, controller.previewDiagnostics.size)
        assertTrue(controller.previewDiagnostics.first().valid)
    }

    @Test
    fun `current diagnostic remains available when an outside preview is hidden`() {
        val controller = PlacementController(World(2, 2) { _, _ -> TestTile }).apply {
            selectedFactory = { TestObject(Footprint.square(2)) }
        }

        controller.update(TilePosition(1, 1))

        assertTrue(controller.previews.isEmpty())
        assertEquals(
            PlacementFailureReason.FOOTPRINT_OUTSIDE_WORLD,
            controller.currentDiagnostic?.reason
        )
    }
}
