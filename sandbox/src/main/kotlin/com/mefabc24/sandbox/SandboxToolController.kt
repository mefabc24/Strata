package com.mefabc24.sandbox

import com.mefabc24.strata.placement.PlacementController

/** Keeps Sandbox editing systems synchronized with one explicit tool mode. */
class SandboxToolController(
    private val painter: SandboxTerrainPainter,
    private val placement: PlacementController,
    private val buildDrag: SandboxBuildDragController
) {
    var mode: SandboxMode = SandboxMode.NONE
        private set

    init {
        applyMode()
    }

    fun select(mode: SandboxMode) {
        if (this.mode == mode) return

        if (this.mode == SandboxMode.BUILD) {
            buildDrag.cancel()
        }
        if (this.mode == SandboxMode.PAINT) {
            painter.cancel()
        }

        this.mode = mode
        applyMode()
    }

    private fun applyMode() {
        painter.enabled = mode == SandboxMode.PAINT
        placement.enabled = mode == SandboxMode.BUILD
    }
}
