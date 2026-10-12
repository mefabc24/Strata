package com.mefabc24.strata.placement

import com.mefabc24.strata.world.TilePosition
import kotlin.test.*

class TilePathSelectionTest {
    private val a = TilePosition(0, 0)
    private val b = TilePosition(3, 1)
    private val c = TilePosition(4, 4)
    private val d = TilePosition(1, 5)

    @Test
    fun `two point selection previews and finishes the same path then returns idle`() {
        val selection = TilePathSelection()
        assertFalse(selection.active)
        assertFalse(selection.previewTo(b))
        assertFalse(selection.addWaypoint(b))
        assertTrue(selection.finish().isEmpty())
        selection.start(a)
        assertTrue(selection.active)
        assertEquals(listOf(a), selection.positions)
        selection.previewTo(b)
        val preview = selection.positions
        assertEquals(DirectTilePathResolver.resolve(a, b), preview)
        assertEquals(preview, selection.finish())
        assertFalse(selection.active)
        assertTrue(selection.waypoints.isEmpty())
        assertTrue(selection.positions.isEmpty())
        selection.start(c)
        assertEquals(DirectTilePathResolver.resolve(c, d), selection.finish(d))
    }

    @Test
    fun `waypoints concatenate ordered segments with one shared junction`() {
        val selection = TilePathSelection()
        selection.start(a)
        selection.addWaypoint(b)
        selection.addWaypoint(c)
        selection.previewTo(d)
        val expected = DirectTilePathResolver.resolve(a, b) +
            DirectTilePathResolver.resolve(b, c).drop(1) +
            DirectTilePathResolver.resolve(c, d).drop(1)
        assertEquals(listOf(a, b, c), selection.waypoints)
        assertEquals(expected, selection.positions)
        assertEquals(1, selection.positions.count { it == b })
        assertEquals(1, selection.positions.count { it == c })
        assertEquals(expected, selection.finish())
    }

    @Test
    fun `endpoint changes only resolve the active segment and confirmed tiles stay intact`() {
        val calls = mutableListOf<Pair<TilePosition, TilePosition>>()
        val selection = TilePathSelection { start, end ->
            calls += start to end
            DirectTilePathResolver.resolve(start, end)
        }
        selection.start(a)
        selection.previewTo(b)
        selection.addWaypoint(b)
        val confirmed = selection.positions
        val waypoints = selection.waypoints
        selection.previewTo(c)
        selection.previewTo(c)
        selection.previewTo(d)
        assertEquals(listOf(a to b, b to c, b to d), calls)
        assertEquals(confirmed, selection.positions.take(confirmed.size))
        assertEquals(listOf(a, b), waypoints)
        selection.previewTo(null)
        assertEquals(confirmed, selection.positions)
        selection.addWaypoint(c)
        assertEquals(listOf(a, b), waypoints)
        assertEquals(confirmed, DirectTilePathResolver.resolve(a, b))
    }

    @Test
    fun `repeated waypoint does not duplicate tiles but revisits preserve connectivity`() {
        val selection = TilePathSelection()
        selection.start(a)
        selection.addWaypoint(b)
        selection.addWaypoint(b)
        assertEquals(listOf(a, b), selection.waypoints)
        selection.addWaypoint(a)
        assertEquals(DirectTilePathResolver.resolve(a, b) +
            DirectTilePathResolver.resolve(b, a).drop(1), selection.positions)
        assertTrue(selection.positions.zipWithNext().all { (a, b) -> a != b })
    }

    @Test
    fun `cancel clear and starting again discard all segments`() {
        val selection = TilePathSelection()
        selection.start(a)
        selection.addWaypoint(b)
        selection.previewTo(c)
        assertTrue(selection.cancel())
        assertFalse(selection.cancel())
        assertFalse(selection.active)
        assertTrue(selection.positions.isEmpty())
        selection.start(c)
        selection.addWaypoint(d)
        selection.start(a)
        assertEquals(listOf(a), selection.waypoints)
        assertEquals(listOf(a), selection.positions)
        assertTrue(selection.clear())
        assertTrue(selection.positions.isEmpty())
    }

    @Test
    fun `selection supports hundreds of waypoints`() {
        val selection = TilePathSelection()
        selection.start(a)
        for (x in 1..300) selection.addWaypoint(TilePosition(x, 0))
        assertEquals(301, selection.waypoints.size)
        assertEquals((0..300).map { TilePosition(it, 0) }, selection.finish())
    }

    @Test
    fun `resolver output is copied and malformed segments fail without corrupting state`() {
        val output = mutableListOf(a, TilePosition(1, 0), TilePosition(2, 0))
        val selection = TilePathSelection { _, _ -> output }
        selection.start(a)
        selection.previewTo(TilePosition(2, 0))
        val snapshot = selection.positions
        output.clear()
        assertEquals(snapshot, selection.positions)
        assertFailsWith<IllegalArgumentException> { selection.previewTo(b) }
        assertEquals(snapshot, selection.positions)
        output += listOf(a, a, b)
        assertFailsWith<IllegalArgumentException> { selection.previewTo(b) }
        assertEquals(snapshot, selection.positions)
    }
}
