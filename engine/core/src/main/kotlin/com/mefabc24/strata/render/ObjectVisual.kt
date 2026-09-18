package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Defines how a placed object is drawn.
 */
data class ObjectVisual(
    val texture: TextureRegion,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)