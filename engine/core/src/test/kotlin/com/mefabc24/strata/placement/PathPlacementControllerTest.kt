package com.mefabc24.strata.placement

import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.debug.tools.DebugBuildDragController
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.world.*
import kotlin.test.*

class PathPlacementControllerTest {
    private data object TestTile : Tile
    private class TestObject(override val footprint: Footprint = Footprint.square(1)) : Placeable

    @Test
    fun `live previews retain confirmed segments and finish uses exactly their logical origins`() {
        val placement = placement()
        val path = placement.path
        val a = TilePosition(1, 1)
        val b = TilePosition(4, 2)
        val c = TilePosition(4, 5)
        path.begin(a)
        path.addWaypoint(b)
        val confirmed = placement.previewPositions()
        path.update(c)
        placement.update(TilePosition(9, 9))
        assertEquals(confirmed, placement.previewPositions().take(confirmed.size))
        path.update(TilePosition(7, 4))
        assertEquals(confirmed, placement.previewPositions().take(confirmed.size))
        path.addWaypoint(c)
        path.update(TilePosition(6, 6))
        val expected = path.positions
        assertEquals(expected, placement.previewPositions())
        val placed = path.finish()
        assertEquals(expected, placed.map { TilePosition(it.x, it.y) })
        assertEquals(placed.size, placed.map { it.placeable }.toSet().size)
        assertTrue(placed.none { it.placeable === placement.selectedPlaceable })
        assertIdle(placement)
        assertTrue(path.begin(TilePosition(8, 8)))
        assertEquals(listOf(TilePosition(8, 8)), path.finish().map { TilePosition(it.x, it.y) })
        assertIdle(placement)
    }

    @Test
    fun `invalid origins are skipped consistently with preview occupancy and external validation`() {
        val world = world()
        world.place(TestObject(), 2, 1)
        val placement = placement(world, validator = { _, position -> position.x != 3 })
        placement.path.begin(TilePosition(1, 1))
        placement.path.update(TilePosition(5, 1))
        assertEquals(listOf(true, false, false, true, true), placement.previews.map { it.valid })
        assertEquals(listOf(null, PlacementFailureReason.OCCUPIED_TILE,
            PlacementFailureReason.EXTERNAL_VALIDATOR_REJECTED, null, null),
            placement.previewDiagnostics.map { it.reason })
        val expected = placement.validPreviewPositions()
        assertEquals(expected, placement.path.finish().map { TilePosition(it.x, it.y) })
    }

    @Test
    fun `bounds policies hide or show invalid footprint previews without changing placement`() {
        val expectedVisibility = mapOf(
            PlacementPreviewBoundsPolicy.ALL_TILES_INSIDE to listOf(0, 1, 2),
            PlacementPreviewBoundsPolicy.ORIGIN_INSIDE to listOf(0, 1, 2, 3),
            PlacementPreviewBoundsPolicy.ANY_TILE_INSIDE to listOf(-1, 0, 1, 2, 3),
            PlacementPreviewBoundsPolicy.ALWAYS to listOf(-2, -1, 0, 1, 2, 3, 4, 5)
        )
        expectedVisibility.forEach { (policy, xs) ->
            val placement = placement(World(4, 4) { _, _ -> TestTile }, policy,
                Footprint.rectangle(2, 1))
            placement.path.begin(TilePosition(-2, 1))
            placement.path.update(TilePosition(5, 1))
            assertEquals(xs.map { TilePosition(it, 1) }, placement.previewPositions(), policy.name)
            val expected = listOf(TilePosition(0, 1), TilePosition(2, 1))
            assertEquals(expected, placement.validPreviewPositions(), policy.name)
            assertEquals(expected, placement.path.finish().map { TilePosition(it.x, it.y) }, policy.name)
        }
    }

    @Test
    fun `custom footprint origin and overlaps use normal ordered reservations`() {
        val footprint = Footprint.custom(TileOffset(0, 0), TileOffset(1, 0),
            TileOffset(1, 1), origin = TileOffset(1, 1))
        val placement = placement(footprint = footprint)
        placement.path.begin(TilePosition(2, 2))
        placement.path.update(TilePosition(5, 2))
        assertEquals(listOf(true, false, true, false), placement.previews.map { it.valid })
        assertEquals(setOf(TilePosition(1, 1), TilePosition(2, 1), TilePosition(2, 2)),
            placement.previews.first().placedObject.occupiedTiles())
        val expected = placement.validPreviewPositions()
        assertEquals(expected, placement.path.finish().map { TilePosition(it.x, it.y) })
    }

    @Test
    fun `revisited origins are previewed and placed once without breaking logical path`() {
        val placement = placement()
        placement.path.begin(TilePosition(1, 1))
        placement.path.addWaypoint(TilePosition(4, 1))
        placement.path.addWaypoint(TilePosition(1, 1))
        assertEquals(7, placement.path.positions.size)
        assertEquals(4, placement.previews.size)
        assertEquals(4, placement.path.finish().size)
    }

    @Test
    fun `disabled previews preserve diagnostics validation and placement`() {
        val placement = PlacementController(world(), previewEnabled = false,
            placementValidator = { _, position -> position.x != 2 }).apply { selectedFactory = ::TestObject }
        placement.path.begin(TilePosition(1, 1))
        placement.path.update(TilePosition(3, 1))
        assertTrue(placement.previews.isEmpty())
        assertTrue(placement.previewDiagnostics.isEmpty())
        assertEquals(true, placement.currentDiagnostic?.valid)
        assertEquals(listOf(TilePosition(1, 1), TilePosition(3, 1)),
            placement.path.finish().map { TilePosition(it.x, it.y) })
    }

    @Test
    fun `preview colors and stationary endpoint validation use current world state`() {
        val world = world()
        val style = PlacementPreviewStyle(Color.BLUE, Color.ORANGE)
        val placement = PlacementController(world, style).apply { selectedFactory = ::TestObject }
        placement.path.begin(TilePosition(1, 1))
        placement.path.addWaypoint(TilePosition(3, 1))
        placement.path.update(TilePosition(5, 1))
        world.place(TestObject(), 2, 1)
        placement.path.update(TilePosition(5, 1))
        assertFalse(placement.previews[1].valid)
        assertEquals(style, placement.previews[1].style)
        val expected = placement.validPreviewPositions()
        assertEquals(expected, placement.path.finish().map { TilePosition(it.x, it.y) })
    }

    @Test
    fun `cancelling reset selection disable and replacement operations clear path immediately`() {
        val operations: List<(PlacementController) -> Unit> = listOf(
            { it.path.cancel() }, { it.path.clear() }, { it.clearPreviewPositions() },
            { it.selectedFactory = ::TestObject }, { it.enabled = false },
            { it.placeAt(TilePosition(8, 8)) }, { it.placeAt(listOf(TilePosition(8, 8))) },
            { it.path.resolver = DirectTilePathResolver }
        )
        operations.forEach { operation ->
            val placement = placement()
            placement.path.begin(TilePosition(1, 1))
            placement.path.addWaypoint(TilePosition(2, 1))
            placement.path.update(TilePosition(3, 1))
            operation(placement)
            assertIdle(placement)
            placement.enabled = true
            placement.update(TilePosition(4, 4))
            assertEquals(listOf(TilePosition(4, 4)), placement.previewPositions())
            assertTrue(placement.path.begin(TilePosition(5, 5)))
        }
    }

    @Test
    fun `explicit and rectangular previews supersede paths while single and rectangle placement still work`() {
        val placement = placement()
        placement.path.begin(TilePosition(1, 1))
        placement.previewAt(listOf(TilePosition(8, 8)))
        assertFalse(placement.path.active)
        assertEquals(listOf(TilePosition(8, 8)), placement.previewPositions())
        placement.path.begin(TilePosition(1, 1))
        assertNotNull(placement.placeAt(TilePosition(9, 9)))
        assertIdle(placement)
        placement.path.begin(TilePosition(1, 1))
        val drag = DebugBuildDragController(placement)
        drag.begin(TilePosition(4, 4))
        drag.dragTo(TilePosition(5, 5))
        assertFalse(placement.path.active)
        assertEquals(listOf(TilePosition(4, 4), TilePosition(5, 4),
            TilePosition(4, 5), TilePosition(5, 5)), placement.previewPositions())
        assertEquals(4, drag.finish(TilePosition(5, 5)).size)
        assertTrue(placement.previews.isEmpty())
    }

    @Test
    fun `finish endpoint update and new path replace unfinished state`() {
        val placement = placement()
        placement.path.begin(TilePosition(1, 1))
        placement.path.addWaypoint(TilePosition(3, 1))
        placement.path.begin(TilePosition(5, 5))
        assertEquals(listOf(TilePosition(5, 5)), placement.path.waypoints)
        assertEquals(listOf(TilePosition(5, 5), TilePosition(6, 6)),
            placement.path.finish(TilePosition(6, 6)).map { TilePosition(it.x, it.y) })
        assertIdle(placement)
    }

    @Test
    fun `unavailable and idle operations return cleanly`() {
        val placement = PlacementController(world())
        assertFalse(placement.path.begin(TilePosition(1, 1)))
        placement.selectedFactory = ::TestObject
        placement.enabled = false
        assertFalse(placement.path.begin(TilePosition(1, 1)))
        assertFalse(placement.path.update(TilePosition(2, 2)))
        assertFalse(placement.path.addWaypoint(TilePosition(2, 2)))
        assertTrue(placement.path.finish().isEmpty())
        assertFalse(placement.path.cancel())
        assertIdle(placement)
    }

    @Test
    fun `finish clears state even when an external validator throws`() {
        var throwing = false
        val placement = placement(validator = { _, _ -> check(!throwing); true })
        placement.path.begin(TilePosition(1, 1))
        throwing = true
        assertFailsWith<IllegalStateException> { placement.path.finish() }
        assertIdle(placement)
    }

    private fun world() = World(10, 10) { _, _ -> TestTile }
    private fun placement(
        world: World = world(),
        policy: PlacementPreviewBoundsPolicy = PlacementPreviewBoundsPolicy.ALL_TILES_INSIDE,
        footprint: Footprint = Footprint.square(1),
        validator: PlacementValidator = { _, _ -> true }
    ) = PlacementController(world, previewBoundsPolicy = policy, placementValidator = validator)
        .apply { selectedFactory = { TestObject(footprint) } }

    private fun PlacementController.previewPositions() = previews.map {
        TilePosition(it.placedObject.x, it.placedObject.y)
    }
    private fun PlacementController.validPreviewPositions() = previews.filter { it.valid }.map {
        TilePosition(it.placedObject.x, it.placedObject.y)
    }
    private fun assertIdle(placement: PlacementController) {
        assertFalse(placement.path.active)
        assertTrue(placement.path.positions.isEmpty())
        assertTrue(placement.path.waypoints.isEmpty())
        assertTrue(placement.previews.isEmpty())
        assertTrue(placement.previewDiagnostics.isEmpty())
        assertNull(placement.currentDiagnostic)
    }
}
