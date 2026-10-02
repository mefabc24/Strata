package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.simulation.SimulationController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugSimulationOverlayTest {
    @Test
    fun `visibility does not alter simulation and controls reflect external state`() {
        val simulation = SimulationController().apply { timeScale = 4f; pause() }
        val state = DebugSimulationOverlayState(simulation)
        state.setVisible(true)
        assertTrue(state.visible)
        assertTrue(state.paused)
        assertEquals(4f, state.selectedTimeScale)
        state.togglePause()
        assertFalse(simulation.paused)
        state.selectTimeScale(0.25f)
        assertEquals(0.25f, simulation.timeScale)
        simulation.timeScale = 1.5f
        assertNull(state.selectedTimeScale)
        state.setCustomTimeScale(3.25f)
        assertEquals(3.25f, simulation.timeScale)
        state.resetTimeScale()
        assertEquals(1f, simulation.timeScale)
    }
}
