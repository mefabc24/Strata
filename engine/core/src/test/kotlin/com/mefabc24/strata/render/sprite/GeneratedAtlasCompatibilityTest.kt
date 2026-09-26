package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.tools.atlas.AtlasPackingConfig
import com.mefabc24.strata.tools.atlas.TextureAtlasPacker
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GeneratedAtlasCompatibilityTest {
    @Test
    fun `generated atlas is accepted by runtime sprite sources`() {
        TestGdxEnvironment.install()
        val root = Files.createTempDirectory("strata-runtime-atlas")
        val source = root.resolve("source")
        val output = root.resolve("output")
        Files.createDirectories(source)
        writePng(source.resolve("grass.png").toFile().path, 4, 3)
        writePng(source.resolve("water_0.png").toFile().path, 2, 2)
        writePng(source.resolve("water_1.png").toFile().path, 2, 2)

        try {
            val result = TextureAtlasPacker.pack(
                AtlasPackingConfig(source, output, "world")
            )
            val atlas = TextureAtlas(FileHandle(result.metadata.toFile()))
            try {
                assertNotNull(atlas.findRegion("grass"))
                assertEquals(
                    listOf(0, 1),
                    atlas.findRegions("water").map { it.index }
                )

                val static = SpriteSource.AtlasRegion("world.atlas", "grass")
                    .prepare({ TextureRegion() }, { atlas })
                val animated = SpriteSource.AtlasAnimation(
                    "world.atlas",
                    "water",
                    0.2f
                ).prepare({ TextureRegion() }, { atlas })

                assertEquals(1, static.frameCount)
                assertEquals(2, animated.frameCount)
            } finally {
                atlas.dispose()
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun writePng(path: String, width: Int, height: Int) {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        try {
            pixmap.setColor(1f, 1f, 1f, 1f)
            pixmap.fill()
            PixmapIO.writePNG(FileHandle(path), pixmap)
        } finally {
            pixmap.dispose()
        }
    }
}
