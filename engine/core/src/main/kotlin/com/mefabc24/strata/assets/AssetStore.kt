package com.mefabc24.strata.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable

/**
 * Loads, caches, and owns game textures.
 *
 * Texture regions returned by this store do not own their textures.
 * Dispose the store only after all consumers have finished rendering.
 */
class AssetStore : Disposable {

    private val textures = mutableMapOf<String, Texture>()
    private var disposed = false

    /**
     * Returns a new region referencing a cached texture.
     */
    fun region(path: String): TextureRegion {
        check(!disposed) {
            "AssetStore has already been disposed."
        }

        require(path.isNotBlank()) {
            "Texture path must not be blank."
        }

        val texture = textures.getOrPut(path) {
            Texture(Gdx.files.classpath(path)).apply {
                setFilter(
                    Texture.TextureFilter.Nearest,
                    Texture.TextureFilter.Nearest
                )
            }
        }

        return TextureRegion(texture)
    }

    /**
     * Releases all textures owned by this store.
     */
    override fun dispose() {
        if (disposed) return

        disposed = true

        textures.values.forEach { it.dispose() }
        textures.clear()
    }
}