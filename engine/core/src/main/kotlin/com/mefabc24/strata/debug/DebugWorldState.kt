package com.mefabc24.strata.debug

import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.WorldEntity
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.iso.PickingDebugSnapshot

/** Mutable runtime-only state consumed by debug world rendering. */
internal class DebugWorldState {
    var inspection: DebugInspection? = null
    var inspectionHighlightVisible: Boolean = false
    var pathfinding: PathfindingDiagnosticResult? = null
    var pathStart: TilePosition? = null
    var cursorWorld: Vector2? = null
    var pickedObject: PlacedObject? = null
    var pickedEntity: WorldEntity? = null
    var picking: PickingDebugSnapshot? = null
}
