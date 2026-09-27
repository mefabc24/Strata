package com.mefabc24.strata

/**
 * Base class for games using a single Strata runtime.
 *
 * The game owns its configured [Strata] facade while this base class
 * coordinates the engine lifecycle around it.
 */
abstract class StrataGame {

    /**
     * Configured Strata runtime owned by this game.
     *
     * Configuration should be completed during game construction.
     */
    protected abstract val strata: Strata

    /**
     * Global engine settings exposed to the backend.
     */
    val engineSettings: EngineSettings
        get() = strata.engineSettings

    private var active = false

    /**
     * Called once after the Strata runtime has been created.
     */
    protected open fun onReady() = Unit

    /**
     * Called after Strata has updated its runtime state.
     */
    protected open fun updateGame(delta: Float) = Unit

    /**
     * Called after Strata has processed a resize.
     */
    protected open fun resizeGame(
        width: Int,
        height: Int
    ) = Unit

    /**
     * Called after the Strata runtime has been disposed.
     */
    protected open fun disposeGame() = Unit

    fun create() {
        check(!active) {
            "The game has already been created."
        }

        strata.create()
        active = true

        try {
            onReady()
        } catch (failure: Throwable) {
            try {
                strata.dispose()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }

            try {
                disposeGame()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }

            active = false
            throw failure
        }
    }

    fun update(delta: Float) {
        strata.update(delta)
        updateGame(delta)
    }

    fun render() {
        strata.render()
    }

    fun resize(
        width: Int,
        height: Int
    ) {
        if (!active) return

        strata.resize(
            width,
            height
        )

        resizeGame(
            width,
            height
        )
    }

    fun dispose() {
        if (!active) return

        try {
            strata.dispose()
        } finally {
            try {
                disposeGame()
            } finally {
                active = false
            }
        }
    }
}