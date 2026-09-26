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
    fun `each placement creates a unique placeable instance`() {
        val world = createWorld()

        val controller = PlacementController(
            world = world
        )

        controller.selectedFactory =
            ::TestPlaceable

        val first = controller.placeAt(
            TilePosition(1, 1)
        )

        val second = controller.placeAt(
            TilePosition(2, 2)
        )

        assertNotNull(first)
        assertNotNull(second)

        assertTrue(
            first.placeable !== second.placeable
        )

        assertTrue(
            first.placeable !== controller.selectedPlaceable
        )

        assertTrue(
            second.placeable !== controller.selectedPlaceable
        )
    }

    @Test
    fun `does not place without selected object`() {
        val world = createWorld()

        val controller = PlacementController(
            world = world
        )

        assertNull(
            controller.placeAt(
                TilePosition(2, 2)
            )
        )
    }

    @Test
    fun `external validator can reject placement`() {
        val world = createWorld()

        val controller = PlacementController(
            world = world,
            placementValidator = { _, _ ->
                false
            }
        )

        controller.selectedFactory =
            ::TestPlaceable

        assertNull(
            controller.placeAt(
                TilePosition(2, 2)
            )
        )

        assertNull(
            world.getObjectAt(
                TilePosition(2, 2)
            )
        )
    }

    @Test
    fun `preview reflects external validation`() {
        val world = createWorld()

        val controller = PlacementController(
            world = world,
            placementValidator = { _, position ->
                position.x == 2
            }
        )

        controller.selectedFactory =
            ::TestPlaceable

        controller.update(
            TilePosition(1, 2)
        )

        assertFalse(
            requireNotNull(controller.preview).valid
        )

        controller.update(
            TilePosition(2, 2)
        )

        assertTrue(
            requireNotNull(controller.preview).valid
        )
    }

    @Test
    fun `external validator cannot override world restrictions`() {
        val world = createWorld()

        assertNotNull(
            world.place(
                placeable = TestPlaceable(),
                position = TilePosition(2, 2)
            )
        )

        val controller = PlacementController(
            world = world,
            placementValidator = { _, _ ->
                true
            }
        )

        controller.selectedFactory =
            ::TestPlaceable

        assertNull(
            controller.placeAt(
                TilePosition(2, 2)
            )
        )

        controller.update(
            TilePosition(2, 2)
        )

        assertFalse(
            requireNotNull(controller.preview).valid
        )
    }

    @Test
    fun `clears preview when nothing is hovered`() {
        val world = createWorld()

        val controller = PlacementController(
            world = world
        )

        controller.selectedFactory =
            ::TestPlaceable

        controller.update(
            TilePosition(2, 2)
        )

        assertNotNull(controller.preview)

        controller.update(null)

        assertNull(controller.preview)
    }

    @Test
    fun `disabled placement clears preview and retains selection`() {
        val controller = PlacementController(
            world = createWorld()
        )

        val factory = ::TestPlaceable
        controller.selectedFactory = factory
        controller.update(TilePosition(2, 2))

        assertNotNull(controller.preview)
        assertNotNull(controller.selectedPlaceable)

        controller.enabled = false

        assertNull(controller.preview)
        assertTrue(controller.selectedFactory === factory)
        assertNotNull(controller.selectedPlaceable)

        controller.update(TilePosition(3, 3))
        assertNull(controller.preview)
    }

    @Test
    fun `disabled placement cannot place selected object`() {
        val world = createWorld()
        val controller = PlacementController(world)

        controller.selectedFactory = ::TestPlaceable
        controller.enabled = false

        assertNull(controller.placeAt(2, 2))
        assertNull(world.getObjectAt(2, 2))
    }
}
