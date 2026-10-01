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

    @Test
    fun `drag packing is deterministic in all four directions`() {
        val expected = mapOf(
            TilePosition(10, 10) to listOf(
                TilePosition(6, 7), TilePosition(8, 7), TilePosition(10, 7),
                TilePosition(6, 10), TilePosition(8, 10), TilePosition(10, 10)
            ),
            TilePosition(2, 10) to listOf(
                TilePosition(6, 7), TilePosition(4, 7), TilePosition(2, 7),
                TilePosition(6, 10), TilePosition(4, 10), TilePosition(2, 10)
            ),
            TilePosition(10, 4) to listOf(
                TilePosition(6, 7), TilePosition(8, 7), TilePosition(10, 7),
                TilePosition(6, 4), TilePosition(8, 4), TilePosition(10, 4)
            ),
            TilePosition(2, 4) to listOf(
                TilePosition(6, 7), TilePosition(4, 7), TilePosition(2, 7),
                TilePosition(6, 4), TilePosition(4, 4), TilePosition(2, 4)
            )
        )

        expected.forEach { (end, origins) ->
            val placement = placement(Footprint.rectangle(2, 3))
            val drag = DebugBuildDragController(placement)
            assertTrue(drag.begin(TilePosition(6, 7)))
            assertTrue(drag.dragTo(end))
            assertEquals(origins, placement.previewOrigins())
            assertEquals(origins.size, drag.previewCount)
        }
    }

    @Test
    fun `drag endpoint that is not footprint aligned stops at last full step`() {
        val placement = placement(Footprint.rectangle(3, 2))
        val drag = DebugBuildDragController(placement)
        drag.begin(TilePosition(1, 1))

        drag.dragTo(TilePosition(8, 6))

        assertEquals(
            listOf(
                TilePosition(1, 1), TilePosition(4, 1), TilePosition(7, 1),
                TilePosition(1, 3), TilePosition(4, 3), TilePosition(7, 3),
                TilePosition(1, 5), TilePosition(4, 5), TilePosition(7, 5)
            ),
            placement.previewOrigins()
        )
    }

    @Test
    fun `begin fails cleanly without selection or while placement is disabled`() {
        val placement = PlacementController(World(3, 3) { _, _ -> TestTile })
        val drag = DebugBuildDragController(placement)

        assertFalse(drag.begin(TilePosition(1, 1)))
        placement.selectedFactory = { TestPlaceable(Footprint.square(1)) }
        placement.enabled = false
        assertFalse(drag.begin(TilePosition(1, 1)))

        assertFalse(drag.active)
        assertEquals(0, drag.previewCount)
        assertTrue(placement.previews.isEmpty())
    }

    @Test
    fun `inactive drag operations are no ops`() {
        val placement = placement(Footprint.square(1))
        val drag = DebugBuildDragController(placement)

        assertFalse(drag.dragTo(TilePosition(2, 2)))
        assertTrue(drag.finish(TilePosition(2, 2)).isEmpty())
        assertFalse(drag.cancel())
    }

    @Test
    fun `finish recomputes origins when release differs from last preview`() {
        val placement = placement(Footprint.square(2))
        val drag = DebugBuildDragController(placement)
        drag.begin(TilePosition(0, 0))
        drag.dragTo(TilePosition(2, 0))

        val placed = drag.finish(TilePosition(4, 0))

        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(2, 0), TilePosition(4, 0)),
            placed.map { TilePosition(it.x, it.y) }
        )
        assertFalse(drag.active)
        assertTrue(placement.previews.isEmpty())
    }

    private fun PlacementController.previewOrigins() = previews.map {
        TilePosition(it.placedObject.x, it.placedObject.y)
    }

    private fun placement(footprint: Footprint) = PlacementController(
        World(20, 20) { _, _ -> TestTile }
    ).apply { selectedFactory = { TestPlaceable(footprint) } }
}
