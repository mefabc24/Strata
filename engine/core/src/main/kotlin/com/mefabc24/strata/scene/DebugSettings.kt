package com.mefabc24.strata.scene

/**
 * Groups the scene's debugging facilities.
 */
class DebugSettings {
    val performance = ScenePerformanceLogger()

    /** Isometric world-grid debug rendering configuration. */
    val grid = DebugGridSettings()

    fun performance(configure: ScenePerformanceLogger.() -> Unit) {
        performance.apply(configure)
    }

    /** Configures the optional isometric world-grid overlay. */
    fun grid(configure: DebugGridSettings.() -> Unit) {
        grid.apply(configure)
    }
}

/**
 * Setup-time configuration for the isometric world-grid overlay.
 *
 * The grid is disabled by default. A scene snapshots this value when setup
 * completes and passes it to a subsequently attached world view.
 */
class DebugGridSettings {
    var enabled: Boolean = false
}
