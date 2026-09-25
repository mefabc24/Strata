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
    fun `complete padded terrain canvas uses logical surface anchor`() {
        val bounds = IsoTerrainBounds.calculate(
            projection = projection,
            x = 0,
            y = 0,
            textureWidth = 32,
            textureHeight = 32,
            result = Rectangle(),
            elevation = 1
        )

        assertEquals(-16f, bounds.x)
        assertEquals(-16f, bounds.y)
        assertEquals(32f, bounds.width)
        assertEquals(32f, bounds.height)
    }

    @Test
    fun `fill level zero uses the complete surface sprite anchor`() {
        val surface = IsoTerrainBounds.calculate(
            projection = projection,
            x = 2,
            y = 3,
            textureWidth = 32,
            textureHeight = 32,
            result = Rectangle(),
            elevation = 4
        )
        val fill = fillBounds(elevation = 4, level = 0)

        assertEquals(surface, fill)
    }

    @Test
    fun `fill levels move by exactly one logical elevation step`() {
        val levelZero = fillBounds(elevation = 4, level = 0)
        val levelOne = fillBounds(elevation = 4, level = 1)
        val levelTwo = fillBounds(elevation = 4, level = 2)

        assertEquals(8f, levelZero.y - levelOne.y)
        assertEquals(16f, levelZero.y - levelTwo.y)
        assertEquals(projection.elevationStep, levelZero.y - levelOne.y)
    }

    private fun fillBounds(elevation: Int, level: Int): Rectangle {
        return IsoTerrainFillBounds.calculate(
            projection = projection,
            x = 2,
            y = 3,
            elevation = elevation,
            levelBelowSurface = level,
            textureWidth = 32,
            textureHeight = 32,
            result = Rectangle()
        )
    }
}
