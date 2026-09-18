package com.mefabc24.strata.terrain

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable

class TerrainRegistry<T : Enum<T>>(
    directory: String
) : Disposable {
    private val baseDirectory = directory.trimEnd('/')

    private val textures = mutableMapOf<String, Texture>()
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

        val texture = textures.getOrPut(path) {
            Texture(Gdx.files.classpath(path)).apply {
                setFilter(
                    Texture.TextureFilter.Nearest,
                    Texture.TextureFilter.Nearest
                )
            }
        }

        regions[type] = TextureRegion(texture)
    }

    /**
     * Returns the sprite registered for a terrain type.
     */
    operator fun get(type: T): TextureRegion {
        return regions[type]
            ?: error("Terrain type $type is not registered.")
    }

    /**
     * Releases all textures owned by this registry.
     */
    override fun dispose() {
        textures.values.forEach { it.dispose() }

        textures.clear()
        regions.clear()
    }
}