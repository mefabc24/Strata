package com.mefabc24.strata.assets

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.assets.loaders.resolvers.ClasspathFileHandleResolver
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable

/**
 * Loads and owns game assets.
 */
class StrataAssets : Disposable {

    private val manager = AssetManager(ClasspathFileHandleResolver())

    private val queuedAssets = mutableMapOf<String, Class<*>>()

    private var disposed = false

    /**
     * Queues a texture with pixel-art filtering.
     */
    fun queueTexture(path: String) {
        checkActive()

        if (!register(path, Texture::class.java)) return

        val parameters = TextureLoader.TextureParameter().apply {
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }

        manager.load(path, Texture::class.java, parameters)
    }

    /** Queues a libGDX texture atlas. Atlas paths are scene asset paths. */
    fun queueAtlas(path: String) {
        checkActive()

        if (register(path, TextureAtlas::class.java)) {
            manager.load(path, TextureAtlas::class.java)
        }
    }

    /**
     * Queues a short sound effect.
     */
    fun queueSound(path: String) {
        checkActive()

        if (register(path, Sound::class.java)) {
            manager.load(path, Sound::class.java)
        }
    }

    /**
     * Queues a streamed music track.
     */
    fun queueMusic(path: String) {
        checkActive()

        if (register(path, Music::class.java)) {
            manager.load(path, Music::class.java)
        }
    }

    /**
     * Returns true when all queued assets are loaded.
     */
    fun update(): Boolean {
        checkActive()
        return manager.update()
    }

    /**
     * Blocks until all queued assets are loaded.
     */
    fun finishLoading() {
        checkActive()
        manager.finishLoading()
    }

    val progress: Float
        get() {
            checkActive()
            return manager.progress
        }

    fun texture(path: String): Texture {
        return get(path, Texture::class.java)
    }

    fun region(path: String): TextureRegion {
        return TextureRegion(texture(path))
    }

    fun atlas(path: String): TextureAtlas {
        return get(path, TextureAtlas::class.java)
    }

    fun sound(path: String): Sound {
        return get(path, Sound::class.java)
    }

    fun music(path: String): Music {
        return get(path, Music::class.java)
    }

    /**
     * Registers an asset path exactly once.
     */
    private fun register(
        path: String,
        type: Class<*>
    ): Boolean {
        require(path.isNotBlank()) {
            "Asset path must not be blank."
        }

        val existingType = queuedAssets[path]

        if (existingType != null) {
            require(existingType == type) {
                "Asset $path is already registered as ${existingType.simpleName}."
            }

            return false
        }

        queuedAssets[path] = type
        return true
    }

    private fun <T> get(
        path: String,
        type: Class<T>
    ): T {
        checkActive()

        check(queuedAssets[path] == type) {
            "Asset is not queued as ${type.simpleName}: $path"
        }

        check(manager.isLoaded(path, type)) {
            "Asset is not loaded yet: $path"
        }

        return manager.get(path, type)
    }

    private fun checkActive() {
        check(!disposed) {
            "StrataAssets has already been disposed."
        }
    }

    /**
     * Releases all assets owned by the manager.
     */
    override fun dispose() {
        if (disposed) return

        disposed = true

        manager.dispose()
        queuedAssets.clear()
    }
}
