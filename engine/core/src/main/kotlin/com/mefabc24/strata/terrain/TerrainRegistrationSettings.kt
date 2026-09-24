package com.mefabc24.strata.terrain

/** Setup-only visual metadata for a registered terrain type. */
class TerrainRegistrationSettings {

    /**
     * Optional sprite repeated for each additional exposed elevation step.
     *
     * A fill sprite may use the same canvas dimensions and alignment as the
     * surface sprite, but should omit its top face. It represents exactly one
     * logical elevation step; transparent canvas padding is allowed.
     */
    var fillSprite: String? = null

    internal fun validate() {
        require(fillSprite == null || fillSprite!!.isNotBlank()) {
            "Terrain fill sprite path must not be blank."
        }
    }
}
