package com.mefabc24.strata.render

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import kotlin.test.Test
import kotlin.test.assertEquals

class TerrainRendererTest {

    private val projection = IsoProjection(
        TileGeometry(width = 32f, height = 24f)
    )

    @Test
    fun `complete padded terrain canvas uses compact logical anchor`() {
        val bounds = IsoTerrainBounds.calculate(
            projection = projection,
            x = 0,
            y = 0,
            textureWidth = 32,
            textureHeight = 32,
            result = Rectangle()
        )

        assertEquals(-16f, bounds.x)
        assertEquals(-24f, bounds.y)
        assertEquals(32f, bounds.width)
        assertEquals(32f, bounds.height)
    }
}
