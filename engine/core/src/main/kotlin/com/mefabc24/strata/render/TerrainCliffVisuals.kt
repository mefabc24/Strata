package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Prepared one-step cliff faces for the two visible isometric sides.
 *
 * Each texture uses a full terrain-tile-width canvas and represents exactly
 * one logical elevation step. Transparent canvas padding is allowed.
 */
data class TerrainCliffVisuals(
    val left: TextureRegion? = null,
    val right: TextureRegion? = null
)
