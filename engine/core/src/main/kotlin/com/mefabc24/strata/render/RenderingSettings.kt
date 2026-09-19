package com.mefabc24.strata.render

/**
 * Configures the rendering of an isometric world.
 *
 * Settings are applied when the world view is created.
 */
data class RenderingSettings(
    var tileWidth: Float = 64f,
    var tileHeight: Float = 32f,

    /**
     * Maximum terrain sprite height in world units.
     *
     * Null enables automatic calculation when the view
     * is created through StrataScene.
     */
    var maxTerrainSpriteHeight: Float? = null
) {

    internal fun validate() {
        require(tileWidth.isFinite() && tileWidth > 0f) {
            "Tile width must be finite and positive."
        }

        require(tileHeight.isFinite() && tileHeight > 0f) {
            "Tile height must be finite and positive."
        }

        require(
            maxTerrainSpriteHeight == null ||
                    (
                            maxTerrainSpriteHeight!! > 0f &&
                                    !maxTerrainSpriteHeight!!.isNaN()
                            )
        ) {
            "Maximum terrain sprite height must be positive."
        }
    }
}