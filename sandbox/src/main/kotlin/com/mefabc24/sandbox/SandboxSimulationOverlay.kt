package com.mefabc24.sandbox

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.ui.StrataButton
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi

internal class SandboxSimulationOverlay(
    ui: StrataUi,
    simulation: SimulationController
) {

    private val state = SandboxSimulationOverlayState(simulation)

    private val pauseButton = StrataButton(
        text = state.pauseButtonText,
        skin = ui.skin,
        styleName = ui.theme.buttonStyle
    ) {
        state.togglePause()
        sync()
    }

    private val speedButtons =
        SandboxSimulationOverlayState.TIME_SCALES.associateWith { timeScale ->
            StrataButton(
                text = speedLabel(timeScale),
                skin = ui.skin,
                styleName = ui.theme.buttonStyle
            ) {
                state.selectTimeScale(timeScale)
                sync()
            }
        }

    private val panel = Table(ui.skin).apply {
        val panelStyleName = requireNotNull(ui.theme.panelStyle) {
            "The Sandbox simulation overlay requires a panel style."
        }
        background = ui.skin.get(
            panelStyleName,
            StrataPanelStyle::class.java
        ).background
        pad(CONTENT_PADDING)
        touchable = Touchable.childrenOnly

        add(pauseButton)
            .minWidth(PAUSE_BUTTON_WIDTH)
            .height(BUTTON_HEIGHT)
            .padRight(BUTTON_GAP)

        for ((index, timeScale) in SandboxSimulationOverlayState.TIME_SCALES.withIndex()) {
            add(speedButtons.getValue(timeScale))
                .minWidth(SPEED_BUTTON_WIDTH)
                .height(BUTTON_HEIGHT)
                .padRight(
                    if (index == SandboxSimulationOverlayState.TIME_SCALES.lastIndex) {
                        0f
                    } else {
                        BUTTON_GAP
                    }
                )
        }
    }

    private val root = Table().apply {
        setFillParent(true)
        bottom()
        pad(BOTTOM_MARGIN)
        touchable = Touchable.childrenOnly
        isVisible = false
        add(panel)
    }

    init {
        ui.stage.addActor(root)
        sync()
    }

    fun setVisible(visible: Boolean) {
        state.setVisible(visible)
        root.isVisible = state.visible
    }

    fun sync() {
        root.isVisible = state.visible
        pauseButton.setText(state.pauseButtonText)
        pauseButton.isChecked = state.paused

        val selectedTimeScale = state.selectedTimeScale
        for ((timeScale, button) in speedButtons) {
            button.isChecked = timeScale == selectedTimeScale
        }
    }

    private companion object {
        const val BOTTOM_MARGIN = 16f
        const val CONTENT_PADDING = 8f
        const val BUTTON_GAP = 4f
        const val BUTTON_HEIGHT = 36f
        const val PAUSE_BUTTON_WIDTH = 78f
        const val SPEED_BUTTON_WIDTH = 58f

        fun speedLabel(timeScale: Float): String = when (timeScale) {
            0.25f -> "0.25x"
            0.5f -> "0.5x"
            1f -> "1x"
            2f -> "2x"
            4f -> "4x"
            else -> error("Unsupported Sandbox simulation speed: $timeScale")
        }
    }
}

internal class SandboxSimulationOverlayState(
    private val simulation: SimulationController
) {

    var visible: Boolean = false
        private set

    val paused: Boolean
        get() = simulation.paused

    val pauseButtonText: String
        get() = if (simulation.paused) "Resume" else "Pause"

    val selectedTimeScale: Float?
        get() = TIME_SCALES.firstOrNull { it == simulation.timeScale }

    fun setVisible(visible: Boolean) {
        this.visible = visible
    }

    fun togglePause() {
        simulation.togglePause()
    }

    fun selectTimeScale(timeScale: Float) {
        require(timeScale in TIME_SCALES) {
            "Unsupported Sandbox simulation speed: $timeScale"
        }
        simulation.timeScale = timeScale
    }

    companion object {
        val TIME_SCALES = listOf(0.25f, 0.5f, 1f, 2f, 4f)
    }
}
