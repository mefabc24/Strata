package com.mefabc24.strata.placement

import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlacementControllerTest {

    private class TestTile : Tile

    private class TestPlaceable : Placeable {
        override val footprint =
            Footprint.square(1)
    }

    private fun createWorld(): World {
        return World(5, 5) { _, _ ->
            TestTile()
        }
    }

    @Test
    fun `allows placement when external validator accepts`() {
        val world = createWorld()

        val controller = PlacementController(
            world = world,
            placementValidator = { _, _ ->
                true
            }
        )

        controller.selectedPlaceable =
            TestPlaceable()

        val placed = controller.placeAt(
            TilePosition(2, 2)
        )

        assertNotNull(placed)
    }

    @Test
    fun `rejects placement when external validator rejects`() {
        val world = createWorld()

        val controller = PlacementController(
            world = world,
            placementValidator = { _, _ ->
                false
            }
        )

        controller.selectedPlaceable =
            TestPlaceable()

        val placed = controller.placeAt(
            TilePosition(2, 2)
        )

        assertNull(placed)
        assertNull(
            world.getObjectAt(
                TilePosition(2, 2)
            )
        )
    }

    @Test
    fun `preview reflects external placement validation`() {
        val world = createWorld()

        val controller = PlacementController(
            world = world,
            placementValidator = { _, position ->
                position.x == 2
            }
        )

        controller.selectedPlaceable =
            TestPlaceable()

        controller.update(
            TilePosition(1, 2)
        )

        assertFalse(
            controller.preview?.valid ?: true
        )

        controller.update(
            TilePosition(2, 2)
        )

        assertTrue(
            controller.preview?.valid ?: false
        )
    }

    @Test
    fun `external validator cannot override geometric restrictions`() {
        val world = createWorld()

        val existing = world.place(
            placeable = TestPlaceable(),
            position = TilePosition(2, 2)
        )

        assertNotNull(existing)

        val controller = PlacementController(
            world = world,
            placementValidator = { _, _ ->
                true
            }
        )

        controller.selectedPlaceable =
            TestPlaceable()

        assertNull(
            controller.placeAt(
                TilePosition(2, 2)
            )
        )
    }
}