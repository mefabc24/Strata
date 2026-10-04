package com.mefabc24.strata.tools.atlas

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.GdxNativesLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextureAtlasPackerTest {
    @BeforeTest
    fun loadNatives() {
        GdxNativesLoader.load()
    }

    @Test
    fun `invalid source and atlas name fail clearly`() = withTemporaryDirectory {
        val missing = resolve("missing")
        val output = resolve("output")

        assertFailsWith<IllegalArgumentException> {
            TextureAtlasPacker.pack(config(missing, output))
        }

        val sourceFile = resolve("source-file").createFile()
        assertFailsWith<IllegalArgumentException> {
            TextureAtlasPacker.pack(config(sourceFile, output))
        }

        val empty = resolve("empty").createDirectories()
        assertFailsWith<IllegalArgumentException> {
            TextureAtlasPacker.pack(config(empty, output))
        }
        assertFailsWith<IllegalArgumentException> {
            TextureAtlasPacker.pack(config(empty, output, " "))
        }
    }

    @Test
    fun `static and indexed sources produce Strata compatible metadata`() =
        withTemporaryDirectory {
            val source = resolve("source").createDirectories()
            val output = resolve("runtime/atlas")
            writePng(source.resolve("grass.png"), 5, 3)
            writePng(source.resolve("house.png"), 4, 6)
            repeat(3) { index ->
                writePng(source.resolve("water_$index.png"), 3, 2)
            }

            val result = TextureAtlasPacker.pack(config(source, output))
            val data = TextureAtlas.TextureAtlasData(
                FileHandle(result.metadata.toFile()),
                FileHandle(output.toFile()),
                false
            )

            assertTrue(output.toFile().isDirectory)
            assertTrue(result.metadata.toFile().isFile)
            assertTrue(result.pages.isNotEmpty())
            assertEquals(Texture.TextureFilter.Nearest, data.pages.first().minFilter)
            assertEquals(Texture.TextureFilter.Nearest, data.pages.first().magFilter)

            val grass = data.regions.single { it.name == "grass" }
            assertEquals(5, grass.width)
            assertEquals(3, grass.height)
            assertEquals(grass.width, grass.originalWidth)
            assertEquals(grass.height, grass.originalHeight)
            assertFalse(grass.rotate)
            assertEquals(0, grass.degrees)
            assertEquals(0f, grass.offsetX)
            assertEquals(0f, grass.offsetY)

            assertTrue(data.regions.any { it.name == "house" })
            assertEquals(
                listOf(0, 1, 2),
                data.regions.filter { it.name == "water" }.map { it.index }
            )

            val metadata = result.metadata.readText()
            assertTrue("grass" in metadata)
            assertTrue("water" in metadata)
            assertFalse("rotate: true" in metadata)
        }

    @Test
    fun `repacking removes stale pages and preserves unrelated output`() =
        withTemporaryDirectory {
            val source = resolve("source").createDirectories()
            val output = resolve("output").createDirectories()
            writePng(source.resolve("grass.png"), 2, 2)
            output.resolve("tiles2.png").writeText("stale")
            output.resolve("other.png").writeText("unrelated")
            output.resolve("other.atlas").writeText("unrelated")

            TextureAtlasPacker.pack(config(source, output))
            val second = TextureAtlasPacker.pack(config(source, output))

            assertFalse(Files.exists(output.resolve("tiles2.png")))
            assertTrue(Files.exists(output.resolve("other.png")))
            assertTrue(Files.exists(output.resolve("other.atlas")))
            assertTrue(second.metadata.isRegularFile())
            assertTrue(second.pages.all(Path::isRegularFile))
        }

    private fun config(
        source: Path,
        output: Path,
        name: String = "tiles"
    ) = AtlasPackingConfig(source, output, name)

    private fun writePng(path: Path, width: Int, height: Int) {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        try {
            pixmap.setColor(1f, 1f, 1f, 1f)
            pixmap.fill()
            PixmapIO.writePNG(FileHandle(path.toFile()), pixmap)
        } finally {
            pixmap.dispose()
        }
    }

    private fun withTemporaryDirectory(block: Path.() -> Unit) {
        val directory = Files.createTempDirectory("strata-packer-test")
        try {
            directory.block()
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
