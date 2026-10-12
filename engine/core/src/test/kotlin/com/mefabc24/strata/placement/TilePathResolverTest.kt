package com.mefabc24.strata.placement

import com.mefabc24.strata.world.TilePosition
import kotlin.math.abs
import kotlin.test.*

class TilePathResolverTest {
    private val resolver = DirectTilePathResolver

    @Test
    fun `grid axes and diagonals include every tile`() {
        for ((dx, dy) in listOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)) {
            val expected = (0..4).map { TilePosition(2 + it * dx, 3 + it * dy) }
            assertEquals(expected, resolver.resolve(expected.first(), expected.last()))
            assertEquals(expected.reversed(), resolver.resolve(expected.last(), expected.first()))
        }
    }

    @Test
    fun `mixed slopes rasterize direct chains instead of rectangles`() {
        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(1, 0), TilePosition(2, 1),
                TilePosition(3, 1), TilePosition(4, 2), TilePosition(5, 2)),
            resolver.resolve(TilePosition(0, 0), TilePosition(5, 2))
        )
        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(0, 1), TilePosition(1, 2),
                TilePosition(1, 3), TilePosition(2, 4), TilePosition(2, 5)),
            resolver.resolve(TilePosition(0, 0), TilePosition(2, 5))
        )
    }

    @Test
    fun `same endpoints select one tile`() {
        val point = TilePosition(-4, 8)
        assertEquals(listOf(point), resolver.resolve(point, point))
    }

    @Test
    fun `all octants ties and translations are deterministic connected and reversible`() {
        for (start in listOf(TilePosition(0, 0), TilePosition(-6, 9))) {
            for (dx in -12..12) for (dy in -12..12) {
                val end = TilePosition(start.x + dx, start.y + dy)
                val path = resolver.resolve(start, end)
                assertEquals(start, path.first())
                assertEquals(end, path.last())
                assertEquals(maxOf(abs(dx), abs(dy)) + 1, path.size)
                assertEquals(path.size, path.toSet().size)
                assertEquals(path, resolver.resolve(start, end))
                assertEquals(path.reversed(), resolver.resolve(end, start))
                path.zipWithNext().forEach { (a, b) ->
                    assertNotEquals(a, b)
                    assertTrue(abs(a.x - b.x) <= 1 && abs(a.y - b.y) <= 1)
                }
            }
        }
    }

    @Test
    fun `coordinates near integer limits do not overflow`() {
        for (base in listOf(Int.MIN_VALUE, Int.MAX_VALUE - 3)) {
            val expected = (0..3).map { TilePosition(base + it, base + it) }
            assertEquals(expected, resolver.resolve(expected.first(), expected.last()))
            assertEquals(expected.reversed(), resolver.resolve(expected.last(), expected.first()))
        }
        assertFailsWith<IllegalArgumentException> {
            resolver.resolve(TilePosition(Int.MIN_VALUE, 0), TilePosition(Int.MAX_VALUE, 0))
        }
    }
}
