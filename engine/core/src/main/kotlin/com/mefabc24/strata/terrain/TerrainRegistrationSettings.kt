package com.mefabc24.strata.terrain

/**
 * Configures optional one-step cliff sprites for a terrain registration.
 *
 * Each sprite is loaded and owned by the scene asset system. It should use a
 * full tile-width canvas and visually represent one logical elevation step.
 */
class TerrainCliffSettings {
    /** Sprite for the visible face toward increasing world Y. */
    var left: String? = null

    /** Sprite for the visible face toward increasing world X. */
    var right: String? = null

    internal fun validate() {
        require(left == null || left!!.isNotBlank()) {
            "Left cliff sprite path must not be blank."
        }
        require(right == null || right!!.isNotBlank()) {
            "Right cliff sprite path must not be blank."
        }
    }
}

/** Setup-only visual metadata for a registered terrain type. */
class TerrainRegistrationSettings {
    /** Optional cliff-face asset paths for exposed elevation levels. */
    val cliffs = TerrainCliffSettings()

    /** Configures optional left and right one-step cliff sprites. */
    fun cliffs(configure: TerrainCliffSettings.() -> Unit) {
        cliffs.apply(configure)
    }

    internal fun validate() {
        cliffs.validate()
    }
}
