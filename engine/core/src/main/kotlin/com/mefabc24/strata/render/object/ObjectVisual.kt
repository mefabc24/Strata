package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Defines how a placed object is drawn.
 */
data class ObjectVisual(
    val texture: TextureRegion,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val alphaMask: AlphaMask? = null,
    val width: Float? = null,
    val height: Float? = null,
    val scale: Float = 1f
) {
    init {
        require(scale.isFinite() && scale > 0f) {
            "Sprite scale must be positive and finite."
        }

        require(width == null || (width.isFinite() && width > 0f)) {
            "Sprite width must be positive and finite."
        }

        require(height == null || (height.isFinite() && height > 0f)) {
            "Sprite height must be positive and finite."
        }
    }
}