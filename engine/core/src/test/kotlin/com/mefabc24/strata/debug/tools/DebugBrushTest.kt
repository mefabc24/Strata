package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugBrushTest {
    @Test
    fun `brush previews omit tile borders by default`() {
        val preview = DebugBrushPreview(
            DebugBrushPreviewKind.PAINT,
            listOf(TilePosition(2, 2))
        )

        assertFalse(preview.showTileBorders)
        assertTrue(preview.copy(showTileBorders = true).showTileBorders)
    }

    @Test
    fun `odd square brushes use consistent centered geometry`() {
        assertEquals(
            listOf(TilePosition(2, 2)),
            DebugBrush.tiles(TilePosition(2, 2), 1, 5, 5)
        )
        assertEquals(
            (1..3).flatMap { y -> (1..3).map { x -> TilePosition(x, y) } },
            DebugBrush.tiles(TilePosition(2, 2), 3, 5, 5)
        )
    }

    @Test
    fun `brush geometry clips exactly at world boundaries`() {
        assertEquals(
            (0..2).flatMap { y -> (0..2).map { x -> TilePosition(x, y) } },
            DebugBrush.tiles(TilePosition(0, 0), 5, 4, 4)
        )
        assertTrue(DebugBrush.tiles(TilePosition(-5, -5), 3, 4, 4).isEmpty())
    }

    @Test
    fun `stroke fills fast drag gaps and processes each tile once`() {
        val stroke = DebugBrushStroke()
        val visited = mutableListOf<TilePosition>()

        stroke.begin(TilePosition(0, 1), 3, 8, 3, visited::add)
        assertTrue(stroke.drag(TilePosition(7, 1), 3, 8, 3, visited::add))
        assertEquals(24, visited.size)
        assertEquals(visited.toSet().size, visited.size)
        assertEquals(
            (0..2).flatMap { y -> (0..7).map { x -> TilePosition(x, y) } }.toSet(),
            visited.toSet()
        )

        assertTrue(stroke.drag(TilePosition(7, 1), 3, 8, 3, visited::add))
        assertEquals(24, visited.size)
        stroke.cancel()
        assertFalse(stroke.drag(TilePosition(0, 0), 3, 8, 3, visited::add))
    }
}
