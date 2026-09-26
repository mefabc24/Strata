package com.mefabc24.strata.render.entity

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.WorldEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoEntityBoundsTest {

    @Test
    fun `entity sprite is bottom center anchored to projected position`() {
        val entity = WorldEntity(TestEntity, EntityPosition(1.5f, 1.5f))
        val visual = EntityVisual(
            texture = region(10, 20),
            offsetX = 2f,
            offsetY = 3f,
            width = 10f,
            height = 20f
        )

        val bounds = IsoEntityBounds.calculate(
            projection = IsoProjection(TileGeometry(32f, 24f)),
            entity = entity,
            visual = visual,
            result = Rectangle()
        )

        assertEquals(Rectangle(-3f, -21f, 10f, 20f), bounds)
    }

    private fun region(width: Int, height: Int): TextureRegion {
        return object : TextureRegion() {
            override fun getRegionWidth(): Int = width
            override fun getRegionHeight(): Int = height
        }
    }

    private data object TestEntity : Entity
}
