package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.debug.DebugPerformanceHistory
import com.mefabc24.strata.debug.DebugPerformanceHistoryGraph
import com.mefabc24.strata.debug.DebugPerformanceMetric
import com.mefabc24.strata.debug.DebugPerformanceSettings
import com.mefabc24.strata.ui.StrataButton
import com.mefabc24.strata.ui.StrataColumn
import com.mefabc24.strata.ui.StrataDropdown
import com.mefabc24.strata.ui.StrataNumericStepper
import com.mefabc24.strata.ui.StrataPanel
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.cell
import com.mefabc24.strata.ui.fillAvailable
import com.mefabc24.strata.ui.fillAvailableX
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A separately positioned, self-contained panel: recording controls, graph selection, and graphs
 * live here, while the Debug Window only toggles its visibility. It never contributes to the live
 * stats stack's height.
 */
internal class DebugPerformanceHistoryOverlay(
    private val ui: StrataUi,
    private val settings: DebugPerformanceSettings
) {
    private val state = DebugPerformanceHistoryOverlayState(settings)
    private val pixel = TextureRegion(ui.skin.get("debug-white", Texture::class.java))
    private val lineColor = ui.skin.get("title", Label.LabelStyle::class.java).fontColor
    private val axisStyle = ui.skin.get("debug-secondary", Label.LabelStyle::class.java)
    private val cardBackground = ui.skin.get("debug-tool-preview", StrataPanelStyle::class.java).background
    private val recordingDrawable: Drawable = ui.skin.newDrawable("debug-white", RECORDING_COLOR)
    private val stoppedDrawable: Drawable = ui.skin.newDrawable("debug-white", STOPPED_COLOR)
    private val cards = mutableMapOf<DebugPerformanceHistoryGraph, GraphCard>()

    private val recordingIndicator = Image(stoppedDrawable)
    private val recordingLabel = Label("Stopped", ui.skin).apply { color = STOPPED_TEXT_COLOR }
    private val samplesLabel = Label("", ui.skin, "debug-secondary").apply { setAlignment(Align.right) }
    private val startButton = actionButton("Start") { settings.startHistoryRecording() }
    private val stopButton = actionButton("Stop") { settings.stopHistoryRecording() }
    private val clearButton = actionButton("Clear") { settings.clearHistory() }
    private val addButton = actionButton("Add graph") {
        if (settings.historyGraphs.canAdd) settings.historyGraphs.add()
    }
    private val lengthStepper = StrataNumericStepper(
        label = "Length",
        skin = ui.skin,
        value = settings.historyLength.toFloat(),
        minimum = 1f,
        maximum = 16_384f,
        step = 10f,
        decimals = 0,
        onChanged = { settings.historyLength = it.toInt() },
        valueStyleName = "debug-stepper-value"
    )
    private lateinit var graphList: StrataColumn

    private val panel = StrataPanel(
        context = ui.root.context,
        styleName = ui.theme.panelStyle,
        spacing = SPACING,
        paddingOverride = null,
        blocksInput = true
    ).apply {
        name = "debug-performance-history"
        isVisible = false
        // The whole surface is hit, so clicks between graphs never reach world tools underneath.
        touchable = Touchable.enabled
        // Clip the complete panel on narrow viewports; the graph list scrolls when it is too tall.
        setClip(true)
        row(spacing = SPACING) {
            label("Performance History", "title").cell { growX(); left() }
            actor(recordingIndicator).cell { size(INDICATOR_SIZE) }
            actor(recordingLabel)
        }.cell { fillAvailableX(); height(HEADER_HEIGHT) }
        row(spacing = 4f) {
            actor(startButton).cell { width(56f); height(CONTROL_HEIGHT) }
            actor(stopButton).cell { width(56f); height(CONTROL_HEIGHT) }
            actor(clearButton).cell { width(56f); height(CONTROL_HEIGHT) }
            actor(samplesLabel).cell { growX(); right() }
        }.cell { fillAvailableX(); height(CONTROL_HEIGHT) }
        scrollColumn(spacing = SPACING) {
            graphList = this
        }.cell { fillAvailable(); minHeight(0f) }
        row(spacing = SPACING) {
            actor(addButton).cell { width(92f); height(CONTROL_HEIGHT) }
            actor(lengthStepper).cell { expandX(); right() }
        }.cell { fillAvailableX(); height(28f) }
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
        val width = minOf(MAXIMUM_WIDTH, maxOf(0f, floor(rightEdge - DebugWindowLayout.OVERLAY_GAP) - x))
        val available = maxOf(0f, floor(viewportHeight - DebugWindowLayout.OVERLAY_MARGIN) - y)
        val height = minOf(ceil(panel.prefHeight), available)
        if (panel.x != x || panel.y != y || panel.width != width || panel.height != height) {
            panel.setBounds(x, y, width, height)
            panel.invalidate()
        }
        panel.validate()
    }

    /** Updates graphs and controls; call before [position] so added or removed graphs are measured. */
    fun update(delta: Float) {
        val plotWidth = cards.values.firstOrNull()?.lineGraph?.plotWidth?.toInt() ?: 0
        val refreshed = state.update(delta, plotWidth)
        panel.isVisible = state.visible
        if (!state.visible) return
        if (state.graphs.size != cards.size || state.graphs.any { it.graph !in cards }) rebuildCards()
        syncControls()
        if (!refreshed) return
        state.graphs.forEach { cards.getValue(it.graph).show(it) }
        recordingIndicator.drawable = if (state.recording) recordingDrawable else stoppedDrawable
        recordingLabel.setText(if (state.recording) "Recording" else "Stopped")
        recordingLabel.color = if (state.recording) RECORDING_COLOR else STOPPED_TEXT_COLOR
        samplesLabel.setText("Samples ${settings.history.size} / ${settings.history.capacity}")
    }

    private fun rebuildCards() {
        cards.keys.retainAll(state.graphs.map { it.graph }.toSet())
        graphList.clearChildren()
        state.graphs.forEach { graphState ->
            val card = cards.getOrPut(graphState.graph) { GraphCard(graphState.graph) }
            graphList.actor(card.root).cell { fillAvailableX() }
        }
        panel.invalidateHierarchy()
    }

    /** Cheap flag updates every frame, so controls never offer an action that does nothing. */
    private fun syncControls() {
        val recording = settings.historyRecording
        startButton.isDisabled = recording
        stopButton.isDisabled = !recording
        clearButton.isDisabled = settings.history.size == 0
        addButton.isDisabled = !settings.historyGraphs.canAdd
        val removable = settings.historyGraphs.canRemove
        cards.values.forEach { it.removeButton.isDisabled = !removable }
        lengthStepper.sync(settings.historyLength.toFloat())
    }

    private fun actionButton(text: String, onClick: () -> Unit) =
        StrataButton(text, ui.skin, "debug-action-card", onClick)

    /** One graph section; its metric selector sits in the upper-right corner of the section header. */
    private inner class GraphCard(graph: DebugPerformanceHistoryGraph) {
        private val title = Label(graph.metric.label, ui.skin, "debug-tool-label").apply { setEllipsis(true) }
        val metricSelector = StrataDropdown(
            options = DebugPerformanceMetric.entries,
            selected = graph.metric,
            skin = ui.skin,
            styleName = ui.theme.dropdownStyle,
            displayText = DebugPerformanceMetric::displayName,
            // The overlay state notices the new metric and redraws this graph on its next update.
            onChanged = { graph.metric = it }
        )
        val removeButton = actionButton("Remove") {
            if (settings.historyGraphs.canRemove) settings.historyGraphs.remove(graph)
        }
        val lineGraph = DebugLineGraph(pixel, lineColor, axisStyle)
        private val current = Label("--", ui.skin)
        private val average = Label("--", ui.skin)
        private val minimum = Label("--", ui.skin)
        private val maximum = Label("--", ui.skin)

        val root = Table(ui.skin).apply {
            background = cardBackground
            pad(6f)
            add(Table(ui.skin).apply {
                add(title).growX().left().minWidth(0f)
                add(metricSelector).width(150f).height(CONTROL_HEIGHT).padLeft(6f)
                add(removeButton).width(64f).height(CONTROL_HEIGHT).padLeft(4f)
            }).growX()
            row()
            add(lineGraph).growX().height(GRAPH_HEIGHT).minWidth(0f).padTop(4f)
            row()
            add(Table(ui.skin).apply {
                touchable = Touchable.disabled
                statistic("Current", current)
                statistic("Avg", average)
                statistic("Min", minimum)
                statistic("Max", maximum)
            }).growX().padTop(4f)
        }

        fun show(state: DebugPerformanceGraphState) {
            val metric = state.graph.metric
            metricSelector.sync(metric)
            title.setText(metric.label)
            lineGraph.show(state.data, metric)
            current.setText(state.data.current?.let(metric::format) ?: "--")
            val summary = state.data.summary
            average.setText(summary?.let { metric.format(it.average) } ?: "--")
            minimum.setText(summary?.let { metric.format(it.minimum) } ?: "--")
            maximum.setText(summary?.let { metric.format(it.maximum) } ?: "--")
        }

        private fun Table.statistic(key: String, value: Label) {
            add(Label(key, ui.skin, "debug-secondary")).padRight(4f)
            // Uniform value columns keep statistics aligned between stacked graphs.
            add(value).expandX().uniformX().left()
        }
    }

    private companion object {
        const val MAXIMUM_WIDTH = 480f
        const val SPACING = 6f
        const val HEADER_HEIGHT = 22f
        const val CONTROL_HEIGHT = 24f
        const val INDICATOR_SIZE = 8f
        const val GRAPH_HEIGHT = 120f
        val RECORDING_COLOR = Color(0.36f, 0.82f, 0.48f, 1f)
        val STOPPED_COLOR = Color(0.45f, 0.46f, 0.50f, 1f)
        val STOPPED_TEXT_COLOR = Color(0.58f, 0.61f, 0.66f, 1f)
    }
}

/** Refresh policy and per-graph data, independent of both capture and live stats visibility. */
internal class DebugPerformanceHistoryOverlayState(private val settings: DebugPerformanceSettings) {
    var visible = false
        private set
    var recording = false
        private set

    /** Views of the configured graphs in panel order; each survives other graphs' addition or removal. */
    var graphs: List<DebugPerformanceGraphState> = emptyList()
        private set
    private var elapsed = 0f
    private var displayedGraphsRevision = -1L
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
        val graphsChanged = syncGraphs()
        val forced = !visible || graphsChanged || graphs.any { it.metricChanged } ||
            recording != settings.historyRecording ||
            displayedRevision != settings.history.structureRevision || displayedWidth != plotWidth
        visible = true
        if (!forced && elapsed < settings.overlayRefreshIntervalSeconds) return false
        graphs.forEach { it.refresh(settings.history, plotWidth) }
        displayedRevision = settings.history.structureRevision
        displayedWidth = plotWidth
        recording = settings.historyRecording
        elapsed = 0f
        return true
    }

    private fun syncGraphs(): Boolean {
        val configured = settings.historyGraphs
        if (displayedGraphsRevision == configured.revision) return false
        val existing = graphs.associateBy { it.graph }
        graphs = configured.graphs.map { existing[it] ?: DebugPerformanceGraphState(it) }
        displayedGraphsRevision = configured.revision
        return true
    }
}

/** Plotted data of one graph, always built from that graph's currently selected metric. */
internal class DebugPerformanceGraphState(val graph: DebugPerformanceHistoryGraph) {
    var data = DebugPerformanceGraphData()
        private set

    /** Metric [data] was built from; `null` before the first refresh. */
    var metric: DebugPerformanceMetric? = null
        private set

    val metricChanged: Boolean
        get() = metric != graph.metric

    fun refresh(history: DebugPerformanceHistory, plotWidth: Int) {
        val selected = graph.metric
        // Metrics have different magnitudes; never inherit another metric's axis hysteresis.
        if (selected != metric) data = DebugPerformanceGraphData()
        data.update(history.size, plotWidth, selected.unit.minimumGraphRange) { history.sampleAt(selected, it) }
        metric = selected
    }
}
