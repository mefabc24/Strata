package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.`object`.ObjectEntry

/** Keeps debug interactions synchronized with one explicit tool mode. */
class DebugToolController(
    private val painter: DebugTerrainPainter,
    private val placement: PlacementController?,
    private val buildDrag: DebugBuildDragController?,
    private val inspector: DebugInspector,
    private val pathfinding: DebugPathfindingTool
) {
    private var savedPlacementEnabled: Boolean? = null
    private var savedPlacementFactory: (() -> com.mefabc24.strata.world.Placeable)? = null
    private var debugPlacementFactory: (() -> com.mefabc24.strata.world.Placeable)? = null

    var mode: DebugToolMode = DebugToolMode.NONE
        private set

    val buildAvailable: Boolean get() = placement != null && buildDrag != null

    fun selectBuildEntry(entry: ObjectEntry?) {
        require(entry == null || entry.isConstructible) {
            "A debug Build entry must have a registered factory."
        }
        buildDrag?.cancel()
        debugPlacementFactory = entry?.let { selected -> selected::create }
        if (mode == DebugToolMode.BUILD) {
            placement?.selectedFactory = debugPlacementFactory
        }
    }

    fun select(mode: DebugToolMode) {
        require(mode != DebugToolMode.BUILD || buildAvailable) {
            "The Build debug tool requires a configured PlacementController."
        }
        if (this.mode == mode) return
        leave(this.mode)
        this.mode = mode
        enter(mode)
    }

    fun clearSelection() {
        inspector.clear()
        pathfinding.clear()
    }

    private fun leave(mode: DebugToolMode) {
        when (mode) {
            DebugToolMode.BUILD -> {
                buildDrag?.cancel()
                placement?.let { controller ->
                    controller.selectedFactory = savedPlacementFactory
                    controller.enabled = savedPlacementEnabled ?: controller.enabled
                }
                savedPlacementEnabled = null
                savedPlacementFactory = null
            }
            DebugToolMode.PAINT -> painter.cancel()
            DebugToolMode.PATHFINDING -> pathfinding.clear()
            else -> Unit
        }
        painter.enabled = false
    }

    private fun enter(mode: DebugToolMode) {
        if (mode == DebugToolMode.BUILD) {
            placement?.let { controller ->
                savedPlacementEnabled = controller.enabled
                savedPlacementFactory = controller.selectedFactory
                controller.selectedFactory = debugPlacementFactory
                controller.enabled = true
            }
        }
        painter.enabled = mode == DebugToolMode.PAINT
    }
}
