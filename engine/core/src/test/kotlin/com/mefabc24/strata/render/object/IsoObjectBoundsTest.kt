package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TileOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class IsoObjectBoundsTest {
    private val projection = IsoProjection(TileGeometry(width = 32f, height = 24f))

    @Test
    fun `default width spans footprint bounding rectangle and preserves aspect ratio`() {
        val placed = PlacedObject(
            placeable(Footprint.rectangle(2, 3)),
            x = 1,
            y = 2
        )
        val result = Rectangle()

        val bounds = IsoObjectBounds.calculate(
            projection,
            placed,
            ObjectVisual(texture = region(20, 30)),
            result
        )

        assertSame(result, bounds)
        assertEquals(80f, bounds.width)
        assertEquals(120f, bounds.height)
        assertEquals(-64f, bounds.x)
        assertEquals(-64f, bounds.y)
    }

    @Test
    fun `explicit dimensions scale around the footprint center and front anchor`() {
        val placed = PlacedObject(placeable(Footprint.square(1)), 0, 0)

        val bounds = IsoObjectBounds.calculate(
            projection,
            placed,
            ObjectVisual(
                texture = region(10, 40),
                width = 20f,
                height = 30f,
                scale = 2f,
                offsetX = 3f,
                offsetY = 4f
            ),
            Rectangle(),
            ObjectRenderingSettings(offsetX = 5f, offsetY = 6f)
        )

        assertEquals(Rectangle(-12f, -6f, 40f, 60f), bounds)
    }

    @Test
    fun `irregular footprint uses complete coordinate span rather than tile count`() {
        val footprint = Footprint.custom(
            TileOffset(0, 0),
            TileOffset(3, 0),
            TileOffset(3, 2)
        )
        val placed = PlacedObject(placeable(footprint), 0, 0)

        val bounds = IsoObjectBounds.calculate(
            projection,
            placed,
            ObjectVisual(texture = region(1, 1)),
            Rectangle()
        )

        assertEquals(112f, bounds.width)
        assertEquals(112f, bounds.height)
        assertEquals(-48f, bounds.x)
        assertEquals(-56f, bounds.y)
    }

    private fun placeable(footprint: Footprint) = object : Placeable {
        override val footprint = footprint
    }

    private fun region(width: Int, height: Int): TextureRegion =
        object : TextureRegion() {
            override fun getRegionWidth() = width
            override fun getRegionHeight() = height
        }
}
