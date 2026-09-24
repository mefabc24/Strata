package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.render.ObjectRenderingSettings
import com.mefabc24.strata.render.ObjectVisual
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObjectPickerTest {

    private class TestTile : Tile

    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `sprite picking follows the global object offset`() {
        val world = World(3, 3) { _, _ -> TestTile() }
        val placed = requireNotNull(
            world.place(TestObject(), 1, 1)
        )
        val visual = ObjectVisual(
            texture = TextureRegion(),
            width = 32f,
            height = 32f
        )
        val picker = ObjectPicker(
            camera = OrthographicCamera(),
            projection = IsoProjection(
                TileGeometry(width = 32f, height = 24f)
            ),
            world = world,
            visualFor = { visual },
            objectSettings = ObjectRenderingSettings(
                offsetX = 10f,
                offsetY = -4f
            )
        )

        assertEquals(placed, picker.pickWorld(24f, -20f))
        assertNull(picker.pickWorld(-14f, -20f))
    }
}
