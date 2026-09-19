package com.mefabc24.strata.scene

/**
 * Groups the scene's debugging facilities.
 */
class DebugSettings {
    val performance = ScenePerformanceLogger()

    fun performance(configure: ScenePerformanceLogger.() -> Unit) {
        performance.apply(configure)
    }
}