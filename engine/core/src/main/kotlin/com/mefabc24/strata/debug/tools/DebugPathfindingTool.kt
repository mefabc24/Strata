package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.pathfinding.findPathDiagnostic
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.debug.DebugWorldState

/** Two-click path search state using optional game-supplied traversal rules. */
class DebugPathfindingTool internal constructor(
    private val world: World,
    private val state: DebugWorldState,
    private val canEnter: (TilePosition) -> Boolean = { true }
) {
    constructor(
        world: World,
        canEnter: (TilePosition) -> Boolean = { true }
    ) : this(world, DebugWorldState(), canEnter)

    var start: TilePosition? = null
        private set(value) {
            field = value
            state.pathStart = value
        }
    val result: PathfindingDiagnosticResult?
        get() = state.pathfinding

    fun click(position: TilePosition): Boolean {
        if (world.getTile(position) == null) return false
        val currentStart = start
        if (currentStart == null) {
            start = position
            state.pathfinding = null
        } else {
            state.pathfinding = world.findPathDiagnostic(currentStart, position, canEnter)
            start = null
        }
        return true
    }

    fun clear(): Boolean {
        val changed = start != null || state.pathfinding != null
        start = null
        state.pathfinding = null
        return changed
    }
}
