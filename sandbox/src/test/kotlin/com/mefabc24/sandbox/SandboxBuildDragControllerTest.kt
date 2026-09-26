package com.mefabc24.sandbox

import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.FootprintOrigin
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class SandboxBuildDragControllerTest {

    private class TestTile : Tile

    private class TestPlaceable(
        override val footprint: Footprint
    ) : Placeable

    @Test
    fun `one by one footprint uses every tile in the area`() {
        val placement = placement(Footprint.square(1))
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
    fun `two by two footprint uses its width and height as spacing`() {
        val placement = placement(Footprint.square(2))
        val drag = SandboxBuildDragController(placement)

        drag.begin(TilePosition(1, 1))
        drag.dragTo(TilePosition(5, 5))

        assertEquals(
            listOf(
                TilePosition(1, 1),
                TilePosition(3, 1),
                TilePosition(1, 3),
                TilePosition(3, 3)
            ),
            placement.previewPositions()
        )
    }

    @Test
    fun `two by three footprint uses rectangular spacing`() {
        val placement = placement(Footprint.rectangle(2, 3))
        val drag = SandboxBuildDragController(placement)

        drag.begin(TilePosition(1, 1))
        drag.dragTo(TilePosition(5, 7))

        assertEquals(
            listOf(
                TilePosition(1, 1),
                TilePosition(3, 1),
                TilePosition(1, 4),
                TilePosition(3, 4)
            ),
            placement.previewPositions()
        )
    }

    @Test
    fun `all drag directions produce the same normalized origins`() {
        val placement = placement(Footprint.square(2))
        val drag = SandboxBuildDragController(placement)
        val expected = listOf(
            TilePosition(1, 1),
            TilePosition(3, 1),
            TilePosition(1, 3),
            TilePosition(3, 3)
        )
        val drags = listOf(
            TilePosition(1, 1) to TilePosition(5, 5),
            TilePosition(5, 5) to TilePosition(1, 1),
            TilePosition(5, 1) to TilePosition(1, 5),
            TilePosition(1, 5) to TilePosition(5, 1)
        )

        for ((start, end) in drags) {
            drag.begin(start)
            drag.dragTo(end)

            assertEquals(expected, placement.previewPositions())
            drag.cancel()
        }
    }

    @Test
    fun `packed origins account for footprint origin and stay inside the area`() {
        val placement = placement(
            Footprint.rectangle(
                width = 2,
                height = 3,
                origin = FootprintOrigin.SOUTH
            )
        )
        val drag = SandboxBuildDragController(placement)
        val min = TilePosition(1, 1)
        val max = TilePosition(5, 7)

        drag.begin(min)
        drag.dragTo(max)

        assertEquals(
            listOf(
                TilePosition(2, 3),
                TilePosition(4, 3),
                TilePosition(2, 6),
                TilePosition(4, 6)
            ),
            placement.previewPositions()
        )
        assertTrue(
            placement.previews.all { preview ->
                preview.placedObject.occupiedTiles().all { tile ->
                    tile.x in min.x..max.x && tile.y in min.y..max.y
                }
            }
        )
    }

    @Test
    fun `preview and final placement use the same packed origins`() {
        val placement = placement(Footprint.square(2))
        val drag = SandboxBuildDragController(placement)
        drag.begin(TilePosition(1, 1))
        drag.dragTo(TilePosition(5, 5))
        val previewPositions = placement.previewPositions()

        val placed = drag.finish(TilePosition(5, 5))

        assertEquals(
            previewPositions,
            placed.map { TilePosition(it.x, it.y) }
        )
        assertEquals(placed.size, placed.map { it.placeable }.toSet().size)
        assertTrue(placed.none { it.placeable === placement.selectedPlaceable })
        assertFalse(drag.active)
        assertTrue(placement.previews.isEmpty())
    }

    @Test
    fun `empty world packed previews are all valid`() {
        val placement = placement(Footprint.square(2))
        val drag = SandboxBuildDragController(placement)

        drag.begin(TilePosition(1, 1))
        drag.dragTo(TilePosition(6, 6))

        assertEquals(9, placement.previews.size)
        assertTrue(placement.previews.all { it.valid })
    }

    @Test
    fun `click with a larger footprint previews and places one fresh object`() {
        val placement = placement(Footprint.square(2))
        val drag = SandboxBuildDragController(placement)
        val previewPlaceable = placement.selectedPlaceable
        val position = TilePosition(2, 3)

        drag.begin(position)

        assertEquals(listOf(position), placement.previewPositions())

        val placed = drag.finish(position)

        assertEquals(1, placed.size)
        assertEquals(position, TilePosition(placed.single().x, placed.single().y))
        assertNotSame(previewPlaceable, placed.single().placeable)
    }

    @Test
    fun `existing world collision remains invalid and is rejected`() {
        val footprint = Footprint.square(2)
        val world = world()
        val placement = placement(footprint, world)
        val drag = SandboxBuildDragController(placement)
        assertNotNull(
            world.place(
                TestPlaceable(footprint),
                TilePosition(1, 1)
            )
        )

        drag.begin(TilePosition(1, 1))
        drag.dragTo(TilePosition(4, 4))

        assertEquals(
            listOf(false, true, true, true),
            placement.previews.map { it.valid }
        )

        val placed = drag.finish(TilePosition(4, 4))

        assertEquals(
            listOf(
                TilePosition(3, 1),
                TilePosition(1, 3),
                TilePosition(3, 3)
            ),
            placed.map { TilePosition(it.x, it.y) }
        )
    }

    @Test
    fun `cancel clears previews and places nothing`() {
        val world = world()
        val placement = placement(Footprint.square(1), world)
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
        val placement = placement(Footprint.square(1)).apply {
            enabled = false
        }
        val drag = SandboxBuildDragController(placement)

        assertFalse(drag.begin(TilePosition(1, 1)))
        assertFalse(drag.active)
        assertTrue(placement.previews.isEmpty())
    }

    private fun placement(
        footprint: Footprint,
        world: World = world()
    ): PlacementController {
        return PlacementController(world).apply {
            selectedFactory = { TestPlaceable(footprint) }
        }
    }

    private fun PlacementController.previewPositions(): List<TilePosition> {
        return previews.map {
            TilePosition(it.placedObject.x, it.placedObject.y)
        }
    }

    private fun world(): World {
        return World(10, 10) { _, _ -> TestTile() }
    }
}
