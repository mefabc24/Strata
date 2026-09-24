package com.mefabc24.strata

import com.mefabc24.strata.scene.StrataScene

/**
 * Convenience base for games centered around one [StrataScene].
 *
 * Scene creation occurs from [create], after subclass initialization. The base
 * updates the scene before [updateGame], renders it, forwards resize events,
 * and disposes it before [disposeGame]. Use [StrataGame] directly for custom
 * rendering, multiple scenes, or scene switching.
 */
abstract class StrataSceneGame<T : Enum<T>, C : Enum<C>> : StrataGame {
    private var activeScene: StrataScene<T, C>? = null

    /** Persistent default engine settings for this game instance. */
    open override val engineSettings: EngineSettings = EngineSettings()

    /** The scene created during the game lifecycle. */
    val scene: StrataScene<T, C>
        get() = activeScene
            ?: error("The game scene has not been created.")

    /** Creates the scene after the subclass has been fully initialized. */
    protected abstract fun createScene(): StrataScene<T, C>

    /** Called once after [scene] becomes available. */
    protected open fun onReady() = Unit

    /** Called after the scene has updated. */
    protected open fun updateGame(delta: Float) = Unit

    /** Called after the scene receives a resize event. */
    protected open fun resizeGame(width: Int, height: Int) = Unit

    /** Called after the scene has been disposed. */
    protected open fun disposeGame() = Unit

    final override fun create() {
        check(activeScene == null) {
            "The game scene has already been created."
        }

        val created = createScene()
        activeScene = created

        try {
            onReady()
        } catch (failure: Throwable) {
            try {
                created.dispose()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }

            try {
                disposeGame()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }

            activeScene = null
            throw failure
        }
    }

    final override fun update(delta: Float) {
        scene.update(delta)
        updateGame(delta)
    }

    final override fun render() {
        scene.render()
    }

    final override fun resize(width: Int, height: Int) {
        val current = activeScene ?: return

        current.resize(width, height)
        resizeGame(width, height)
    }

    final override fun dispose() {
        val current = activeScene ?: return

        try {
            current.dispose()
        } finally {
            try {
                disposeGame()
            } finally {
                activeScene = null
            }
        }
    }
}
