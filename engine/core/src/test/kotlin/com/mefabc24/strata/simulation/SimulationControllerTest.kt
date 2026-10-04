package com.mefabc24.strata.simulation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulationControllerTest {

    @Test
    fun `defaults to running at normal speed`() {
        val simulation = SimulationController()

        assertFalse(simulation.paused)
        assertEquals(1f, simulation.timeScale)
        assertEquals(0.1f, simulation.simulationDelta(0.1f))
    }

    @Test
    fun `scales real frame delta`() {
        val simulation = SimulationController()

        simulation.timeScale = 2f
        assertEquals(0.2f, simulation.simulationDelta(0.1f))

        simulation.timeScale = 0.5f
        assertEquals(0.05f, simulation.simulationDelta(0.1f))
        assertEquals(0.1f, simulation.lastRealDelta)
        assertEquals(0.05f, simulation.lastSimulationDelta)
    }

    @Test
    fun `pause and resume preserve time scale`() {
        val simulation = SimulationController()
        simulation.timeScale = 4f

        simulation.pause()
        assertTrue(simulation.paused)
        assertEquals(0f, simulation.simulationDelta(0.1f))
        assertEquals(0f, simulation.lastSimulationDelta)
        assertEquals(4f, simulation.timeScale)

        simulation.resume()
        assertFalse(simulation.paused)
        assertEquals(0.4f, simulation.simulationDelta(0.1f))
    }

    @Test
    fun `reset restores normal custom speed and preserves pause state`() {
        val simulation = SimulationController().apply {
            timeScale = 3.75f
            pause()
        }

        simulation.resetTimeScale()

        assertEquals(1f, simulation.timeScale)
        assertTrue(simulation.paused)
    }

    @Test
    fun `single step reports its effective delta`() {
        val simulation = SimulationController().apply { pause(); step() }

        assertEquals(1f / 60f, simulation.simulationDelta(0.5f))
        assertEquals(0.5f, simulation.lastRealDelta)
        assertEquals(1f / 60f, simulation.lastSimulationDelta)
    }

    @Test
    fun `toggle pause changes state`() {
        val simulation = SimulationController()

        simulation.togglePause()
        assertTrue(simulation.paused)

        simulation.togglePause()
        assertFalse(simulation.paused)
    }

    @Test
    fun `rejects invalid time scales`() {
        val simulation = SimulationController()

        for (invalid in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                simulation.timeScale = invalid
            }
        }
    }

    @Test
    fun `rejects invalid real deltas`() {
        val simulation = SimulationController()

        for (invalid in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                simulation.simulationDelta(invalid)
            }
        }
    }
}
