package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoObjectOrderingTest {

    private class SmallObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    private class MediumObject : Placeable {
        override val footprint = Footprint.square(2)
    }

    private class LargeObject : Placeable {
        override val footprint = Footprint.square(4)
    }

    private val projection = IsoProjection(
        TileGeometry(width = 32f, height = 24f)
    )

    @Test
    fun `orders one tile objects back to front`() {
        val behind = placed(SmallObject(), x = 1, y = 1)
        val inFront = placed(SmallObject(), x = 2, y = 1)

        assertEquals(
            listOf(behind, inFront),
            order(inFront, behind)
        )
    }

    @Test
    fun `tree in front of large house renders after house`() {
        val house = placed(LargeObject(), x = 2, y = 2)
        val tree = placed(SmallObject(), x = 6, y = 3)

        assertEquals(10, house.occupiedTiles().maxOf { it.x + it.y })
        assertEquals(9, tree.occupiedTiles().maxOf { it.x + it.y })

        assertEquals(
            listOf(house, tree),
            order(tree, house)
        )
    }

    @Test
    fun `tree behind large house renders before house`() {
        val house = placed(LargeObject(), x = 2, y = 2)
        val tree = placed(SmallObject(), x = 1, y = 3)

        assertEquals(
            listOf(tree, house),
            order(house, tree)
        )
    }

    @Test
    fun `ambiguous beside placement has deterministic fallback`() {
        val house = placed(LargeObject(), x = 2, y = 2)
        val tree = placed(SmallObject(), x = 6, y = 1)

        assertEquals(
            order(house, tree),
            order(tree, house)
        )
    }

    @Test
    fun `large footprints use their complete occupied areas`() {
        val behind = placed(MediumObject(), x = 0, y = 0)
        val inFront = placed(LargeObject(), x = 2, y = 0)

        assertEquals(
            listOf(behind, inFront),
            order(inFront, behind)
        )
    }

    @Test
    fun `fallback accounts for terrain elevation`() {
        val flat = placed(SmallObject(), x = 2, y = 0)
        val elevated = placed(SmallObject(), x = 0, y = 2)

        assertEquals(
            listOf(elevated, flat),
            order(flat, elevated) { placed ->
                if (placed === elevated) 2 else 0
            }
        )
    }

    private fun order(
        vararg placed: PlacedObject,
        elevationFor: (PlacedObject) -> Int = { 0 }
    ): List<PlacedObject> {
        return IsoObjectOrdering.backToFront(
            objects = placed.toList(),
            projection = projection,
            elevationFor = elevationFor
        )
    }

    private fun placed(
        placeable: Placeable,
        x: Int,
        y: Int
    ) = PlacedObject(placeable, x, y)
}
