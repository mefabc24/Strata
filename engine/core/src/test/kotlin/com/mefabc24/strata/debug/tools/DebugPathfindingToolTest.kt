package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugPathfindingToolTest {
    private data object TestTile : Tile

    @Test
    fun `two clicks produce diagnostic path state`() {
        val state = DebugWorldState()
        val tool = DebugPathfindingTool(World(3, 3) { _, _ -> TestTile }, state)
        assertTrue(tool.click(TilePosition(0, 0)))
        assertEquals(TilePosition(0, 0), tool.start)
        assertTrue(tool.click(TilePosition(2, 0)))
        assertNull(tool.start)
        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(1, 0), TilePosition(2, 0)),
            tool.result?.path
        )
        assertTrue(tool.result!!.explored.isNotEmpty())
        assertTrue(tool.clear())
        assertNull(tool.result)
    }
}
