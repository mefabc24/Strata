package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.placement.PlacementDiagnostic
import com.mefabc24.strata.placement.PlacementFailureReason
import com.mefabc24.strata.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugContextStatusTest {
    @Test
    fun `none mode hides contextual status`() {
        assertNull(debugContextStatus(DebugContextInputs(DebugToolMode.NONE)))
    }

    @Test
    fun `build status reports selection preview and drag state`() {
        val hover = requireNotNull(debugContextStatus(DebugContextInputs(
            mode = DebugToolMode.BUILD,
            buildObject = "House"
        )))
        assertEquals("House", hover.rows.first { it.key == "Object" }.value)
        assertEquals("Hover a tile to preview", hover.rows.first { it.key == "Status" }.value)

        val invalidDrag = requireNotNull(debugContextStatus(DebugContextInputs(
            mode = DebugToolMode.BUILD,
            buildObject = "House",
            placementDiagnostic = PlacementDiagnostic(
                false,
                PlacementFailureReason.OCCUPIED_TILE
            ),
            buildDragging = true,
            buildPreviewCount = 3
        )))
        assertEquals(
            "Dragging 3 placements — invalid: occupied tile",
            invalidDrag.rows.first { it.key == "Status" }.value
        )

        val valid = requireNotNull(debugContextStatus(DebugContextInputs(
            mode = DebugToolMode.BUILD,
            buildObject = "House",
            placementDiagnostic = PlacementDiagnostic(true)
        )))
        assertEquals("Placement valid", valid.rows.first { it.key == "Status" }.value)
    }

    @Test
    fun `paint spawn and inspect modes expose concise context`() {
        val paint = requireNotNull(debugContextStatus(DebugContextInputs(
            DebugToolMode.PAINT,
            paintTerrain = "Low grass",
            paintLayer = "Ground"
        )))
        assertEquals(listOf("Terrain", "Layer"), paint.rows.map { it.key })

        val spawn = requireNotNull(debugContextStatus(DebugContextInputs(
            DebugToolMode.SPAWN,
            spawnEntity = "Boar"
        )))
        assertEquals("Boar", spawn.rows.single().value)

        val inspect = requireNotNull(debugContextStatus(DebugContextInputs(
            DebugToolMode.INSPECT,
            inspection = "House (Object)"
        )))
        assertEquals("House (Object)", inspect.rows.single().value)
    }

    @Test
    fun `delete mode describes removable targets`() {
        val delete = requireNotNull(debugContextStatus(DebugContextInputs(DebugToolMode.DELETE)))

        assertEquals("Click an entity, object, or terrain overlay", delete.rows.single().value)
    }

    @Test
    fun `path mode summarizes waiting and completed searches`() {
        val start = TilePosition(1, 1)
        val waiting = requireNotNull(debugContextStatus(DebugContextInputs(
            DebugToolMode.PATHFINDING,
            pathWaypoints = listOf(start)
        )))
        assertTrue(waiting.rows.any { it.value == "Click the next waypoint" })

        val result = PathfindingDiagnosticResult(
            start,
            TilePosition(2, 1),
            listOf(start, TilePosition(2, 1)),
            listOf(start),
            10L
        )
        val complete = requireNotNull(debugContextStatus(DebugContextInputs(
            DebugToolMode.PATHFINDING,
            pathWaypoints = result.waypoints,
            pathResult = result
        )))
        assertEquals("Success", complete.rows.first { it.key == "Status" }.value)
        assertEquals("2 tiles, cost 1.00", complete.rows.first { it.key == "Path" }.value)

        val held = requireNotNull(debugContextStatus(DebugContextInputs(
            DebugToolMode.PATHFINDING,
            pathEntity = "Wolf",
            pathEntityWaiting = true,
            pathWaypoints = listOf(TilePosition(2, 1)),
            pathResult = result.copy(
                start = TilePosition(2, 1),
                path = listOf(TilePosition(2, 1)),
                waypoints = listOf(TilePosition(2, 1)),
                totalCost = 0f
            )
        )))
        assertEquals("Waiting at destination", held.rows.first { it.key == "Status" }.value)
    }

    @Test
    fun `status footer reserves one footprint for changing tool diagnostics`() {
        val singleRow = requireNotNull(debugContextStatus(DebugContextInputs(
            mode = DebugToolMode.INSPECT,
            inspection = "A very long selected entity name that wraps in the footer"
        )))
        val fourRows = requireNotNull(debugContextStatus(DebugContextInputs(
            mode = DebugToolMode.PATHFINDING,
            pathWaypoints = listOf(TilePosition(1, 1), TilePosition(12, 8)),
            pathResult = PathfindingDiagnosticResult(
                TilePosition(1, 1),
                TilePosition(12, 8),
                listOf(TilePosition(1, 1), TilePosition(12, 8)),
                emptyList(),
                0L
            )
        )))

        assertEquals(1, singleRow.rows.size)
        assertEquals(4, fourRows.rows.size)
        assertTrue(DebugContextFooterLayout.reservedHeight > 0f)
    }
}
