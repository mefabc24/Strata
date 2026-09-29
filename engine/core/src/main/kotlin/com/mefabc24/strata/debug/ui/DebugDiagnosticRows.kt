package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.placement.PlacementDiagnostic
import com.mefabc24.strata.placement.PlacementFailureReason
import com.mefabc24.strata.debug.inspector.formatTilePosition
import com.mefabc24.strata.debug.inspector.formatTilePositions
import com.mefabc24.strata.world.TilePosition
import java.util.Locale

data class DebugDiagnosticRow(val key: String, val value: String)

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

    var currentRows: List<DebugDiagnosticRow> = emptyList()
        private set

    init {
        top().left()
        touchable = Touchable.disabled
        isVisible = false
    }

    fun show(rows: List<DebugDiagnosticRow>) {
        currentRows = rows
        isVisible = rows.isNotEmpty()
        if (rows.isEmpty()) return
        val newKeys = rows.map(DebugDiagnosticRow::key)
        if (newKeys != keys) rebuild(newKeys)
        rows.forEachIndexed { index, row -> valueLabels[index].setText(row.value) }
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
    start: TilePosition?,
    result: PathfindingDiagnosticResult?
): List<DebugDiagnosticRow> = when {
    start != null -> listOf(
        DebugDiagnosticRow("Start", formatTilePosition(start)),
        DebugDiagnosticRow("Next", "Click a goal tile")
    )
    result == null -> listOf(
        DebugDiagnosticRow("Status", "Click a start tile, then a goal tile")
    )
    else -> listOf(
        DebugDiagnosticRow("Start", formatTilePosition(result.start)),
        DebugDiagnosticRow("Goal", formatTilePosition(result.goal)),
        DebugDiagnosticRow("Result", if (result.success) "Success" else "No path"),
        DebugDiagnosticRow("Path length", (result.path?.size ?: 0).toString()),
        DebugDiagnosticRow("Explored", result.explored.size.toString()),
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
