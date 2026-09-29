package com.mefabc24.strata.simulation

/**
 * Controls how quickly simulation time advances relative to real frame time.
 *
 * Pausing is independent of [timeScale], so resuming continues at the
 * previously selected speed. Camera movement, UI, input, rendering, and
 * performance measurement use real frame time and are not paused. Game-owned
 * simulation should consume the simulation delta supplied by [com.mefabc24.strata.StrataGame].
 */
class SimulationController {

    /** Multiplier applied to real frame delta while simulation is running. */
    var timeScale: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Simulation time scale must be finite and greater than zero."
            }

            field = value
        }

    /** Whether simulation progression is currently paused. */
    var paused: Boolean = false
        private set

    private var stepRequested: Boolean = false

    /** Pauses simulation progression without changing [timeScale]. */
    fun pause() {
        paused = true
    }

    /** Resumes simulation progression at the current [timeScale]. */
    fun resume() {
        stepRequested = false
        paused = false
    }

    /** Switches between paused and running simulation states. */
    fun togglePause() {
        paused = !paused

        if (!paused) {
            stepRequested = false
        }
    }

    /**
     * Advances a paused simulation by one fixed simulation step.
     */
    fun step() {
        if (!paused) return
        stepRequested = true
    }

    internal fun simulationDelta(realDelta: Float): Float {
        require(realDelta.isFinite() && realDelta >= 0f) {
            "Real frame delta must be finite and non-negative."
        }

        if (paused) {
            if (!stepRequested) return 0f

            stepRequested = false
            return FIXED_STEP
        }

        val simulationDelta = realDelta * timeScale

        require(simulationDelta.isFinite()) {
            "Scaled simulation delta must remain finite."
        }

        return simulationDelta
    }

    private companion object {
        const val FIXED_STEP = 1f / 60f
    }
}


