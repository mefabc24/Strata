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
    fun `complete padded terrain canvas follows logical elevation`() {
        val bounds = (0..2).map { elevation ->
            Rectangle(IsoTerrainBounds.calculate(
                projection = projection,
                x = 0,
                y = 0,
                textureWidth = 32,
                textureHeight = 32,
                result = Rectangle(),
                elevation = elevation
            ))
        }

        assertEquals(-16f, bounds[0].x)
        assertEquals(-24f, bounds[0].y)
        assertEquals(32f, bounds[0].width)
        assertEquals(32f, bounds[0].height)
        assertEquals(8f, bounds[1].y - bounds[0].y)
        assertEquals(16f, bounds[2].y - bounds[0].y)
        assertEquals(projection.elevationStep, bounds[1].y - bounds[0].y)
    }
}
