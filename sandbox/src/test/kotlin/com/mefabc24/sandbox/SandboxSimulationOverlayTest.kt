package com.mefabc24.sandbox

import com.mefabc24.strata.simulation.SimulationController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxSimulationOverlayTest {

    @Test
    fun `visibility changes do not alter simulation state`() {
        val simulation = SimulationController().apply {
            timeScale = 4f
            pause()
        }
        val state = SandboxSimulationOverlayState(simulation)

        state.setVisible(true)
        assertTrue(state.visible)
        state.setVisible(false)

        assertFalse(state.visible)
        assertTrue(simulation.paused)
        assertEquals(4f, simulation.timeScale)
    }

    @Test
    fun `speed selection updates controller`() {
        val simulation = SimulationController()
        val state = SandboxSimulationOverlayState(simulation)

        simulation.pause()
        state.selectTimeScale(0.25f)
        assertTrue(simulation.paused)
        assertEquals(0.25f, simulation.timeScale)
        assertEquals(0.25f, state.selectedTimeScale)

        state.selectTimeScale(4f)
        assertEquals(4f, simulation.timeScale)
        assertEquals(4f, state.selectedTimeScale)
    }

    @Test
    fun `selection reflects external controller changes`() {
        val simulation = SimulationController()
        val state = SandboxSimulationOverlayState(simulation)

        simulation.timeScale = 2f
        assertEquals(2f, state.selectedTimeScale)

        simulation.timeScale = 1.5f
        assertNull(state.selectedTimeScale)
    }

    @Test
    fun `pause control toggles controller and label`() {
        val simulation = SimulationController()
        val state = SandboxSimulationOverlayState(simulation)

        assertFalse(state.paused)
        assertEquals("Pause", state.pauseButtonText)

        state.togglePause()
        assertTrue(state.paused)
        assertEquals("Resume", state.pauseButtonText)

        state.togglePause()
        assertFalse(state.paused)
        assertEquals("Pause", state.pauseButtonText)
    }
}
