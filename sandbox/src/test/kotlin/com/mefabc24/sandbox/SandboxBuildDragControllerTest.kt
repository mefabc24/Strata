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
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class SandboxBuildDragControllerTest {

    private class TestTile : Tile

    private class TestPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `begin and drag preview an inclusive rectangle`() {
        val placement = placement()
        val drag = SandboxBuildDragController(placement)

        assertTrue(drag.begin(TilePosition(3, 2)))
        assertEquals(
            listOf(TilePosition(3, 2)),
            placement.previewPositions()
        )

        assertTrue(drag.dragTo(TilePosition(1, 1)))
        assertEquals(
            listOf(
                TilePosition(1, 1),
                TilePosition(2, 1),
                TilePosition(3, 1),
                TilePosition(1, 2),
                TilePosition(2, 2),
                TilePosition(3, 2)
            ),
            placement.previewPositions()
        )
    }

    @Test
    fun `finish places the final rectangle and clears drag state`() {
        val placement = placement()
        val drag = SandboxBuildDragController(placement)
        drag.begin(TilePosition(1, 1))

        val placed = drag.finish(TilePosition(2, 2))

        assertEquals(4, placed.size)
        assertEquals(
            listOf(
                TilePosition(1, 1),
                TilePosition(2, 1),
                TilePosition(1, 2),
                TilePosition(2, 2)
            ),
            placed.map { TilePosition(it.x, it.y) }
        )
        assertEquals(4, placed.map { it.placeable }.toSet().size)
        assertTrue(placed.none { it.placeable === placement.selectedPlaceable })
        assertFalse(drag.active)
        assertTrue(placement.previews.isEmpty())
    }

    @Test
    fun `click places exactly one fresh object`() {
        val placement = placement()
        val drag = SandboxBuildDragController(placement)
        val previewPlaceable = placement.selectedPlaceable
        val position = TilePosition(2, 3)

        drag.begin(position)
        val placed = drag.finish(position)

        assertEquals(1, placed.size)
        assertEquals(position, TilePosition(placed.single().x, placed.single().y))
        assertNotSame(previewPlaceable, placed.single().placeable)
    }

    @Test
    fun `cancel clears previews and places nothing`() {
        val world = world()
        val placement = PlacementController(world).apply {
            selectedFactory = ::TestPlaceable
        }
        val drag = SandboxBuildDragController(placement)
        drag.begin(TilePosition(1, 1))
        drag.dragTo(TilePosition(3, 3))

        assertTrue(drag.cancel())

        assertFalse(drag.active)
        assertTrue(placement.previews.isEmpty())
        assertTrue(world.getObjects().isEmpty())
        assertFalse(drag.cancel())
    }

    @Test
    fun `disabled placement cannot begin a drag`() {
        val placement = placement().apply {
            enabled = false
        }
        val drag = SandboxBuildDragController(placement)

        assertFalse(drag.begin(TilePosition(1, 1)))
        assertFalse(drag.active)
        assertTrue(placement.previews.isEmpty())
    }

    private fun placement(): PlacementController {
        return PlacementController(world()).apply {
            selectedFactory = ::TestPlaceable
        }
    }

    private fun PlacementController.previewPositions(): List<TilePosition> {
        return previews.map {
            TilePosition(it.placedObject.x, it.placedObject.y)
        }
    }

    private fun world(): World {
        return World(6, 6) { _, _ -> TestTile() }
    }
}
