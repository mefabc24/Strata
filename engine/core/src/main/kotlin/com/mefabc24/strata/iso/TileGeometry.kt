package com.mefabc24.strata.iso

/**
 * Describes the geometry of an isometric terrain tile.
 *
 * Strata uses a 2:1 isometric top face, so the face height is
 * derived from half of the configured tile width.
 */
data class TileGeometry(
    var width: Float = 64f,
    var height: Float = 64f
) {

    /**
     * Height of the diamond-shaped top face.
     */
    val faceHeight: Float
        get() = width / 2f

    /**
     * Vertical distance between two terrain elevation levels.
     */
    val elevationStep: Float
        get() = height - faceHeight

    internal fun validate() {
        require(width.isFinite() && width > 0f) {
            "Tile width must be finite and positive."
        }

        require(height.isFinite() && height > 0f) {
            "Tile height must be finite and positive."
        }

        require(height >= faceHeight) {
            "Tile height must not be smaller than the top face height."
        }
    }
}