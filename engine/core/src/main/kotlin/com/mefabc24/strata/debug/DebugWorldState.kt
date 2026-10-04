package com.mefabc24.strata.debug

import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.WorldEntity
import com.mefabc24.strata.iso.PickingDebugSnapshot
import com.mefabc24.strata.debug.tools.DebugMovePreview
import com.mefabc24.strata.debug.tools.DebugBrushPreview
import com.mefabc24.strata.render.preview.EntityPreview
import com.mefabc24.strata.iso.PickedTarget

/** Mutable runtime-only state consumed by debug world rendering. */
internal class DebugWorldState {
    val entityTrails = DebugEntityTrailRecorder()
    var inspection: DebugInspection? = null
    var inspectionHighlightVisible: Boolean = false
    var pathfinding: PathfindingDiagnosticResult? = null
    var pathfindingWaypoints: List<TilePosition> = emptyList()
    var pathfindingEntity: WorldEntity? = null
    var pathfindingEntityWaiting: Boolean = false
    var cursorWorld: Vector2? = null
    var picking: PickingDebugSnapshot? = null
    var hoveredTarget: PickedTarget? = null
    val pickingSelection = DebugPickingSelection()
    var movePreview: DebugMovePreview? = null
    var spawnPreview: EntityPreview? = null
    var brushPreview: DebugBrushPreview? = null
}
