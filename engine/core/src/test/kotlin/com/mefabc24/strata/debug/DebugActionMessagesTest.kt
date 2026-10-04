package com.mefabc24.strata.debug

import com.mefabc24.strata.placement.PlacementFailureReason
import com.mefabc24.strata.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugActionMessagesTest {
    @Test
    fun `discrete action summaries are concise`() {
        assertEquals(
            "Wolf spawned at (3, 19)",
            DebugActionMessages.spawned("Wolf", TilePosition(3, 19))
        )
        assertEquals("Placed 1 object", DebugActionMessages.placedObjects(1))
        assertEquals("Placed 8 objects", DebugActionMessages.placedObjects(8))
        assertEquals("Path found: 12 tiles", DebugActionMessages.pathFound(12))
    }

    @Test
    fun `placement rejection includes useful reason`() {
        assertEquals(
            "Object placement rejected: occupied tile",
            DebugActionMessages.placementRejected(PlacementFailureReason.OCCUPIED_TILE)
        )
        assertEquals(
            "Object placement rejected: outside world",
            DebugActionMessages.placementRejected(
                PlacementFailureReason.FOOTPRINT_OUTSIDE_WORLD
            )
        )
    }
}
