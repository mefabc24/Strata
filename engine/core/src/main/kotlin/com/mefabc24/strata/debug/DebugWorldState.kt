package com.mefabc24.strata.debug

import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.iso.PickingDebugSnapshot
import com.mefabc24.strata.debug.tools.DebugMovePreview
import com.mefabc24.strata.render.preview.EntityPreview

/** Mutable runtime-only state consumed by debug world rendering. */
internal class DebugWorldState {
    var inspection: DebugInspection? = null
    var inspectionHighlightVisible: Boolean = false
    var pathfinding: PathfindingDiagnosticResult? = null
    var pathStart: TilePosition? = null
    var cursorWorld: Vector2? = null
    var picking: PickingDebugSnapshot? = null
    val pickingSelection = DebugPickingSelection()
    var movePreview: DebugMovePreview? = null
    var spawnPreview: EntityPreview? = null
}
