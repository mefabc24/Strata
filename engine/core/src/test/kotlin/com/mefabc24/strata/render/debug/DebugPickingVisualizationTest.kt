package com.mefabc24.strata.render.debug

import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DebugPickingVisualizationTest {
    @Test
    fun `locked picking keeps an independent hover target`() {
        val hover = PickedTarget.Tile(TilePosition(1, 2))
        val locked = PickedTarget.Tile(TilePosition(3, 4))

        val visuals = pickingVisualTargets(true, hover, locked)

        assertSame(hover, visuals.hover)
        assertSame(locked, visuals.locked)
    }

    @Test
    fun `tile hover and lock visualization preserve both positions`() {
        val visuals = pickingVisualTargets(
            enabled = true,
            hover = PickedTarget.Tile(TilePosition(0, 1)),
            locked = PickedTarget.Tile(TilePosition(2, 3))
        )

        assertEquals(TilePosition(0, 1), (visuals.hover as PickedTarget.Tile).position)
        assertEquals(TilePosition(2, 3), (visuals.locked as PickedTarget.Tile).position)
    }
}
