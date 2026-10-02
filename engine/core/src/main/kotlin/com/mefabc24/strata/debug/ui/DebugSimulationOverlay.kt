package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.ui.StrataButton
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi

internal class DebugSimulationOverlay(
    ui: StrataUi,
    simulation: SimulationController
) {
    private val state = DebugSimulationOverlayState(simulation)
    private val pauseButton = StrataButton(
        state.pauseButtonText, ui.skin, ui.theme.buttonStyle
    ) { state.togglePause(); sync() }
    private val stepButton = StrataButton(
        "Step",
        ui.skin,
        ui.theme.buttonStyle
    ) {
        state.step()
    }
    private val resetSpeedButton = StrataButton(
        "Reset speed", ui.skin, ui.theme.buttonStyle
    ) {
        state.resetTimeScale()
        sync()
    }
    private val deltaLabel = Label("", ui.skin)
    private val speedButtons = DebugSimulationOverlayState.TIME_SCALES.associateWith { scale ->
        StrataButton(speedLabel(scale), ui.skin, ui.theme.buttonStyle) {
            state.selectTimeScale(scale)
            sync()
        }
    }
    private val root = Table().apply {
        setFillParent(true)
        bottom().pad(16f)
        touchable = Touchable.childrenOnly
        isVisible = false
        add(Table(ui.skin).apply {
            background = ui.skin.get(
                requireNotNull(ui.theme.panelStyle), StrataPanelStyle::class.java
            ).background
            pad(8f)
            touchable = Touchable.childrenOnly
            add(pauseButton).minWidth(78f).height(34f).padRight(4f)
            add(stepButton).minWidth(64f).height(34f).padRight(4f)
            DebugSimulationOverlayState.TIME_SCALES.forEachIndexed { index, scale ->
                add(speedButtons.getValue(scale)).minWidth(52f).height(34f)
                    .padRight(4f)
            }
            add(resetSpeedButton).minWidth(88f).height(34f)
            row()
            add(deltaLabel).colspan(8).growX().left().padTop(5f)
        })
    }

    init { ui.stage.addActor(root); sync() }

    fun setVisible(visible: Boolean) { state.setVisible(visible); sync() }
    fun sync() {
        root.isVisible = state.visible

        pauseButton.setText(state.pauseButtonText)
        pauseButton.isChecked = state.paused

        stepButton.isDisabled = !state.paused

        speedButtons.forEach { (scale, button) ->
            button.isChecked = scale == state.selectedTimeScale
        }
        deltaLabel.setText(state.deltaText)
    }

    private fun speedLabel(scale: Float) = when (scale) {
        0.25f -> "0.25x"; 0.5f -> "0.5x"; 1f -> "1x"; 2f -> "2x"; 4f -> "4x"
        else -> error("Unsupported debug simulation speed: $scale")
    }
}

class DebugSimulationOverlayState(private val simulation: SimulationController) {
    var visible: Boolean = false
        private set
    val paused get() = simulation.paused
    val pauseButtonText get() = if (paused) "Resume" else "Pause"
    val selectedTimeScale get() = TIME_SCALES.firstOrNull { it == simulation.timeScale }
    val deltaText: String
        get() = "Real Δ ${formatMs(simulation.lastRealDelta)} ms   " +
            "Simulation Δ ${formatMs(simulation.lastSimulationDelta)} ms   " +
            "Scale ${simulation.timeScale}x"
    fun setVisible(visible: Boolean) { this.visible = visible }
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
    private fun formatMs(delta: Float) = String.format(java.util.Locale.ROOT, "%.2f", delta * 1000f)
    companion object { val TIME_SCALES = listOf(0.25f, 0.5f, 1f, 2f, 4f) }
}
