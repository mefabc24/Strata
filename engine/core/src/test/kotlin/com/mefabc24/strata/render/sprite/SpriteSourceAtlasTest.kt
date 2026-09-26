package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpriteSourceAtlasTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `atlas sources validate names and missing regions`() {
        assertFailsWith<IllegalArgumentException> {
            SpriteSource.AtlasRegion(" ", "grass")
        }
        assertFailsWith<IllegalArgumentException> {
            SpriteSource.AtlasRegion("world.atlas", " ")
        }
        assertFailsWith<IllegalStateException> {
            SpriteSource.AtlasRegion("world.atlas", "grass")
                .prepare({ TextureRegion() }, { TextureAtlas() })
        }
        assertFailsWith<IllegalStateException> {
            SpriteSource.AtlasAnimation("world.atlas", "water", 0.1f)
                .prepare({ TextureRegion() }, { TextureAtlas() })
        }
    }

    @Test
    fun `static and indexed atlas regions resolve through SpriteFrames`() {
        withAtlas { texture, atlas ->
            atlas.addRegion("grass", texture, 0, 0, 2, 2)
            atlas.addRegion("water", texture, 2, 0, 2, 2).index = 0
            atlas.addRegion("water", texture, 0, 2, 2, 2).index = 1

            val static = SpriteSource.AtlasRegion("world.atlas", "grass")
                .prepare({ TextureRegion() }, { atlas })
            val animated = SpriteSource.AtlasAnimation(
                "world.atlas",
                "water",
                0.2f
            ).prepare({ TextureRegion() }, { atlas })

            assertEquals(1, static.frameCount)
            assertEquals(0, static.frameAtIndex(0).regionX)
            assertEquals(2, animated.frameCount)
            assertEquals(listOf(2, 0), List(2) { animated.frameAtIndex(it).regionX })
            assertEquals(1, animated.frameIndexAt(0.2f))
        }
    }

    @Test
    fun `rotated and trimmed atlas regions fail clearly`() {
        withAtlas { texture, atlas ->
            val rotated = atlas.addRegion("rotated", texture, 0, 0, 2, 2)
            rotated.degrees = 90
            rotated.rotate = true
            val trimmed = atlas.addRegion("trimmed", texture, 2, 0, 2, 2)
            trimmed.originalWidth = 3

            assertFailsWith<IllegalArgumentException> {
                SpriteSource.AtlasRegion("world.atlas", "rotated")
                    .prepare({ TextureRegion() }, { atlas })
            }
            assertFailsWith<IllegalArgumentException> {
                SpriteSource.AtlasRegion("world.atlas", "trimmed")
                    .prepare({ TextureRegion() }, { atlas })
            }
        }
    }

    private fun withAtlas(block: (Texture, TextureAtlas) -> Unit) {
        val pixmap = Pixmap(4, 4, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val atlas = TextureAtlas()
        try {
            block(texture, atlas)
        } finally {
            atlas.dispose()
        }
    }
}
