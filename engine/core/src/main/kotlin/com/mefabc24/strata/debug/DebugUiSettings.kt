package com.mefabc24.strata.debug

import com.badlogic.gdx.Input
import com.mefabc24.strata.render.RenderStats

/** Configures the Debug System's optional user interfaces. */
class DebugUiSettings internal constructor() {
    /** Tool Rail availability, startup visibility, and keyboard shortcut. */
    val toolRail = DebugUiComponentSettings(defaultToggleKey = Input.Keys.F2)

    /** Settings Window availability, startup visibility, and keyboard shortcut. */
    val settingsWindow = DebugUiComponentSettings(defaultToggleKey = Input.Keys.F3)

    /** Configures the Tool Rail without affecting Debug rendering or runtime features. */
    fun toolRail(configure: DebugUiComponentSettings.() -> Unit) = toolRail.apply(configure)

    /** Configures the Settings Window without affecting Debug rendering or runtime features. */
    fun settingsWindow(configure: DebugUiComponentSettings.() -> Unit) =
        settingsWindow.apply(configure)

    internal fun validate() {
        val enabledComponents = listOf(toolRail, settingsWindow)
            .filter(DebugUiComponentSettings::enabled)
        if (enabledComponents.size < 2) return

        require(toolRail.toggleKey != settingsWindow.toggleKey) {
            "Enabled debug interfaces must use different toggle keys; " +
                "both use ${toolRail.toggleKey}."
        }
    }
}

/** Startup configuration and current visibility for one Debug UI component. */
class DebugUiComponentSettings internal constructor(
    defaultToggleKey: Int
) {
    /** Whether this interface can be opened in the current scene. */
    var enabled: Boolean = false

    /** Whether this interface is shown when the Debug runtime is initialized. */
    var visibleOnStartup: Boolean = false

    /** Whether this interface is currently visible. */
    var isVisible: Boolean = false
        internal set

    /** Keyboard shortcut that toggles only this interface. */
    var toggleKey: Int = defaultToggleKey
        set(value) {
            require(value >= 0) {
                "Debug interface toggle key must be non-negative."
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
