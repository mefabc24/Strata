package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpriteFramesTest {

    @Test
    fun `static sprite always resolves its only texture`() {
        val texture = TextureRegion()
        val sprite = SpriteFrames.static(texture)

        assertFalse(sprite.isAnimated)
        assertEquals(1, sprite.frameCount)
        assertEquals(0, sprite.frameIndexAt(0f))
        assertEquals(0, sprite.frameIndexAt(10_000f))
        assertSame(texture, sprite.frameAt(0f))
        assertSame(texture, sprite.frameAt(10_000f))
    }

    @Test
    fun `animated sprite resolves and loops through libgdx animation`() {
        val frames = List(4) { TextureRegion() }
        val sprite = SpriteFrames.animated(
            frames = frames,
            frameDuration = 0.25f
        )

        assertTrue(sprite.isAnimated)
        assertEquals(4, sprite.frameCount)
        assertFrame(sprite, frames, stateTime = 0f, expectedIndex = 0)
        assertFrame(sprite, frames, stateTime = 0.25f, expectedIndex = 1)
        assertFrame(sprite, frames, stateTime = 0.75f, expectedIndex = 3)
        assertFrame(sprite, frames, stateTime = 1f, expectedIndex = 0)
        assertFrame(sprite, frames, stateTime = 10_000.5f, expectedIndex = 2)
    }

    @Test
    fun `animated sprite rejects empty frames`() {
        assertFailsWith<IllegalArgumentException> {
            SpriteFrames.animated(emptyList(), frameDuration = 0.1f)
        }
    }

    @Test
    fun `animated sprite rejects invalid frame durations`() {
        val frame = TextureRegion()

        for (
            duration in listOf(
                0f,
                -0.1f,
                Float.NaN,
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                SpriteFrames.animated(listOf(frame), duration)
            }
        }
    }

    @Test
    fun `animated sprite rejects mismatched frame dimensions`() {
        assertFailsWith<IllegalArgumentException> {
            SpriteFrames.animated(
                frames = listOf(
                    region(width = 16, height = 24),
                    region(width = 32, height = 24)
                ),
                frameDuration = 0.1f
            )
        }
    }

    private fun assertFrame(
        sprite: SpriteFrames,
        frames: List<TextureRegion>,
        stateTime: Float,
        expectedIndex: Int
    ) {
        assertEquals(expectedIndex, sprite.frameIndexAt(stateTime))
        assertSame(frames[expectedIndex], sprite.frameAt(stateTime))
    }

    private fun region(
        width: Int,
        height: Int
    ): TextureRegion {
        return object : TextureRegion() {
            override fun getRegionWidth(): Int = width
            override fun getRegionHeight(): Int = height
        }
    }
}
