package com.mefabc24.strata.world

import kotlin.test.Test
import kotlin.test.assertEquals

class TileAreaTest {

    private val expected = listOf(
        TilePosition(1, 2),
        TilePosition(2, 2),
        TilePosition(3, 2),
        TilePosition(1, 3),
        TilePosition(2, 3),
        TilePosition(3, 3)
    )

    @Test
    fun `between includes endpoints in row-major order in every direction`() {
        val corners = listOf(
            TilePosition(1, 2) to TilePosition(3, 3),
            TilePosition(3, 2) to TilePosition(1, 3),
            TilePosition(1, 3) to TilePosition(3, 2),
            TilePosition(3, 3) to TilePosition(1, 2)
        )

        for ((start, end) in corners) {
            val area = TileArea.between(start, end)

            assertEquals(expected, area.positions)
            assertEquals(expected, area.toList())
            assertEquals(1, area.minX)
            assertEquals(2, area.minY)
            assertEquals(3, area.maxX)
            assertEquals(3, area.maxY)
        }
    }

    @Test
    fun `between one position creates a one-tile area`() {
        val position = TilePosition(4, 7)

        assertEquals(
            listOf(position),
            TileArea.between(position, position).positions
        )
    }
}
