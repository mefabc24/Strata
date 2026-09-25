package com.mefabc24.strata.render

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import kotlin.test.Test
import kotlin.test.assertEquals

class TerrainTopRendererTest {

    @Test
    fun `compact padded canvas selects only logical top face rows`() {
        val layout = TerrainTopSlices.calculate(
            textureWidth = 32,
            textureHeight = 32,
            tileWidth = 32f,
            faceHeight = 16f,
            logicalHeight = 24f
        )

        assertEquals(8, layout.sourceY)
        assertEquals(16, layout.sourceHeight)
        assertEquals(24, layout.sourceY + layout.sourceHeight)
    }

    @Test
    fun `standard canvas excludes its vertical side rows`() {
        val layout = TerrainTopSlices.calculate(
            textureWidth = 32,
            textureHeight = 32,
            tileWidth = 32f,
            faceHeight = 16f,
            logicalHeight = 32f
        )

        assertEquals(0, layout.sourceY)
        assertEquals(16, layout.sourceHeight)
    }

    @Test
    fun `top bounds follow logical face geometry at elevation`() {
        val projection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )
        val bounds = IsoTerrainTopBounds.calculate(
            projection = projection,
            x = 0,
            y = 0,
            elevation = 1,
            result = Rectangle()
        )

        assertEquals(-16f, bounds.x)
        assertEquals(-8f, bounds.y)
        assertEquals(32f, bounds.width)
        assertEquals(16f, bounds.height)
    }
}
