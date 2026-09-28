package com.mefabc24.strata.debug.tools

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

class DebugBuildDragControllerTest {
    private data object TestTile : Tile
    private class TestPlaceable(override val footprint: Footprint) : Placeable

    @Test
    fun `rectangular footprints remain anchored in every drag direction`() {
        val placement = placement(Footprint.rectangle(2, 3))
        val drag = DebugBuildDragController(placement)
        assertTrue(drag.begin(TilePosition(6, 7)))
        assertTrue(drag.dragTo(TilePosition(2, 1)))
        assertEquals(
            listOf(
                TilePosition(6, 7), TilePosition(4, 7), TilePosition(2, 7),
                TilePosition(6, 4), TilePosition(4, 4), TilePosition(2, 4),
                TilePosition(6, 1), TilePosition(4, 1), TilePosition(2, 1)
            ),
            placement.previews.map { TilePosition(it.placedObject.x, it.placedObject.y) }
        )
    }

    @Test
    fun `finish uses preview origins and cancel clears active state`() {
        val placement = placement(Footprint.square(2))
        val drag = DebugBuildDragController(placement)
        drag.begin(TilePosition(1, 1))
        drag.dragTo(TilePosition(5, 5))
        val origins = placement.previews.map { TilePosition(it.placedObject.x, it.placedObject.y) }
        val placed = drag.finish(TilePosition(5, 5))
        assertEquals(origins, placed.map { TilePosition(it.x, it.y) })
        assertFalse(drag.active)
        assertTrue(placement.previews.isEmpty())
    }

    private fun placement(footprint: Footprint) = PlacementController(
        World(10, 10) { _, _ -> TestTile }
    ).apply { selectedFactory = { TestPlaceable(footprint) } }
}
