package com.mefabc24.strata.placement

import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlacementControllerTest {

    private class TestTile : Tile

    private class TestPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }

    private class LargePlaceable : Placeable {
        override val footprint = Footprint.square(2)
    }

    private class IdentifiedPlaceable(
        val id: Int
    ) : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `each single placement creates a unique placeable instance`() {
        val controller = PlacementController(createWorld())
        controller.selectedFactory = ::TestPlaceable

        val first = controller.placeAt(TilePosition(1, 1))
        val second = controller.placeAt(TilePosition(2, 2))

        assertNotNull(first)
        assertNotNull(second)
        assertNotSame(first.placeable, second.placeable)
        assertNotSame(first.placeable, controller.selectedPlaceable)
        assertNotSame(second.placeable, controller.selectedPlaceable)
    }

    @Test
    fun `normal hover produces one preview and hovering nothing clears it`() {
        val controller = PlacementController(createWorld())
        controller.selectedFactory = ::TestPlaceable

        controller.update(TilePosition(2, 3))

        assertEquals(listOf(TilePosition(2, 3)), controller.previewPositions())
        assertTrue(controller.previews.single().valid)

        controller.update(null)

        assertTrue(controller.previews.isEmpty())
    }

    @Test
    fun `preview reflects world and external validation`() {
        val world = createWorld()
        assertNotNull(world.place(TestPlaceable(), TilePosition(2, 2)))
        val controller = PlacementController(
            world = world,
            placementValidator = { _, position -> position.x != 1 }
        )
        controller.selectedFactory = ::TestPlaceable

        controller.update(TilePosition(1, 2))
        assertFalse(controller.previews.single().valid)

        controller.update(TilePosition(2, 2))
        assertFalse(controller.previews.single().valid)

        controller.update(TilePosition(3, 2))
        assertTrue(controller.previews.single().valid)
    }

    @Test
    fun `explicit positions produce ordered deduplicated previews`() {
        val controller = PlacementController(createWorld())
        controller.selectedFactory = ::TestPlaceable

        controller.previewAt(
            listOf(
                TilePosition(3, 1),
                TilePosition(1, 2),
                TilePosition(3, 1),
                TilePosition(2, 4)
            )
        )

        assertEquals(
            listOf(
                TilePosition(3, 1),
                TilePosition(1, 2),
                TilePosition(2, 4)
            ),
            controller.previewPositions()
        )
    }

    @Test
    fun `explicit previews survive hover updates until cleared`() {
        val controller = PlacementController(createWorld())
        controller.selectedFactory = ::TestPlaceable
        val explicit = listOf(TilePosition(1, 1), TilePosition(2, 2))

        controller.previewAt(explicit)
        controller.update(TilePosition(4, 4))

        assertEquals(explicit, controller.previewPositions())

        controller.clearPreviewPositions()
        assertTrue(controller.previews.isEmpty())

        controller.update(TilePosition(4, 4))
        assertEquals(listOf(TilePosition(4, 4)), controller.previewPositions())
    }

    @Test
    fun `valid explicit previews reserve occupied tiles in order`() {
        val controller = PlacementController(createWorld())
        controller.selectedFactory = ::LargePlaceable

        controller.previewAt(
            listOf(
                TilePosition(1, 1),
                TilePosition(2, 1),
                TilePosition(3, 1)
            )
        )

        assertEquals(listOf(true, false, true), controller.previews.map { it.valid })
    }

    @Test
    fun `batch placement preserves order deduplicates and creates unique instances`() {
        var nextId = 0
        val controller = PlacementController(createWorld())
        controller.selectedFactory = {
            IdentifiedPlaceable(nextId++)
        }
        val previewPlaceable = controller.selectedPlaceable

        val placed = controller.placeAt(
            listOf(
                TilePosition(3, 1),
                TilePosition(1, 2),
                TilePosition(3, 1),
                TilePosition(2, 4)
            )
        )

        assertEquals(
            listOf(
                TilePosition(3, 1),
                TilePosition(1, 2),
                TilePosition(2, 4)
            ),
            placed.map { TilePosition(it.x, it.y) }
        )
        assertEquals(4, nextId)
        assertEquals(3, placed.map { it.placeable }.toSet().size)
        assertTrue(placed.none { it.placeable === previewPlaceable })
    }

    @Test
    fun `batch placement skips positions rejected by the world`() {
        val world = createWorld()
        assertNotNull(world.place(TestPlaceable(), TilePosition(1, 1)))
        val controller = PlacementController(world)
        controller.selectedFactory = ::TestPlaceable

        val placed = controller.placeAt(
            listOf(
                TilePosition(0, 0),
                TilePosition(1, 1),
                TilePosition(2, 2),
                TilePosition(9, 9)
            )
        )

        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(2, 2)),
            placed.map { TilePosition(it.x, it.y) }
        )
    }

    @Test
    fun `batch placement respects the external validator`() {
        val controller = PlacementController(
            world = createWorld(),
            placementValidator = { _, position -> position.x % 2 == 0 }
        )
        controller.selectedFactory = ::TestPlaceable

        val placed = controller.placeAt(
            listOf(
                TilePosition(0, 1),
                TilePosition(1, 1),
                TilePosition(2, 1)
            )
        )

        assertEquals(
            listOf(TilePosition(0, 1), TilePosition(2, 1)),
            placed.map { TilePosition(it.x, it.y) }
        )
    }

    @Test
    fun `batch placement naturally rejects conflicts from earlier successes`() {
        val controller = PlacementController(createWorld())
        controller.selectedFactory = ::LargePlaceable

        val placed = controller.placeAt(
            listOf(
                TilePosition(1, 1),
                TilePosition(2, 1),
                TilePosition(3, 1)
            )
        )

        assertEquals(
            listOf(TilePosition(1, 1), TilePosition(3, 1)),
            placed.map { TilePosition(it.x, it.y) }
        )
    }

    @Test
    fun `disabled placement clears explicit state and retains selection`() {
        val controller = PlacementController(createWorld())
        val factory = ::TestPlaceable
        controller.selectedFactory = factory
        controller.previewAt(listOf(TilePosition(1, 1), TilePosition(2, 2)))

        controller.enabled = false

        assertTrue(controller.previews.isEmpty())
        assertSame(factory, controller.selectedFactory)
        assertNotNull(controller.selectedPlaceable)
        assertNull(controller.placeAt(2, 2))
        assertTrue(controller.placeAt(listOf(TilePosition(2, 2))).isEmpty())

        controller.enabled = true
        controller.update(TilePosition(4, 4))
        assertEquals(listOf(TilePosition(4, 4)), controller.previewPositions())
    }

    @Test
    fun `changing selected factory clears stale explicit previews`() {
        val controller = PlacementController(createWorld())
        controller.selectedFactory = ::TestPlaceable
        controller.previewAt(listOf(TilePosition(1, 1), TilePosition(2, 2)))
        val oldPreviewPlaceable = controller.selectedPlaceable

        controller.selectedFactory = ::LargePlaceable

        assertTrue(controller.previews.isEmpty())
        assertNotNull(controller.selectedPlaceable)
        assertNotSame(oldPreviewPlaceable, controller.selectedPlaceable)

        controller.update(TilePosition(3, 3))
        assertEquals(listOf(TilePosition(3, 3)), controller.previewPositions())
        assertTrue(controller.selectedPlaceable is LargePlaceable)
    }

    @Test
    fun `placement without a selected factory returns no objects`() {
        val controller = PlacementController(createWorld())

        assertNull(controller.placeAt(TilePosition(2, 2)))
        assertTrue(
            controller.placeAt(listOf(TilePosition(1, 1), TilePosition(2, 2)))
                .isEmpty()
        )
    }

    private fun PlacementController.previewPositions(): List<TilePosition> {
        return previews.map {
            TilePosition(it.placedObject.x, it.placedObject.y)
        }
    }

    private fun createWorld(): World {
        return World(5, 5) { _, _ -> TestTile() }
    }
}
