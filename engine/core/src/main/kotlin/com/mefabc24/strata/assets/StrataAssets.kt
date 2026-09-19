package com.mefabc24.strata.assets

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.assets.loaders.resolvers.ClasspathFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable

/**
 * Manages the lifecycle of game assets.
 */
class StrataAssets : Disposable {

    private val manager = AssetManager(ClasspathFileHandleResolver())

    private val queuedTextures = mutableSetOf<String>()

    private var disposed = false

    /**
     * Queues a texture for loading if it has not been requested yet.
     */
    fun queueTexture(path: String) {
        checkActive()

        require(path.isNotBlank()) {
            "Texture path must not be blank."
        }

        if (path in queuedTextures) return

        val parameters = TextureLoader.TextureParameter().apply {
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }

        manager.load(path, Texture::class.java, parameters)

        queuedTextures.add(path)
    }

    /**
     * Processes pending loading tasks.
     *
     * Returns true when all assets have finished loading.
     */
    fun update(): Boolean {
        checkActive()
        return manager.update()
    }

    /**
     * Blocks until all queued assets have finished loading.
     */
    fun finishLoading() {
        checkActive()
        manager.finishLoading()
    }

    /**
     * Returns the current loading progress.
     */
    val progress: Float
        get() {
            checkActive()
            return manager.progress
        }

    /**
     * Returns an already loaded texture.
     */
    fun texture(path: String): Texture {
        checkActive()

        check(path in queuedTextures) {
            "Texture has not been queued: $path"
        }

        check(manager.isLoaded(path, Texture::class.java)) {
            "Texture is not loaded yet: $path"
        }

        return manager.get(path, Texture::class.java)
    }

    /**
     * Creates a region referencing an already loaded texture.
     */
    fun region(path: String): TextureRegion {
        return TextureRegion(texture(path))
    }

    private fun checkActive() {
        check(!disposed) {
            "StrataAssets has already been disposed."
        }
    }

    /**
     * Releases all managed assets.
     */
    override fun dispose() {
        if (disposed) return

        disposed = true

        manager.dispose()
        queuedTextures.clear()
    }
}