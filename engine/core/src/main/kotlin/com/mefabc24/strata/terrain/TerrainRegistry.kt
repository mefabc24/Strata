package com.mefabc24.strata.terrain

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.assets.StrataAssets

/**
 * Maps terrain types to their sprites.
 */
class TerrainRegistry<T : Enum<T>>(
    directory: String,
    private val assets: StrataAssets
) {
    private val baseDirectory = directory.trimEnd('/')

    private val registrations = mutableMapOf<T, String>()
    private val regions = mutableMapOf<T, TextureRegion>()

    /**
     * Registers a terrain type and queues its texture.
     */
    fun register(
        type: T,
        sprite: String = "${type.name.lowercase()}.png"
    ) {
        require(type !in registrations) {
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

        assets.queueTexture(path)
        registrations[type] = path
    }

    /**
     * Resolves registered sprites after their textures have loaded.
     */
    fun prepare() {
        for ((type, path) in registrations) {
            if (type in regions) continue

            regions[type] = assets.region(path)
        }
    }

    /**
     * Returns the maximum terrain sprite height in world units.
     *
     * An empty registry returns infinity to disable height-based culling.
     */
    fun maxSpriteHeight(tileWidth: Float): Float {
        require(tileWidth > 0f && tileWidth.isFinite()) {
            "Tile width must be finite and positive."
        }

        check(registrations.keys.all { it in regions }) {
            "Terrain sprites must be prepared before calculating their height."
        }

        return regions.values.maxOfOrNull { region ->
            require(region.regionWidth > 0) {
                "Terrain sprite width must be positive."
            }

            tileWidth * region.regionHeight / region.regionWidth
        } ?: Float.POSITIVE_INFINITY
    }

    /**
     * Returns the prepared sprite for a terrain type.
     */
    operator fun get(type: T): TextureRegion {
        return regions[type]
            ?: error(
                "Terrain type $type is not prepared. " +
                        "Load the assets and call prepare() first."
            )
    }
}