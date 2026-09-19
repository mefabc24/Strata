package com.mefabc24.strata.scene

import com.badlogic.gdx.utils.Disposable
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.audio.SoundRegistry
import com.mefabc24.strata.audio.StrataAudio
import com.mefabc24.strata.render.ObjectRegistry
import com.mefabc24.strata.terrain.TerrainRegistry

/**
 * Coordinates asset registration, loading, and preparation for a scene.
 *
 * Assets are owned by this scene and are released when the scene is disposed.
 */
class StrataScene<T : Enum<T>, C : Enum<C>>(
    terrainDirectory: String,
    objectDirectory: String,
    configure: StrataScene<T, C>.() -> Unit
) : Disposable {

    val assets = StrataAssets()

    val terrain = TerrainRegistry<T>(
        directory = terrainDirectory,
        assets = assets
    )

    val objects = ObjectRegistry(
        directory = objectDirectory,
        assets = assets
    )

    val sounds = SoundRegistry<C>(assets)

    val audio = StrataAudio(
        assets = assets,
        sounds = sounds
    )

    private var disposed = false

    init {
        try {
            configure(this)

            assets.finishLoading()

            terrain.prepare()
            objects.prepare()
        } catch (failure: Throwable) {
            try {
                dispose()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }

            throw failure
        }
    }

    /**
     * Stops audio and releases all assets owned by this scene.
     */
    override fun dispose() {
        if (disposed) return

        disposed = true

        try {
            audio.dispose()
        } finally {
            assets.dispose()
        }
    }
}