package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.render.`object`.AlphaMask
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.entity.IsoEntityBounds
import com.mefabc24.strata.render.sprite.SpriteFrames
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EntityPickerTest {

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `picking uses current animation frame alpha mask`() {
        val world = World(3, 3) { _, _ -> TestTile }
        val entity = world.addEntity(TestEntity, EntityPosition(1.5f, 1.5f))
        val projection = IsoProjection(TileGeometry(32f, 24f))
        val visual = EntityVisual(
            sprite = SpriteFrames.animated(
                frames = listOf(region(16, 24), region(16, 24)),
                frameDuration = 0.5f
            ),
            alphaMasks = listOf(mask(true), mask(false)),
            width = 16f,
            height = 24f
        )
        var animationTime = 0f
        val picker = EntityPicker(
            camera = OrthographicCamera(),
            projection = projection,
            world = world,
            visualFor = { visual },
            animationTime = { animationTime }
        )
        val bounds = IsoEntityBounds.calculate(
            projection,
            entity,
            visual,
            Rectangle()
        )
        val x = bounds.x + bounds.width / 2f
        val y = bounds.y + bounds.height / 2f

        assertEquals(entity, picker.pickWorld(x, y))
        animationTime = 0.5f
        assertNull(picker.pickWorld(x, y))
    }

    private fun region(width: Int, height: Int): TextureRegion {
        return object : TextureRegion() {
            override fun getRegionWidth(): Int = width
            override fun getRegionHeight(): Int = height
        }
    }

    private fun mask(solid: Boolean): AlphaMask {
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

    private data object TestEntity : Entity
    private data object TestTile : Tile
}
