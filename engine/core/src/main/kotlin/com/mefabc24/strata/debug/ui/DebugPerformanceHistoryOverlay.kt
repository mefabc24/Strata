package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.mefabc24.strata.debug.DebugPerformanceMetric
import com.mefabc24.strata.debug.DebugPerformanceSettings
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi
import kotlin.math.ceil
import kotlin.math.floor

/** A separately positioned panel; never contributes to the live stats stack's height. */
internal class DebugPerformanceHistoryOverlay(ui: StrataUi, private val settings: DebugPerformanceSettings) {
    private val state = DebugPerformanceHistoryOverlayState(settings)
    private val metric = Label("", ui.skin, "debug-secondary")
    private val summary = Label("", ui.skin)
    private val current = Label("", ui.skin, "debug-secondary")
    private val samples = Label("", ui.skin, "debug-secondary")
    private val recording = Label("Stopped", ui.skin, "debug-secondary")
    private val graph = DebugLineGraph(
        TextureRegion(ui.skin.get("debug-white", Texture::class.java)),
        ui.skin.get("title", Label.LabelStyle::class.java).fontColor,
        ui.skin.get("debug-secondary", Label.LabelStyle::class.java)
    )
    private val panel = Table(ui.skin).apply {
        name = "debug-performance-history"
        background = ui.skin.get(requireNotNull(ui.theme.panelStyle), StrataPanelStyle::class.java).background
        pad(10f)
        touchable = Touchable.disabled
        isVisible = false
        // Clip the complete panel as well as the actor's plot on narrow viewports.
        setClip(true)
        add(Label("Performance History", ui.skin, "title")).growX().left().colspan(2).height(22f)
        row()
        add(metric).growX().left().colspan(2).height(22f).padBottom(4f)
        row()
        add(graph).grow().colspan(2).minSize(0f)
        row()
        add(summary).growX().left().colspan(2).height(22f).padTop(6f)
        row()
        add(current).growX().left().colspan(2).height(20f)
        row()
        add(samples).growX().left().height(20f)
        add(recording).right().height(20f)
    }

    init {
        ui.stage.addActor(panel)
    }

    /** Occupied insets come from the actual Debug Window, rail, and live overlay layout. */
    fun position(viewportWidth: Float, viewportHeight: Float, left: Float, rightEdge: Float, bottom: Float) {
        // Clipping makes the panel a transform group: its origin offsets every glyph after the font cache
        // has snapped them locally, so bounds must stay on whole pixels for the bitmap font to stay crisp.
        val x = ceil(left).coerceIn(0f, floor(viewportWidth))
        val y = ceil(bottom).coerceIn(0f, floor(viewportHeight))
        val width = minOf(480f, maxOf(0f, floor(rightEdge - DebugWindowLayout.OVERLAY_GAP) - x))
        val height = minOf(320f, maxOf(0f, floor(viewportHeight - DebugWindowLayout.OVERLAY_MARGIN) - y))
        if (panel.x != x || panel.y != y || panel.width != width || panel.height != height) {
            panel.setBounds(x, y, width, height)
            panel.invalidate()
        }
        panel.validate()
    }

    fun update(delta: Float) {
        val refreshed = state.update(delta, graph.plotWidth.toInt())
        panel.isVisible = state.visible
        if (!refreshed) return
        val shown = settings.historyMetric
        metric.setText(shown.label)
        graph.show(state.graph, shown)
        summary.setText(state.graph.summary?.let {
            "Avg ${shown.format(it.average)}    Min ${shown.format(it.minimum)}    Max ${shown.format(it.maximum)}"
        } ?: "Avg --    Min --    Max --")
        current.setText("Current ${state.graph.current?.let(shown::format) ?: "--"}")
        samples.setText("Samples ${state.graph.sampleCount}")
        recording.setText(if (state.recording) "Recording" else "Stopped")
    }
}

/** Refresh policy and graph data are independent of both capture and live stats visibility. */
internal class DebugPerformanceHistoryOverlayState(private val settings: DebugPerformanceSettings) {
    var visible = false
        private set
    var recording = false
        private set
    var graph = DebugPerformanceGraphData()
        private set
    private var elapsed = 0f
    private var displayedMetric: DebugPerformanceMetric? = null
    private var displayedRevision = -1L
    private var displayedWidth = -1

    fun update(delta: Float, plotWidth: Int): Boolean {
        require(delta.isFinite() && delta >= 0f)
        if (!settings.historyOverlayEnabled) {
            visible = false
            elapsed = 0f
            return false
        }
        elapsed += delta
        val metricChanged = displayedMetric != settings.historyMetric
        val forced = !visible || metricChanged || recording != settings.historyRecording ||
            displayedRevision != settings.history.structureRevision || displayedWidth != plotWidth
        visible = true
        if (!forced && elapsed < settings.overlayRefreshIntervalSeconds) return false
        // Metrics have different magnitudes; do not inherit another metric's axis hysteresis.
        if (metricChanged) graph = DebugPerformanceGraphData()
        val metric = settings.historyMetric
        graph.update(settings.history.size, plotWidth, metric.unit.minimumGraphRange) {
            settings.history.sampleAt(metric, it)
        }
        displayedMetric = settings.historyMetric
        displayedRevision = settings.history.structureRevision
        displayedWidth = plotWidth
        recording = settings.historyRecording
        elapsed = 0f
        return true
    }
}
