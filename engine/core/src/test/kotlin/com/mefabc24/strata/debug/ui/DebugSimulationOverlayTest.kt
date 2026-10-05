package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.simulation.SimulationController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugSimulationOverlayTest {
    @Test
    fun `visibility does not alter simulation state`() {
        val simulation = SimulationController().apply {
            timeScale = 4f
            pause()
        }
        val state = DebugSimulationOverlayState(simulation)

        state.setVisible(true)

        assertTrue(state.visible)
        assertTrue(simulation.paused)
        assertEquals(4f, simulation.timeScale)
    }

    @Test
    fun `pause presentation and step availability follow external simulation state`() {
        val simulation = SimulationController()
        val state = DebugSimulationOverlayState(simulation)

        assertFalse(state.paused)
        assertFalse(state.stepAvailable)
        assertEquals("Pause", state.pauseButtonText)

        simulation.pause()

        assertTrue(state.paused)
        assertTrue(state.stepAvailable)
        assertEquals("Resume", state.pauseButtonText)

        state.togglePause()

        assertFalse(simulation.paused)
        assertFalse(state.stepAvailable)
    }

    @Test
    fun `fixed time scales remain an exclusive presentation selection`() {
        val simulation = SimulationController()
        val state = DebugSimulationOverlayState(simulation)

        assertEquals(
            listOf(0.25f, 0.5f, 1f, 2f, 4f),
            DebugSimulationOverlayState.TIME_SCALES
        )
        DebugSimulationOverlayState.TIME_SCALES.forEach { scale ->
            state.selectTimeScale(scale)

            assertEquals(scale, simulation.timeScale)
            assertEquals(scale, state.selectedTimeScale)
        }
    }

    @Test
    fun `custom external time scale clears preset selection and reset restores one times`() {
        val simulation = SimulationController().apply { timeScale = 1.5f }
        val state = DebugSimulationOverlayState(simulation)

        assertNull(state.selectedTimeScale)

        state.setCustomTimeScale(3.25f)

        assertEquals(3.25f, simulation.timeScale)
        assertNull(state.selectedTimeScale)

        state.resetTimeScale()

        assertEquals(1f, simulation.timeScale)
        assertEquals(1f, state.selectedTimeScale)
    }

    @Test
    fun `timing rows format values consistently`() {
        val simulation = SimulationController().apply {
            timeScale = 1.1f
        }
        val state = DebugSimulationOverlayState(simulation)

        assertEquals(
            listOf(
                DebugDiagnosticRow("Real", "16.67 ms"),
                DebugDiagnosticRow("Simulation", "18.33 ms"),
                DebugDiagnosticRow("Scale", "1.10x")
            ),
            state.timingRows(0.01667f, 0.01833f)
        )
    }
}
