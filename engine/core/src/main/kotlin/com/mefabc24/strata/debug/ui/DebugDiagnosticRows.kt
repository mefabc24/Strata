package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.pathfinding.PathfindingSearchStatus
import com.mefabc24.strata.placement.PlacementDiagnostic
import com.mefabc24.strata.placement.PlacementFailureReason
import com.mefabc24.strata.debug.inspector.formatTilePosition
import com.mefabc24.strata.debug.inspector.formatTilePositions
import com.mefabc24.strata.world.TilePosition
import java.util.Locale

data class DebugDiagnosticRow(val key: String, val value: String)
internal const val EMPTY_DIAGNOSTIC_VALUE = "-"

internal fun debugDiagnosticValue(value: Any?): String =
    value?.toString() ?: EMPTY_DIAGNOSTIC_VALUE

internal enum class DebugDiagnosticLayoutState { COLLAPSED, EXPANDED }

/** A compact two-column diagnostic view with distinct key and value styles. */
internal class DebugDiagnosticTable(
    private val skin: Skin,
    private val keyStyle: String = "debug-key",
    private val valueStyle: String = "default",
    private val wrapValues: Boolean = true,
    private val keyMinimumWidth: Float = 0f,
    private val valueMinimumWidth: Float = 0f
) : Table(skin) {
    private var keys: List<String> = emptyList()
    private var valueLabels: List<Label> = emptyList()
    private var layoutCell: Cell<DebugDiagnosticTable>? = null
    private var visiblePadTop = 0f
    private var visiblePadLeft = 0f
    private var visiblePadBottom = 0f
    private var visiblePadRight = 0f
    private var visibleSpaceTop = 0f
    private var visibleSpaceLeft = 0f
    private var visibleSpaceBottom = 0f
    private var visibleSpaceRight = 0f

    var currentRows: List<DebugDiagnosticRow> = emptyList()
        private set

    var layoutState: DebugDiagnosticLayoutState = DebugDiagnosticLayoutState.COLLAPSED
        private set

    init {
        top().left()
        touchable = Touchable.disabled
        isVisible = false
    }

    fun show(rows: List<DebugDiagnosticRow>) {
        currentRows = rows
        isVisible = rows.isNotEmpty()
        updateLayoutCell()
        if (rows.isEmpty()) {
            invalidateHierarchy()
            return
        }
        val newKeys = rows.map(DebugDiagnosticRow::key)
        if (newKeys != keys) rebuild(newKeys)
        rows.forEachIndexed { index, row -> valueLabels[index].setText(row.value) }
        invalidateHierarchy()
    }

    internal fun bindLayout(cell: Cell<DebugDiagnosticTable>) {
        layoutCell = cell
        visiblePadTop = cell.padTop
        visiblePadLeft = cell.padLeft
        visiblePadBottom = cell.padBottom
        visiblePadRight = cell.padRight
        visibleSpaceTop = cell.spaceTop
        visibleSpaceLeft = cell.spaceLeft
        visibleSpaceBottom = cell.spaceBottom
        visibleSpaceRight = cell.spaceRight
        updateLayoutCell()
    }

    private fun updateLayoutCell() {
        layoutState = if (currentRows.isEmpty()) {
            DebugDiagnosticLayoutState.COLLAPSED
        } else {
            DebugDiagnosticLayoutState.EXPANDED
        }
        val cell = layoutCell ?: return
        if (currentRows.isEmpty()) {
            cell.minHeight(0f).prefHeight(0f).maxHeight(0f).pad(0f).space(0f)
        } else {
            cell
                .minHeight(Value.minHeight)
                .prefHeight(Value.prefHeight)
                .maxHeight(Value.maxHeight)
                .padTop(visiblePadTop)
                .padLeft(visiblePadLeft)
                .padBottom(visiblePadBottom)
                .padRight(visiblePadRight)
                .spaceTop(visibleSpaceTop)
                .spaceLeft(visibleSpaceLeft)
                .spaceBottom(visibleSpaceBottom)
                .spaceRight(visibleSpaceRight)
        }
        invalidateHierarchy()
    }

    private fun rebuild(newKeys: List<String>) {
        clearChildren()
        keys = newKeys
        valueLabels = newKeys.map { key ->
            add(Label(key, skin, keyStyle).apply { setAlignment(Align.topLeft) })
                .top().left().padRight(10f).minWidth(keyMinimumWidth)
            val value = Label("", skin, valueStyle).apply {
                setAlignment(Align.topLeft)
                setWrap(wrapValues)
            }
            add(value).growX().fillX().top().left().minWidth(valueMinimumWidth)
            row()
            value
        }
    }
}

internal fun placementDiagnosticRows(
    placementAvailable: Boolean,
    diagnostics: List<PlacementDiagnostic>
): List<DebugDiagnosticRow> {
    if (!placementAvailable) {
        return listOf(DebugDiagnosticRow("Status", "Placement unavailable"))
    }
    if (diagnostics.isEmpty()) {
        return listOf(DebugDiagnosticRow("Status", "Hover or drag to preview"))
    }
    val invalid = diagnostics.filterNot(PlacementDiagnostic::valid)
    val rows = mutableListOf(
        DebugDiagnosticRow("Status", if (invalid.isEmpty()) "Valid" else "Invalid")
    )
    if (diagnostics.size > 1) {
        rows += DebugDiagnosticRow("Previews", diagnostics.size.toString())
        rows += DebugDiagnosticRow("Valid", (diagnostics.size - invalid.size).toString())
    }
    if (invalid.isNotEmpty()) {
        val reasons = invalid.groupingBy { it.reason }.eachCount()
            .entries.joinToString { (reason, count) ->
                val text = placementReasonText(reason)
                if (count == 1) text else "$text ($count)"
            }
        rows += DebugDiagnosticRow("Reason", reasons)
    }
    return rows
}

internal fun pathfindingDiagnosticRows(
    waypoints: List<TilePosition>,
    selectedEntity: String?,
    result: PathfindingDiagnosticResult?
): List<DebugDiagnosticRow> = when {
    result == null && waypoints.isNotEmpty() -> listOf(
        DebugDiagnosticRow("Mode", selectedEntity?.let { "Entity: $it" } ?: "Standalone"),
        DebugDiagnosticRow("Waypoints", waypoints.size.toString()),
        DebugDiagnosticRow("Next", "Click the next waypoint")
    )
    result == null -> listOf(
        DebugDiagnosticRow("Status", "Click an entity or start tile")
    )
    else -> listOf(
        DebugDiagnosticRow("Mode", selectedEntity?.let { "Entity: $it" } ?: "Standalone"),
        DebugDiagnosticRow("Waypoints", result.waypoints.size.toString()),
        DebugDiagnosticRow(
            "Start",
            debugDiagnosticValue(result.start?.let(::formatTilePosition))
        ),
        DebugDiagnosticRow(
            "Goal",
            debugDiagnosticValue(result.goal?.let(::formatTilePosition))
        ),
        DebugDiagnosticRow("Result", when (result.status) {
            PathfindingSearchStatus.READY, PathfindingSearchStatus.RUNNING -> "Pending"
            PathfindingSearchStatus.SUCCEEDED -> "Success"
            PathfindingSearchStatus.FAILED -> "No path"
        }),
        DebugDiagnosticRow("Search state", when (result.status) {
            PathfindingSearchStatus.READY -> "Ready"
            PathfindingSearchStatus.RUNNING -> "Running"
            PathfindingSearchStatus.SUCCEEDED -> "Goal reached"
            PathfindingSearchStatus.FAILED -> "No path"
        }),
        DebugDiagnosticRow("Path length", (result.path?.size ?: 0).toString()),
        DebugDiagnosticRow(
            "Total cost",
            debugDiagnosticValue(
                result.totalCost?.let { String.format(Locale.ROOT, "%.2f", it) }
            )
        ),
        DebugDiagnosticRow("Explored", result.explored.size.toString()),
        DebugDiagnosticRow("Open set", result.openSetSize.toString()),
        DebugDiagnosticRow("Closed set", result.closedSetSize.toString()),
        DebugDiagnosticRow("Goal reached", if (result.goalReached) "Yes" else "No"),
        DebugDiagnosticRow(
            "Search time",
            String.format(Locale.ROOT, "%.2f ms", result.durationNanos / 1_000_000.0)
        ),
        DebugDiagnosticRow("Path", formatTilePositions(result.path.orEmpty()))
    )
}

internal fun placementReasonText(reason: PlacementFailureReason?): String = when (reason) {
    PlacementFailureReason.FOOTPRINT_OUTSIDE_WORLD -> "Footprint outside world"
    PlacementFailureReason.OCCUPIED_TILE -> "Occupied tile"
    PlacementFailureReason.RESERVED_TILE_CONFLICT -> "Reserved by another preview"
    PlacementFailureReason.EXTERNAL_VALIDATOR_REJECTED -> "External validator rejected placement"
    null -> "Unknown reason"
}
