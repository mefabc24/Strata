package com.mefabc24.strata.debug

import com.mefabc24.strata.render.RenderStats

/** Availability, startup visibility, and shortcut for one debug window. */
class DebugWindowSettings internal constructor(
    defaultToggleKey: Int
) {
    /** Whether this window is available in the current scene. */
    var enabled: Boolean = false

    /** Whether this window is displayed when a debug runtime is initialized. */
    var visibleOnStartup: Boolean = false

    /** Current runtime visibility. Hidden windows retain their UI and engine state. */
    var visible: Boolean = false
        internal set

    /** Keyboard shortcut that toggles only this window. */
    var toggleKey: Int = defaultToggleKey
        set(value) {
            require(value >= 0) {
                "Debug window toggle key must be non-negative."
            }
            field = value
        }
}

/** Independent output controls for shared renderer performance metrics. */
class DebugPerformanceSettings internal constructor() {
    private val terminalLogger = DebugPerformanceLogger()
    val history = DebugPerformanceHistory()

    /** Whether the on-screen performance overlay is visible. */
    var overlayEnabled: Boolean = false

    /** Whether performance summaries are periodically written to the terminal log. */
    var terminalLoggingEnabled: Boolean
        get() = terminalLogger.enabled
        set(value) { terminalLogger.enabled = value }

    /** Seconds of rendered time accumulated between terminal performance summaries. */
    var terminalLoggingIntervalSeconds: Float
        get() = terminalLogger.intervalSeconds
        set(value) { terminalLogger.intervalSeconds = value }

    /** Number of samples retained in performance history. */
    var historyLength: Int
        get() = history.capacity
        set(value) { history.capacity = value }

    /** Whether renderer metrics are currently added to performance history. */
    var historyRecording: Boolean
        get() = history.recording
        set(value) { history.recording = value }

    /** Metric shown by the performance history graph. */
    var historyMetric: DebugPerformanceMetric = DebugPerformanceMetric.FRAME_TIME

    fun startHistoryRecording() {
        history.recording = true
    }

    fun stopHistoryRecording() {
        history.recording = false
    }

    fun clearHistory() {
        history.clear()
    }

    internal fun record(stats: RenderStats, delta: Float) {
        terminalLogger.record(stats, delta)
        history.record(stats, delta)
    }
}
