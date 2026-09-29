package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.mefabc24.strata.pathfinding.PathfindingDiagnosticResult
import com.mefabc24.strata.placement.PlacementDiagnostic
import com.mefabc24.strata.placement.PlacementFailureReason
import com.mefabc24.strata.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugDiagnosticRowsTest {
    @Test
    fun `diagnostic rows retain separate keys and values`() {
        assertEquals(
            listOf(DebugDiagnosticRow("Screen", "10, 20")),
            diagnosticRows("Screen" to "10, 20")
        )
    }

    @Test
    fun `placement diagnostics explain the current failure`() {
        val rows = placementDiagnosticRows(
            placementAvailable = true,
            diagnostics = listOf(
                PlacementDiagnostic(
                    valid = false,
                    reason = PlacementFailureReason.RESERVED_TILE_CONFLICT
                )
            )
        )

        assertEquals("Invalid", rows.first { it.key == "Status" }.value)
        assertEquals("Reserved by another preview", rows.first { it.key == "Reason" }.value)
    }

    @Test
    fun `pathfinding rows describe the tool workflow and result`() {
        val start = TilePosition(1, 2)
        val waiting = pathfindingDiagnosticRows(start, null)
        assertEquals("Click a goal tile", waiting.first { it.key == "Next" }.value)

        val result = PathfindingDiagnosticResult(
            start = start,
            goal = TilePosition(3, 2),
            path = listOf(start, TilePosition(2, 2), TilePosition(3, 2)),
            explored = listOf(start, TilePosition(2, 2)),
            durationNanos = 1_500_000
        )
        val rows = pathfindingDiagnosticRows(null, result)

        assertEquals("Success", rows.first { it.key == "Result" }.value)
        assertEquals("3", rows.first { it.key == "Path length" }.value)
        assertEquals("2", rows.first { it.key == "Explored" }.value)
        assertEquals("1.50 ms", rows.first { it.key == "Search time" }.value)
    }

    @Test
    fun `hidden diagnostic table collapses its layout cell`() {
        val skin = diagnosticSkin()
        val table = DebugDiagnosticTable(skin)
        val parent = Table(skin)
        val cell = parent.add(table).pad(3f).space(8f)
        table.bindLayout(cell)

        table.show(diagnosticRows("Mode" to "Hover"))
        assertEquals(DebugDiagnosticLayoutState.EXPANDED, table.layoutState)

        table.show(emptyList())

        assertEquals(DebugDiagnosticLayoutState.COLLAPSED, table.layoutState)
        assertEquals(0f, cell.minHeight)
        assertEquals(0f, cell.prefHeight)
        assertEquals(0f, cell.maxHeight)
        assertEquals(0f, cell.padTop)
        assertEquals(0f, cell.spaceTop)

        table.show(diagnosticRows("Mode" to "Locked"))
        assertEquals(DebugDiagnosticLayoutState.EXPANDED, table.layoutState)
        assertEquals(3f, cell.padTop)
        assertEquals(8f, cell.spaceTop)
        skin.dispose()
    }

    private fun diagnosticSkin(): Skin {
        val font = BitmapFont(
            BitmapFont.BitmapFontData(),
            Array<TextureRegion>().apply { add(TextureRegion()) },
            false
        )
        return Skin().apply {
            add("font", font)
            add("default", Label.LabelStyle(font, null))
            add("debug-key", Label.LabelStyle(font, null))
        }
    }
}
