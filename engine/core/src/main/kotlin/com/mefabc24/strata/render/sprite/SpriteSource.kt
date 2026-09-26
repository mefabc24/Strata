package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.graphics.g2d.TextureRegion

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

    fun prepare(
        regionFor: (String) -> TextureRegion
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
            regionFor: (String) -> TextureRegion
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
            regionFor: (String) -> TextureRegion
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
            regionFor: (String) -> TextureRegion
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
}
