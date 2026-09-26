package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.mefabc24.strata.render.`object`.AlphaMask
import java.io.Closeable

/**
 * Reads atlas page images during visual preparation and copies each region's
 * alpha channel. Page Pixmaps are cached for the batch and always disposed.
 */
internal class AtlasAlphaMaskLoader(
    private val atlasFileFor: (String) -> FileHandle = {
        Gdx.files.classpath(it)
    }
) : Closeable {
    private val atlasData = mutableMapOf<String, TextureAtlas.TextureAtlasData>()
    private val pagePixmaps = mutableMapOf<String, Pixmap>()
    private val masks = mutableMapOf<RegionKey, AlphaMask>()

    fun load(source: SpriteSource): List<AlphaMask?> {
        val atlasPath: String
        val regionName: String
        val animated: Boolean

        when (source) {
            is SpriteSource.AtlasRegion -> {
                atlasPath = source.atlas
                regionName = source.region
                animated = false
            }

            is SpriteSource.AtlasAnimation -> {
                atlasPath = source.atlas
                regionName = source.region
                animated = true
            }

            else -> error("Atlas alpha masks require an atlas sprite source.")
        }

        val data = atlasData.getOrPut(atlasPath) {
            val atlasFile = atlasFileFor(atlasPath)
            TextureAtlas.TextureAtlasData(
                atlasFile,
                atlasFile.parent(),
                false
            )
        }
        val matches = data.regions.filter { it.name == regionName }
        val regions = if (animated) matches else matches.take(1)

        check(regions.isNotEmpty()) {
            "Texture atlas '$atlasPath' has no region named '$regionName'."
        }

        return regions.map { region ->
            validateRegion(atlasPath, region)
            val pageFile = checkNotNull(region.page.textureFile) {
                "Texture atlas '$atlasPath' page '${region.page.name}' has no image file."
            }
            val pageKey = pageFile.path()
            val pixmap = pagePixmaps.getOrPut(pageKey) { Pixmap(pageFile) }
            val key = RegionKey(
                atlas = atlasPath,
                page = pageKey,
                left = region.left,
                top = region.top,
                width = region.width,
                height = region.height
            )
            masks.getOrPut(key) {
                AlphaMask.fromPixmap(
                    pixmap = pixmap,
                    x = region.left,
                    y = region.top,
                    width = region.width,
                    height = region.height
                )
            }
        }
    }

    override fun close() {
        pagePixmaps.values.forEach(Pixmap::dispose)
        pagePixmaps.clear()
    }

    private fun validateRegion(
        atlasPath: String,
        region: TextureAtlas.TextureAtlasData.Region
    ) {
        require(region.degrees == 0 && !region.rotate) {
            "Texture atlas '$atlasPath' region '${region.name}' is rotated; " +
                "world visual regions must be packed with rotation disabled."
        }
        require(
            region.offsetX == 0f && region.offsetY == 0f &&
                region.width == region.originalWidth &&
                region.height == region.originalHeight
        ) {
            "Texture atlas '$atlasPath' region '${region.name}' is trimmed; " +
                "world visual regions must be packed without whitespace stripping."
        }
    }

    private data class RegionKey(
        val atlas: String,
        val page: String,
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int
    )
}

internal fun alphaMasksFromAtlasClasspath(
    sources: List<SpriteSource>
): Map<SpriteSource, List<AlphaMask?>> {
    if (sources.isEmpty()) return emptyMap()

    return AtlasAlphaMaskLoader().use { loader ->
        sources.distinct().associateWith(loader::load)
    }
}
