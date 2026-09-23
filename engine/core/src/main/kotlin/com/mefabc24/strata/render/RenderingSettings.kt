package com.mefabc24.strata.render

import com.mefabc24.strata.iso.TileGeometry

/**
 * Configures the rendering of an isometric world.
 *
 * Settings are applied when the world view is created.
 */
class RenderingSettings(
    val tileGeometry: TileGeometry = TileGeometry(),

    /**
     * Maximum terrain sprite height in world units.
     *
     * Null enables automatic calculation when the view
     * is created through StrataScene.
     */
    var maxTerrainSpriteHeight: Float? = null
) {

    fun tileGeometry(configure: TileGeometry.() -> Unit) {
        tileGeometry.apply(configure)
    }

    fun copy(): RenderingSettings {
        return RenderingSettings(
            tileGeometry = tileGeometry.copy(),
            maxTerrainSpriteHeight = maxTerrainSpriteHeight
        )
    }

    internal fun validate() {
        tileGeometry.validate()

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