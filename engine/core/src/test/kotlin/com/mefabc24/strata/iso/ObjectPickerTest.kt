package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.render.`object`.AlphaMask
import com.mefabc24.strata.render.`object`.IsoObjectBounds
import com.mefabc24.strata.render.`object`.ObjectRenderingSettings
import com.mefabc24.strata.render.`object`.ObjectVisual
import com.mefabc24.strata.render.sprite.SpriteFrames
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObjectPickerTest {

    private class TestTile : Tile

    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
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

    @Test
    fun `sprite picking uses the alpha mask for the current animation frame`() {
        val world = World(3, 3) { _, _ -> TestTile() }
        val placed = requireNotNull(world.place(TestObject(), 1, 1))
        val projection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )
        val solid = alphaMask(solid = true)
        val transparent = alphaMask(solid = false)
        val sprite = SpriteFrames.animated(
            frames = listOf(TextureRegion(), TextureRegion()),
            frameDuration = 0.5f
        )
        val visual = ObjectVisual(
            sprite = sprite,
            alphaMasks = listOf(solid, transparent),
            width = 32f,
            height = 32f
        )
        var animationTime = 0f
        val picker = ObjectPicker(
            camera = OrthographicCamera(),
            projection = projection,
            world = world,
            visualFor = { visual },
            animationTime = { animationTime }
        )
        val bounds = IsoObjectBounds.calculate(
            projection = projection,
            placed = placed,
            visual = visual,
            result = Rectangle()
        )
        val x = bounds.x + bounds.width / 2f
        val y = bounds.y + bounds.height / 2f

        assertEquals(placed, picker.pickWorld(x, y))

        animationTime = 0.5f

        assertNull(picker.pickWorld(x, y))
    }

    private fun alphaMask(solid: Boolean): AlphaMask {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)

        if (solid) {
            pixmap.setColor(1f, 1f, 1f, 1f)
            pixmap.drawPixel(0, 0)
        }

        return try {
            AlphaMask.fromPixmap(pixmap)
        } finally {
            pixmap.dispose()
        }
    }
}
