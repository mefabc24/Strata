package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas

internal data class SpriteSheetCell(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

internal object SpriteSheetGrid {
    fun cells(
        sheetWidth: Int,
        sheetHeight: Int,
        frameWidth: Int,
        frameHeight: Int,
        frameCount: Int? = null
    ): List<SpriteSheetCell> {
        require(frameWidth > 0) {
            "Sprite sheet frame width must be positive."
        }
        require(frameHeight > 0) {
            "Sprite sheet frame height must be positive."
        }
        require(sheetWidth > 0 && sheetHeight > 0) {
            "Sprite sheet dimensions must be positive."
        }
        require(
            sheetWidth % frameWidth == 0 &&
                sheetHeight % frameHeight == 0
        ) {
            "Sprite sheet dimensions must be divisible by frame dimensions."
        }

        val columns = sheetWidth / frameWidth
        val rows = sheetHeight / frameHeight
        val availableFrames = columns * rows
        val resolvedFrameCount = frameCount ?: availableFrames

        require(resolvedFrameCount > 0) {
            "Sprite sheet frame count must be positive."
        }
        require(resolvedFrameCount <= availableFrames) {
            "Sprite sheet frame count $resolvedFrameCount exceeds " +
                "$availableFrames available frames."
        }

        return buildList(resolvedFrameCount) {
            for (index in 0 until resolvedFrameCount) {
                val column = index % columns
                val row = index / columns

                add(
                    SpriteSheetCell(
                        x = column * frameWidth,
                        y = row * frameHeight,
                        width = frameWidth,
                        height = frameHeight
                    )
                )
            }
        }
    }
}

internal sealed interface SpriteSource {
    val assetPaths: List<String>

    val texturePaths: List<String>
        get() = assetPaths

    val atlasPaths: List<String>
        get() = emptyList()

    fun queue(
        queueTexture: (String) -> Unit,
        queueAtlas: (String) -> Unit
    ) {
        texturePaths.forEach(queueTexture)
        atlasPaths.forEach(queueAtlas)
    }

    fun prepare(
        regionFor: (String) -> TextureRegion,
        atlasFor: (String) -> TextureAtlas = {
            error("Atlas resolver is required for atlas sprite sources.")
        }
    ): SpriteFrames

    data class Static(
        val path: String
    ) : SpriteSource {
        init {
            require(path.isNotBlank()) {
                "Sprite path must not be blank."
            }
        }

        override val assetPaths = listOf(path)

        override fun prepare(
            regionFor: (String) -> TextureRegion,
            atlasFor: (String) -> TextureAtlas
        ): SpriteFrames {
            return SpriteFrames.static(regionFor(path))
        }
    }

    class AnimatedFiles(
        paths: List<String>,
        val frameDuration: Float
    ) : SpriteSource {
        override val assetPaths = paths.toList()

        init {
            require(assetPaths.isNotEmpty()) {
                "An animation must contain at least one frame path."
            }
            require(assetPaths.all { it.isNotBlank() }) {
                "Animation frame paths must not be blank."
            }
            SpriteFrames.validateFrameDuration(frameDuration)
        }

        override fun prepare(
            regionFor: (String) -> TextureRegion,
            atlasFor: (String) -> TextureAtlas
        ): SpriteFrames {
            return SpriteFrames.animated(
                frames = assetPaths.map(regionFor),
                frameDuration = frameDuration
            )
        }
    }

    data class SpriteSheet(
        val path: String,
        val frameWidth: Int,
        val frameHeight: Int,
        val frameDuration: Float,
        val frameCount: Int?
    ) : SpriteSource {
        init {
            require(path.isNotBlank()) {
                "Sprite sheet path must not be blank."
            }
            require(frameWidth > 0) {
                "Sprite sheet frame width must be positive."
            }
            require(frameHeight > 0) {
                "Sprite sheet frame height must be positive."
            }
            require(frameCount == null || frameCount > 0) {
                "Sprite sheet frame count must be positive."
            }
            SpriteFrames.validateFrameDuration(frameDuration)
        }

        override val assetPaths = listOf(path)

        override fun prepare(
            regionFor: (String) -> TextureRegion,
            atlasFor: (String) -> TextureAtlas
        ): SpriteFrames {
            val sheet = regionFor(path)
            val cells = SpriteSheetGrid.cells(
                sheetWidth = sheet.regionWidth,
                sheetHeight = sheet.regionHeight,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                frameCount = frameCount
            )
            val frames = cells.map { cell ->
                TextureRegion(
                    sheet,
                    cell.x,
                    cell.y,
                    cell.width,
                    cell.height
                )
            }

            return SpriteFrames.animated(frames, frameDuration)
        }
    }

    /** A single untrimmed, unrotated region from a scene-level atlas path. */
    data class AtlasRegion(
        val atlas: String,
        val region: String
    ) : SpriteSource {
        init {
            validateAtlasNames(atlas, region)
        }

        override val assetPaths = listOf(atlas)
        override val texturePaths = emptyList<String>()
        override val atlasPaths = assetPaths

        override fun prepare(
            regionFor: (String) -> TextureRegion,
            atlasFor: (String) -> TextureAtlas
        ): SpriteFrames {
            val resolved = atlasFor(atlas).findRegion(region)
                ?: error("Texture atlas '$atlas' has no region named '$region'.")
            validateAtlasRegion(atlas, resolved)
            return SpriteFrames.static(resolved)
        }
    }

    /** Indexed atlas regions, returned by libGDX in ascending index order. */
    data class AtlasAnimation(
        val atlas: String,
        val region: String,
        val frameDuration: Float
    ) : SpriteSource {
        init {
            validateAtlasNames(atlas, region)
            SpriteFrames.validateFrameDuration(frameDuration)
        }

        override val assetPaths = listOf(atlas)
        override val texturePaths = emptyList<String>()
        override val atlasPaths = assetPaths

        override fun prepare(
            regionFor: (String) -> TextureRegion,
            atlasFor: (String) -> TextureAtlas
        ): SpriteFrames {
            val resolved = atlasFor(atlas).findRegions(region)
            check(resolved.notEmpty()) {
                "Texture atlas '$atlas' has no indexed regions named '$region'."
            }
            resolved.forEach { validateAtlasRegion(atlas, it) }
            return SpriteFrames.animated(
                frames = List(resolved.size) { resolved[it] },
                frameDuration = frameDuration
            )
        }
    }
}

private fun validateAtlasNames(atlas: String, region: String) {
    require(atlas.isNotBlank()) { "Texture atlas path must not be blank." }
    require(region.isNotBlank()) { "Texture atlas region must not be blank." }
}

/**
 * World visuals currently require regions packed without rotation or
 * whitespace stripping so rendering bounds and alpha masks describe the same
 * rectangle.
 */
internal fun validateAtlasRegion(
    atlas: String,
    region: TextureAtlas.AtlasRegion
) {
    require(region.degrees == 0 && !region.rotate) {
        "Texture atlas '$atlas' region '${region.name}' is rotated; " +
            "world visual regions must be packed with rotation disabled."
    }
    require(
        region.offsetX == 0f && region.offsetY == 0f &&
            region.packedWidth == region.originalWidth &&
            region.packedHeight == region.originalHeight
    ) {
        "Texture atlas '$atlas' region '${region.name}' is trimmed; " +
            "world visual regions must be packed without whitespace stripping."
    }
}
