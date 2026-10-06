package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.ui.StrataButton
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataSelectableButton
import com.mefabc24.strata.ui.StrataSelectionGroup
import com.mefabc24.strata.ui.StrataSeparator
import com.mefabc24.strata.ui.StrataSeparatorOrientation
import com.mefabc24.strata.ui.StrataSeparatorStyle
import com.mefabc24.strata.ui.StrataToggleButton
import com.mefabc24.strata.ui.StrataUi
import java.util.Locale

internal class DebugSimulationOverlay(
    ui: StrataUi,
    private val simulation: SimulationController
) {
    private val state = DebugSimulationOverlayState(simulation)
    private var refreshElapsed = 0f
    private var accumulatedRealDelta = 0f
    private var accumulatedSimulationDelta = 0f
    private var sampledFrames = 0
    private val pauseButton = StrataToggleButton(
        text = state.pauseButtonText,
        skin = ui.skin,
        styleName = ui.theme.selectableButtonStyle,
        checked = state.paused
    ) {
        state.togglePause()
        sync()
    }
    private val stepButton = StrataButton(
        "Step",
        ui.skin,
        "debug-action-card"
    ) {
        state.step()
    }
    private val resetSpeedButton = StrataButton(
        "Reset",
        ui.skin,
        "debug-action-card"
    ) {
        state.resetTimeScale()
        sync()
    }
    private val speedSelection = StrataSelectionGroup(
        options = DebugSimulationOverlayState.TIME_SCALES,
        initialSelection = state.selectedTimeScale,
        selectionRequired = false
    ).apply {
        onSelectionChanged { scale ->
            if (scale != null) {
                state.selectTimeScale(scale)
            } else {
                state.selectedTimeScale?.let(::select)
            }
        }
    }
    private val speedButtons = DebugSimulationOverlayState.TIME_SCALES.associateWith { scale ->
        StrataSelectableButton(
            speedLabel(scale),
            scale,
            speedSelection,
            ui.skin,
            ui.theme.selectableButtonStyle
        )
    }
    private val realDeltaLabel = Label("", ui.skin)
    private val simulationDeltaLabel = Label("", ui.skin)
    private val scaleLabel = Label("", ui.skin)
    private val root = Table().apply {
        setFillParent(true)
        bottom().pad(16f)
        touchable = Touchable.childrenOnly
        isVisible = false
        add(simulationRail(ui))
    }

    init {
        ui.stage.addActor(root)
        updateTimingLabels(state.timing(0f, 0f))
        sync()
    }

    fun setVisible(visible: Boolean) {
        state.setVisible(visible)
        sync()
    }

    /** Keeps separately positioned charts above the bottom Simulation rail. */
    fun topEdge(): Float {
        if (!state.visible) return DebugWindowLayout.OVERLAY_MARGIN
        root.validate()
        val rail = root.children.first()
        return rail.y + rail.height + DebugWindowLayout.OVERLAY_GAP
    }

    fun sync() {
        root.isVisible = state.visible

        pauseButton.setText(state.pauseButtonText)
        pauseButton.syncChecked(state.paused)
        stepButton.isDisabled = !state.stepAvailable

        val selectedScale = state.selectedTimeScale
        if (selectedScale == null) speedSelection.clearSelection()
        else speedSelection.select(selectedScale)
    }

    /** Updates the timing display using averaged values at a fixed interval. */
    fun update(delta: Float) {
        sync()

        if (!state.visible) {
            refreshElapsed = 0f
            accumulatedRealDelta = 0f
            accumulatedSimulationDelta = 0f
            sampledFrames = 0
            return
        }

        refreshElapsed += delta
        accumulatedRealDelta += simulation.lastRealDelta
        accumulatedSimulationDelta += simulation.lastSimulationDelta
        sampledFrames++

        if (refreshElapsed < REFRESH_INTERVAL_SECONDS) return

        updateTimingLabels(
            state.timing(
                realDelta = accumulatedRealDelta / sampledFrames,
                simulationDelta = accumulatedSimulationDelta / sampledFrames
            )
        )

        refreshElapsed = 0f
        accumulatedRealDelta = 0f
        accumulatedSimulationDelta = 0f
        sampledFrames = 0
    }

    private fun simulationRail(ui: StrataUi): Table {
        val panelStyle = ui.skin.get(
            requireNotNull(ui.theme.panelStyle),
            StrataPanelStyle::class.java
        )
        val statsBackground = ui.skin.get(
            "debug-setting-row",
            StrataPanelStyle::class.java
        ).background
        val separatorStyle = ui.skin.get(
            requireNotNull(ui.theme.separatorStyle),
            StrataSeparatorStyle::class.java
        )

        return Table(ui.skin).apply {
            background = panelStyle.background
            pad(4f, 6f, 4f, 6f)
            touchable = Touchable.childrenOnly

            add(Table(ui.skin).apply {
                touchable = Touchable.childrenOnly

                add(Label("Simulation", ui.skin, "title").apply {
                    touchable = Touchable.disabled
                }).padLeft(2f).padRight(2f)
                verticalSeparator(separatorStyle)

                add(pauseButton).width(78f).height(CONTROL_HEIGHT).padRight(2f)
                add(stepButton).width(54f).height(CONTROL_HEIGHT)
                verticalSeparator(separatorStyle)

                DebugSimulationOverlayState.TIME_SCALES.forEachIndexed { index, scale ->
                    add(speedButtons.getValue(scale))
                        .width(46f)
                        .height(CONTROL_HEIGHT)
                        .padRight(if (index == speedButtons.size - 1) 0f else 2f)
                }
                verticalSeparator(separatorStyle)

                add(resetSpeedButton).width(56f).height(CONTROL_HEIGHT)
            }).left()
            row()

            add(StrataSeparator(StrataSeparatorOrientation.HORIZONTAL, separatorStyle))
                .growX()
                .height(separatorStyle.thickness)
                .padTop(3f)
            row()

            add(Table(ui.skin).apply {
                background = statsBackground
                pad(2f, 6f, 2f, 6f)
                touchable = Touchable.disabled

                timingValue("Real", realDeltaLabel, ui)
                statsSeparator(separatorStyle)
                timingValue("Simulation", simulationDeltaLabel, ui)
                statsSeparator(separatorStyle)
                timingValue("Scale", scaleLabel, ui)
            }).growX().fillX().height(STATS_HEIGHT)
        }
    }

    private fun Table.verticalSeparator(style: StrataSeparatorStyle) {
        add(StrataSeparator(StrataSeparatorOrientation.VERTICAL, style))
            .width(style.thickness)
            .height(24f)
            .padLeft(6f)
            .padRight(6f)
    }

    private fun Table.statsSeparator(style: StrataSeparatorStyle) {
        add(StrataSeparator(StrataSeparatorOrientation.VERTICAL, style))
            .width(style.thickness)
            .height(14f)
            .padLeft(10f)
            .padRight(10f)
    }

    private fun Table.timingValue(
        key: String,
        value: Label,
        ui: StrataUi
    ) {
        add(Label(key, ui.skin, "debug-secondary").apply {
            touchable = Touchable.disabled
            setFontScale(STATS_FONT_SCALE)
        }).padRight(4f)
        add(value.apply {
            touchable = Touchable.disabled
            setFontScale(STATS_FONT_SCALE)
        })
    }

    private fun updateTimingLabels(timing: DebugSimulationTiming) {
        realDeltaLabel.setText(timing.real)
        simulationDeltaLabel.setText(timing.simulation)
        scaleLabel.setText(timing.scale)
    }

    private fun speedLabel(scale: Float) = when (scale) {
        0.25f -> "0.25x"
        0.5f -> "0.5x"
        1f -> "1x"
        2f -> "2x"
        4f -> "4x"
        else -> error("Unsupported debug simulation speed: $scale")
    }

    private companion object {
        const val CONTROL_HEIGHT = 32f
        const val STATS_HEIGHT = 22f
        const val STATS_FONT_SCALE = 0.9f
        const val REFRESH_INTERVAL_SECONDS = 0.25f
    }
}

class DebugSimulationOverlayState(private val simulation: SimulationController) {
    var visible: Boolean = false
        private set
    val paused get() = simulation.paused
    val stepAvailable get() = paused
    val pauseButtonText get() = if (paused) "Resume" else "Pause"
    val selectedTimeScale get() = TIME_SCALES.firstOrNull { it == simulation.timeScale }

    /** Formats averaged simulation timing values for the debug overlay. */
    fun timing(
        realDelta: Float,
        simulationDelta: Float
    ) = DebugSimulationTiming(
        real = "${formatMs(realDelta)} ms",
        simulation = "${formatMs(simulationDelta)} ms",
        scale = "${formatScale(simulation.timeScale)}x"
    )

    fun setVisible(visible: Boolean) {
        this.visible = visible
    }

    fun togglePause() = simulation.togglePause()

    fun selectTimeScale(timeScale: Float) {
        require(timeScale in TIME_SCALES) { "Unsupported debug simulation speed: $timeScale" }
        simulation.timeScale = timeScale
    }

    fun step() {
        simulation.step()
    }

    fun setCustomTimeScale(timeScale: Float) {
        simulation.timeScale = timeScale
    }

    fun resetTimeScale() {
        simulation.resetTimeScale()
    }

    private fun formatMs(delta: Float) = String.format(Locale.ROOT, "%.2f", delta * 1000f)

    private fun formatScale(scale: Float): String =
        String.format(Locale.ROOT, "%.2f", scale)

    companion object {
        val TIME_SCALES = listOf(0.25f, 0.5f, 1f, 2f, 4f)
    }
}

data class DebugSimulationTiming(
    val real: String,
    val simulation: String,
    val scale: String
)
