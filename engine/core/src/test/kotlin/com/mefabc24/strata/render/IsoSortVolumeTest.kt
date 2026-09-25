package com.mefabc24.strata.render

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
    fun `lower elevation is behind higher elevation`() {
        val lower = volume(minZ = 0, maxZ = 1)
        val higher = volume(minZ = 1, maxZ = 2)

        assertEquals(IsoSpatialRelation.BEHIND, lower.relationTo(higher))
        assertEquals(IsoSpatialRelation.IN_FRONT, higher.relationTo(lower))
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
    fun `identical zero thickness planes remain ambiguous`() {
        val first = volume(minZ = 2, maxZ = 2)
        val second = volume(minZ = 2, maxZ = 2)

        assertEquals(
            IsoSpatialRelation.AMBIGUOUS,
            first.relationTo(second)
        )
    }

    private fun volume(
        minX: Int = 0,
        maxX: Int = 1,
        minY: Int = 0,
        maxY: Int = 1,
        minZ: Int = 0,
        maxZ: Int = 1
    ): IsoSortVolume {
        return IsoSortVolume(
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            minZ = minZ,
            maxZ = maxZ
        )
    }
}
