package com.mefabc24.strata.render.terrain

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class IsoTerrainBoundsGeometryTest {
    private val projection = IsoProjection(TileGeometry(width = 64f, height = 48f))

    @Test
    fun `terrain textures scale uniformly to tile width`() {
        val result = Rectangle()

        val bounds = IsoTerrainBounds.calculate(
            projection, x = 2, y = 1,
            textureWidth = 32, textureHeight = 48,
            result = result
        )

        assertSame(result, bounds)
        assertEquals(Rectangle(0f, -96f, 64f, 96f), bounds)
    }

    @Test
    fun `texture aspect ratios do not move the logical bottom alignment`() {
        val short = IsoTerrainBounds.calculate(
            projection, 0, 0, 64, 32, Rectangle()
        )
        val tall = IsoTerrainBounds.calculate(
            projection, 0, 0, 64, 128, Rectangle()
        )

        assertEquals(-48f, short.y)
        assertEquals(-48f, tall.y)
        assertEquals(32f, short.height)
        assertEquals(128f, tall.height)
    }

    @Test
    fun `terrain bounds reject zero and negative texture dimensions`() {
        for ((width, height) in listOf(0 to 1, 1 to 0, -1 to 1, 1 to -1)) {
            assertFailsWith<IllegalArgumentException> {
                IsoTerrainBounds.calculate(
                    projection, 0, 0, width, height, Rectangle()
                )
            }
        }
    }
}
