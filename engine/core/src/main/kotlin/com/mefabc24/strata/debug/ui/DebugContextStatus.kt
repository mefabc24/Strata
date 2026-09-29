package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.inspector.formatTilePosition
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.placement.PlacementDiagnostic
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.debug.tools.DebugMovePreview

internal data class DebugContextStatus(
    val rows: List<DebugDiagnosticRow>
)

internal data class DebugContextInputs(
    val mode: DebugToolMode,
    val buildObject: String? = null,
    val placementAvailable: Boolean = true,
    val placementDiagnostic: PlacementDiagnostic? = null,
    val buildDragging: Boolean = false,
    val buildPreviewCount: Int = 0,
    val paintTerrain: String? = null,
    val paintLayer: String? = null,
    val spawnEntity: String? = null,
    val inspection: String? = null,
    val pathStart: TilePosition? = null,
    val pathResult: PathfindingDiagnosticResult? = null,
    val movePreview: DebugMovePreview? = null
)

internal fun debugContextStatus(
    input: DebugContextInputs
): DebugContextStatus? = when (input.mode) {
    DebugToolMode.NONE -> null
    DebugToolMode.FREE_CAMERA -> DebugContextStatus(
        diagnosticRows("Camera" to "Unrestricted pan and zoom")
    )
    DebugToolMode.BUILD -> DebugContextStatus(buildStatusRows(input))
    DebugToolMode.MOVE -> DebugContextStatus(moveStatusRows(input.movePreview))
    DebugToolMode.PAINT -> DebugContextStatus(
        diagnosticRows(
            "Terrain" to (input.paintTerrain ?: "No terrain selected"),
            "Layer" to (input.paintLayer ?: "Ground")
        )
    )
    DebugToolMode.SPAWN -> DebugContextStatus(
        diagnosticRows("Entity" to (input.spawnEntity ?: "No entity selected"))
    )
    DebugToolMode.INSPECT -> DebugContextStatus(
        diagnosticRows("Selection" to (input.inspection ?: "Click a world target"))
    )
    DebugToolMode.PATHFINDING -> DebugContextStatus(pathStatusRows(input))
}

private fun moveStatusRows(preview: DebugMovePreview?): List<DebugDiagnosticRow> {
    if (preview == null) return diagnosticRows("Status" to "Drag an object or entity")
    return diagnosticRows(
        "Target type" to preview.type.name.lowercase().replaceFirstChar(Char::titlecase),
        "Source" to formatTilePosition(preview.source),
        "Target" to formatTilePosition(preview.target),
        "Status" to if (preview.valid) "Valid" else "Invalid: ${preview.rejection?.name?.lowercase()?.replace('_', ' ')}"
    )
}

private fun buildStatusRows(input: DebugContextInputs): List<DebugDiagnosticRow> {
    val rows = mutableListOf(
        DebugDiagnosticRow("Object", input.buildObject ?: "No object selected")
    )
    val status = when {
        !input.placementAvailable -> "Placement unavailable"
        input.buildDragging -> {
            val count = input.buildPreviewCount
            val base = "Dragging $count ${if (count == 1) "placement" else "placements"}"
            val diagnostic = input.placementDiagnostic
            if (diagnostic?.valid == false) {
                "$base — invalid: ${placementReasonText(diagnostic.reason).lowercase()}"
            } else {
                base
            }
        }
        input.placementDiagnostic == null -> "Hover a tile to preview"
        input.placementDiagnostic.valid -> "Placement valid"
        else -> "Invalid: ${placementReasonText(input.placementDiagnostic.reason).lowercase()}"
    }
    rows += DebugDiagnosticRow("Status", status)
    return rows
}

private fun pathStatusRows(input: DebugContextInputs): List<DebugDiagnosticRow> {
    val result = input.pathResult
    return when {
        input.pathStart != null -> diagnosticRows(
            "Start" to formatTilePosition(input.pathStart),
            "Status" to "Click a goal tile"
        )
        result == null -> diagnosticRows(
            "Status" to "Click a start tile, then a goal tile"
        )
        else -> diagnosticRows(
            "Start" to formatTilePosition(result.start),
            "Goal" to formatTilePosition(result.goal),
            "Result" to if (result.success) "Success" else "No path",
            "Length" to (result.path?.size ?: 0).toString()
        )
    }
}
