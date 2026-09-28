package com.mefabc24.strata.debug.inspector

import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.TileOffset
import com.mefabc24.strata.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugInspectorFormattingTest {
    @Test
    fun `rectangular footprints use compact dimensions`() {
        assertEquals("2 x 3", formatFootprint(Footprint.rectangle(2, 3)))
    }

    @Test
    fun `irregular footprints list their occupied offsets`() {
        val footprint = Footprint.custom(
            TileOffset(0, 1),
            TileOffset(1, 0),
            TileOffset(0, 0)
        )

        assertEquals("[(0,0), (1,0), (0,1)]", formatFootprint(footprint))
    }

    @Test
    fun `world positions use compact coordinate formatting`() {
        assertEquals("(2, -3)", formatTilePosition(TilePosition(2, -3)))
        assertEquals("(1.25, 2.50)", formatEntityPosition(EntityPosition(1.25f, 2.5f)))
    }
}
