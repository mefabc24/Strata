package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.graphics.g2d.TextureAtlas

/**
 * Validates a runtime atlas region used by prepared sprite visuals.
 */
internal fun validateAtlasRegion(
    atlas: String,
    region: TextureAtlas.AtlasRegion
) {
    validateAtlasRegionLayout(
        atlas = atlas,
        regionName = region.name,
        degrees = region.degrees,
        rotated = region.rotate,
        offsetX = region.offsetX,
        offsetY = region.offsetY,
        packedWidth = region.packedWidth,
        packedHeight = region.packedHeight,
        originalWidth = region.originalWidth,
        originalHeight = region.originalHeight
    )
}

/**
 * Validates an atlas data region used while reading alpha masks.
 */
internal fun validateAtlasRegion(
    atlas: String,
    region: TextureAtlas.TextureAtlasData.Region
) {
    validateAtlasRegionLayout(
        atlas = atlas,
        regionName = region.name,
        degrees = region.degrees,
        rotated = region.rotate,
        offsetX = region.offsetX,
        offsetY = region.offsetY,
        packedWidth = region.width,
        packedHeight = region.height,
        originalWidth = region.originalWidth,
        originalHeight = region.originalHeight
    )
}

/**
 * World visuals require regions packed without rotation or whitespace
 * stripping so rendering bounds and alpha masks describe the same rectangle.
 */
private fun validateAtlasRegionLayout(
    atlas: String,
    regionName: String,
    degrees: Int,
    rotated: Boolean,
    offsetX: Float,
    offsetY: Float,
    packedWidth: Int,
    packedHeight: Int,
    originalWidth: Int,
    originalHeight: Int
) {
    require(degrees == 0 && !rotated) {
        "Texture atlas '$atlas' region '$regionName' is rotated; " +
                "world visual regions must be packed with rotation disabled."
    }

    require(
        offsetX == 0f &&
                offsetY == 0f &&
                packedWidth == originalWidth &&
                packedHeight == originalHeight
    ) {
        "Texture atlas '$atlas' region '$regionName' is trimmed; " +
                "world visual regions must be packed without whitespace stripping."
    }
}