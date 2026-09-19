package com.mefabc24.strata.terrain

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.assets.AssetStore

/**
 * Maps terrain types to sprites managed by an AssetStore.
 */
class TerrainRegistry<T : Enum<T>>(
    directory: String,
    private val assets: AssetStore
) {
    private val baseDirectory = directory.trimEnd('/')

    private val regions = mutableMapOf<T, TextureRegion>()

    /**
     * Registers a terrain type using its name as the default sprite filename.
     */
    fun register(
        type: T,
        sprite: String = "${type.name.lowercase()}.png"
    ) {
        require(type !in regions) {
            "Terrain type $type is already registered."
        }

        require(sprite.isNotBlank()) {
            "Sprite path must not be blank."
        }

        val path = if (baseDirectory.isEmpty()) {
            sprite
        } else {
            "$baseDirectory/$sprite"
        }

        regions[type] = assets.region(path)
    }

    /**
     * Returns the sprite registered for a terrain type.
     */
    operator fun get(type: T): TextureRegion {
        return regions[type]
            ?: error("Terrain type $type is not registered.")
    }
}