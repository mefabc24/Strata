package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.Color

/**
 * Defines the appearance of placement previews.
 */
data class PlacementPreviewStyle(
    val validColor: Color = Color(0.5f, 1f, 0.5f, 0.65f),
    val invalidColor: Color = Color(1f, 0.4f, 0.4f, 0.65f)
) {
    companion object {
        val DEFAULT = PlacementPreviewStyle()
    }
}