package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.render.`object`.IsoObjectBounds
import com.mefabc24.strata.render.`object`.ObjectRenderingSettings
import com.mefabc24.strata.render.`object`.ObjectVisual
import com.mefabc24.strata.render.`object`.ResolvedObjectVisual
import com.mefabc24.strata.render.sprite.AlphaMask
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

    @Test
    fun `sprite picking uses the resolved state frame and its bounds`() {
        val world = World(3, 3) { _, _ -> TestTile() }
        val placed = requireNotNull(world.place(TestObject(), 1, 1))
        val projection = IsoProjection(TileGeometry(32f, 24f))
        val visible = ObjectVisual(
            texture = sizedRegion(16, 32),
            alphaMask = alphaMask(true)
        )
        val hidden = ObjectVisual(
            texture = sizedRegion(32, 16),
            alphaMask = alphaMask(false)
        )
        var resolved = ResolvedObjectVisual(visible, 0f, null)
        val picker = ObjectPicker(
            camera = OrthographicCamera(),
            projection = projection,
            world = world,
            visualFor = { null },
            resolvedVisualFor = { _, _ -> resolved }
        )
        val visibleBounds = IsoObjectBounds.calculate(
            projection,
            placed,
            resolved,
            Rectangle()
        )
        val x = visibleBounds.x + visibleBounds.width / 2f
        val y = visibleBounds.y + visibleBounds.height / 2f

        assertEquals(64f, visibleBounds.height)
        assertEquals(placed, picker.pickWorld(x, y))

        resolved = ResolvedObjectVisual(hidden, 0f, null)
        val hiddenBounds = IsoObjectBounds.calculate(
            projection,
            placed,
            resolved,
            Rectangle()
        )
        assertEquals(16f, hiddenBounds.height)
        assertNull(picker.pickWorld(x, hiddenBounds.y + hiddenBounds.height / 2f))
    }

    private fun sizedRegion(width: Int, height: Int): TextureRegion {
        return object : TextureRegion() {
            override fun getRegionWidth(): Int = width
            override fun getRegionHeight(): Int = height
        }
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
