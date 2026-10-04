package com.mefabc24.strata.render.debug

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IsoGridRendererTest {

    private val projection = IsoProjection(
        TileGeometry(width = 32f, height = 24f)
    )

    @Test
    fun `visible range covers viewport corners with one tile margin`() {
        val range = visibleDebugGridTileRange(
            projection = projection,
            minWorldX = -16f,
            minWorldY = -24f,
            maxWorldX = 16f,
            maxWorldY = 0f
        )

        assertEquals(-2..3, range.x)
        assertEquals(-2..3, range.y)
    }

    @Test
    fun `visible range follows camera area outside world coordinates`() {
        val range = visibleDebugGridTileRange(
            projection = projection,
            minWorldX = 300f,
            minWorldY = 100f,
            maxWorldX = 364f,
            maxWorldY = 148f
        )

        assertTrue(range.x.all { it < 10 })
        assertTrue(range.y.all { it < 0 })
        assertTrue(range.x.count() <= 10)
        assertTrue(range.y.count() <= 10)
    }
}
