package com.mefabc24.strata.debug

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DebugPickingDiagnosticsTest {
    private data object TestTile : Tile
    private data object TestEntity : Entity
    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    private val projection = IsoProjection()

    @Test
    fun `locked tile diagnostics use the locked tile instead of the cursor`() {
        val world = World(3, 3) { _, _ -> TestTile }
        val lockedTile = TilePosition(1, 2)
        val placed = requireNotNull(world.place(TestObject(), lockedTile.x, lockedTile.y))
        val entity = world.addEntity(TestEntity, EntityPosition(1.5f, 2.5f))
        val selection = DebugPickingSelection().apply {
            selectFromClick(PickedTarget.Tile(lockedTile))
        }

        val diagnostics = resolve(selection, world)
        val expectedWorld = projection.tileToWorld(1.5f, 2.5f)

        assertEquals(DebugPickingTargetMode.LOCKED, diagnostics.mode)
        assertEquals(lockedTile, diagnostics.tile)
        assertEquals(lockedTile, diagnostics.grid)
        assertEquals(expectedWorld, diagnostics.world)
        assertEquals(projectScreen(expectedWorld), diagnostics.screen)
        assertSame(placed, diagnostics.placedObject)
        assertEquals(listOf(entity), diagnostics.entities)
    }

    @Test
    fun `locked object diagnostics do not use cursor state`() {
        val world = World(3, 3) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 2, 1))
        val bounds = Rectangle(2f, 3f, 4f, 5f)
        val selection = DebugPickingSelection().apply {
            selectFromClick(PickedTarget.Object(placed, bounds, true))
        }

        val diagnostics = resolve(selection, world)
        val expectedTile = TilePosition(2, 1)
        val expectedWorld = projection.surfaceAnchor(2, 1)

        assertEquals(expectedTile, diagnostics.tile)
        assertEquals(expectedTile, diagnostics.grid)
        assertEquals(expectedWorld, diagnostics.world)
        assertEquals(projectScreen(expectedWorld), diagnostics.screen)
        assertSame(placed, diagnostics.placedObject)
        assertEquals(true, diagnostics.alphaAccepted)
        assertEquals(bounds, diagnostics.bounds)
    }

    @Test
    fun `locked entity diagnostics follow the moving entity position`() {
        val world = World(3, 3) { _, _ -> TestTile }
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val selection = DebugPickingSelection().apply {
            selectFromClick(PickedTarget.Entity(entity, Rectangle()))
        }

        entity.teleport(EntityPosition(2.25f, 1.75f))
        val diagnostics = resolve(selection, world)
        val expectedWorld = projection.tileToWorld(2.25f, 1.75f)

        assertEquals(entity.currentTile, diagnostics.tile)
        assertEquals(entity.currentTile, diagnostics.grid)
        assertEquals(expectedWorld, diagnostics.world)
        assertEquals(projectScreen(expectedWorld), diagnostics.screen)
        assertEquals(listOf(entity), diagnostics.entities)
    }

    private fun resolve(
        selection: DebugPickingSelection,
        world: World
    ): DebugPickingDiagnostics = resolvePickingDiagnostics(
        selection = selection,
        hoverTarget = PickedTarget.Tile(TilePosition(0, 0)),
        cursorScreen = Vector2(900f, 700f),
        cursorWorld = Vector2(800f, 600f),
        cursorGrid = TilePosition(0, 0),
        cursorTile = TilePosition(0, 0),
        world = world,
        tileCenterWorld = { projection.tileToWorld(it.x + 0.5f, it.y + 0.5f) },
        objectOriginWorld = { projection.surfaceAnchor(it.x, it.y) },
        entityWorld = { projection.tileToWorld(it.position.x, it.position.y) },
        worldToScreen = ::projectScreen
    )

    private fun projectScreen(world: Vector2): Vector2 =
        Vector2(world.x + 100f, world.y + 200f)
}
