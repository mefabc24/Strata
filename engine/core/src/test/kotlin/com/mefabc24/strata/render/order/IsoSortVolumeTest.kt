package com.mefabc24.strata.render.order

import kotlin.test.Test
import kotlin.test.assertEquals

class IsoSortVolumeTest {

    @Test
    fun `separated world axes establish a definite back to front relation`() {
        val back = volume(minX = 0, maxX = 1)
        val front = volume(minX = 1, maxX = 2)

        assertEquals(IsoSpatialRelation.BEHIND, back.relationTo(front))
        assertEquals(IsoSpatialRelation.IN_FRONT, front.relationTo(back))
    }

    @Test
    fun `conflicting axis relations remain ambiguous`() {
        val first = volume(
            minX = 0,
            maxX = 1,
            minY = 1,
            maxY = 2
        )
        val second = volume(
            minX = 1,
            maxX = 2,
            minY = 0,
            maxY = 1
        )

        assertEquals(
            IsoSpatialRelation.AMBIGUOUS,
            first.relationTo(second)
        )
        assertEquals(
            IsoSpatialRelation.AMBIGUOUS,
            second.relationTo(first)
        )
    }

    @Test
    fun `identical flat volumes remain ambiguous`() {
        val first = volume()
        val second = volume()

        assertEquals(
            IsoSpatialRelation.AMBIGUOUS,
            first.relationTo(second)
        )
    }

    @Test
    fun `continuous point volumes sort across integer object bounds`() {
        val entityBehind = IsoSortVolume(0.5f, 0.5f, 1.5f, 1.5f)
        val entityInFront = IsoSortVolume(3.5f, 3.5f, 2.5f, 2.5f)
        val objectVolume = IsoSortVolume(2, 3, 2, 3)

        assertEquals(
            IsoSpatialRelation.BEHIND,
            entityBehind.relationTo(objectVolume)
        )
        assertEquals(
            IsoSpatialRelation.IN_FRONT,
            entityInFront.relationTo(objectVolume)
        )
    }

    private fun volume(
        minX: Int = 0,
        maxX: Int = 1,
        minY: Int = 0,
        maxY: Int = 1
    ): IsoSortVolume {
        return IsoSortVolume(
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY
        )
    }
}
