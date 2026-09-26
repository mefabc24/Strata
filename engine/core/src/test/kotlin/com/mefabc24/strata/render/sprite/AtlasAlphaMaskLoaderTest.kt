package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.mefabc24.strata.testing.TestGdxEnvironment
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AtlasAlphaMaskLoaderTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `masks use exact regions pages frame order and bottom left coordinates`() {
        val directory = Files.createTempDirectory("strata-atlas-mask")
        val firstPage = directory.resolve("first.png")
        val secondPage = directory.resolve("second.png")
        val atlasFile = directory.resolve("world.atlas")

        writePage(firstPage.toFile().path, 4, 2) { pixmap ->
            opaque(pixmap, 0, 0)
            opaque(pixmap, 3, 1)
        }
        writePage(secondPage.toFile().path, 2, 2) { pixmap ->
            opaque(pixmap, 1, 0)
        }
        atlasFile.writeText(atlasText())

        try {
            AtlasAlphaMaskLoader { FileHandle(atlasFile.toFile()) }.use { loader ->
                val first = loader.load(
                    SpriteSource.AtlasRegion("world.atlas", "first")
                ).single()!!
                val neighbor = loader.load(
                    SpriteSource.AtlasRegion("world.atlas", "neighbor")
                ).single()!!
                val second = loader.load(
                    SpriteSource.AtlasRegion("world.atlas", "second")
                ).single()!!
                val animation = loader.load(
                    SpriteSource.AtlasAnimation("world.atlas", "walker", 0.1f)
                )

                assertTrue(first.isSolid(0.25f, 0.75f))
                assertFalse(first.isSolid(0.75f, 0.25f))
                assertFalse(neighbor.isSolid(0.25f, 0.75f))
                assertTrue(neighbor.isSolid(0.75f, 0.25f))
                assertTrue(second.isSolid(0.75f, 0.75f))
                assertTrue(animation[0]!!.isSolid(0.25f, 0.75f))
                assertTrue(animation[1]!!.isSolid(0.75f, 0.75f))
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun writePage(
        path: String,
        width: Int,
        height: Int,
        draw: (Pixmap) -> Unit
    ) {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        try {
            draw(pixmap)
            PixmapIO.writePNG(FileHandle(path), pixmap)
        } finally {
            pixmap.dispose()
        }
    }

    private fun opaque(pixmap: Pixmap, x: Int, y: Int) {
        pixmap.setColor(1f, 1f, 1f, 1f)
        pixmap.drawPixel(x, y)
    }

    private fun atlasText() = """
        first.png
        size: 4, 2
        format: RGBA8888
        filter: Nearest, Nearest
        repeat: none
        first
          rotate: false
          xy: 0, 0
          size: 2, 2
          orig: 2, 2
          offset: 0, 0
          index: -1
        neighbor
          rotate: false
          xy: 2, 0
          size: 2, 2
          orig: 2, 2
          offset: 0, 0
          index: -1
        walker
          rotate: false
          xy: 0, 0
          size: 2, 2
          orig: 2, 2
          offset: 0, 0
          index: 0

        second.png
        size: 2, 2
        format: RGBA8888
        filter: Nearest, Nearest
        repeat: none
        second
          rotate: false
          xy: 0, 0
          size: 2, 2
          orig: 2, 2
          offset: 0, 0
          index: -1
        walker
          rotate: false
          xy: 0, 0
          size: 2, 2
          orig: 2, 2
          offset: 0, 0
          index: 1
    """.trimIndent()
}
