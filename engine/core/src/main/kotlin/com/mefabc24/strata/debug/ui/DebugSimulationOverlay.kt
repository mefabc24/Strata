package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
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
        "Reset speed",
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
    private val timingRows = DebugDiagnosticTable(
        skin = ui.skin,
        wrapValues = false,
        keyMinimumWidth = 90f,
        valueMinimumWidth = 72f
    ).apply {
        background = ui.skin.get("debug-setting-row", StrataPanelStyle::class.java).background
        pad(6f)
        show(state.timingRows(0f, 0f))
    }
    private val root = Table().apply {
        setFillParent(true)
        bottom().pad(16f)
        touchable = Touchable.childrenOnly
        isVisible = false
        add(simulationPanel(ui)).fillX()
    }

    init {
        ui.stage.addActor(root)
        sync()
    }

    fun setVisible(visible: Boolean) {
        state.setVisible(visible)
        sync()
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

        timingRows.show(
            state.timingRows(
                realDelta = accumulatedRealDelta / sampledFrames,
                simulationDelta = accumulatedSimulationDelta / sampledFrames
            )
        )

        refreshElapsed = 0f
        accumulatedRealDelta = 0f
        accumulatedSimulationDelta = 0f
        sampledFrames = 0
    }

    private fun simulationPanel(ui: StrataUi): Table {
        val panelStyle = ui.skin.get(
            requireNotNull(ui.theme.panelStyle),
            StrataPanelStyle::class.java
        )
        val settingBackground = ui.skin.get(
            "debug-setting-row",
            StrataPanelStyle::class.java
        ).background
        val separatorStyle = ui.skin.get(
            requireNotNull(ui.theme.separatorStyle),
            StrataSeparatorStyle::class.java
        )

        return Table(ui.skin).apply {
            background = panelStyle.background
            pad(
                panelStyle.padTop,
                panelStyle.padLeft,
                panelStyle.padBottom,
                panelStyle.padRight
            )
            touchable = Touchable.childrenOnly

            add(Label("SIMULATION", ui.skin, "title").apply {
                setAlignment(Align.left)
                touchable = Touchable.disabled
            }).growX().fillX().left().padBottom(6f)
            row()
            add(StrataSeparator(StrataSeparatorOrientation.HORIZONTAL, separatorStyle))
                .growX().height(separatorStyle.thickness).padBottom(6f)
            row()

            add(Table(ui.skin).apply {
                background = settingBackground
                pad(2f)
                add(pauseButton).growX().minWidth(116f).height(CONTROL_HEIGHT).padRight(2f)
                add(stepButton).minWidth(76f).height(CONTROL_HEIGHT)
            }).growX().fillX()
            row()

            add(Label("SPEED", ui.skin, "title").apply {
                setAlignment(Align.left)
                touchable = Touchable.disabled
            }).growX().fillX().left().padTop(8f).padBottom(4f)
            row()
            add(Table(ui.skin).apply {
                background = settingBackground
                pad(2f)
                DebugSimulationOverlayState.TIME_SCALES.forEachIndexed { index, scale ->
                    add(speedButtons.getValue(scale))
                        .minWidth(52f)
                        .height(CONTROL_HEIGHT)
                        .padRight(if (index == speedButtons.size - 1) 0f else 2f)
                }
            }).growX().fillX()
            row()
            add(resetSpeedButton).growX().fillX().height(CONTROL_HEIGHT).padTop(2f)
            row()

            add(StrataSeparator(StrataSeparatorOrientation.HORIZONTAL, separatorStyle))
                .growX().height(separatorStyle.thickness).padTop(8f).padBottom(6f)
            row()
            add(timingRows).growX().fillX().left()
        }
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
    fun timingRows(
        realDelta: Float,
        simulationDelta: Float
    ): List<DebugDiagnosticRow> = listOf(
        DebugDiagnosticRow("Real", "${formatMs(realDelta)} ms"),
        DebugDiagnosticRow("Simulation", "${formatMs(simulationDelta)} ms"),
        DebugDiagnosticRow("Scale", "${formatScale(simulation.timeScale)}x")
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
