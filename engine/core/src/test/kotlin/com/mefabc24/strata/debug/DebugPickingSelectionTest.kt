package com.mefabc24.strata.debug

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugPickingSelectionTest {
    private data object TestTile : Tile
    private data object TestEntity : Entity
    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `clicks lock objects entities and tiles and replace the previous target`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val selection = DebugPickingSelection()

        assertTrue(selection.selectFromClick(PickedTarget.Object(placed, Rectangle())))
        assertSame(
            placed,
            assertIs<PickedTarget.Object>(selection.lockedTarget).placedObject
        )

        assertTrue(selection.selectFromClick(PickedTarget.Entity(entity, Rectangle())))
        assertSame(
            entity,
            assertIs<PickedTarget.Entity>(selection.lockedTarget).worldEntity
        )

        assertTrue(selection.selectFromClick(PickedTarget.Tile(TilePosition(0, 0))))
        assertEquals(
            TilePosition(0, 0),
            assertIs<PickedTarget.Tile>(selection.lockedTarget).position
        )
    }

    @Test
    fun `empty clicks and hover do not replace a locked target`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val locked = PickedTarget.Object(placed, Rectangle())
        val hover = PickedTarget.Entity(entity, Rectangle())
        val selection = DebugPickingSelection()
        selection.selectFromClick(locked)

        assertFalse(selection.selectFromClick(null))
        assertSame(locked, selection.lockedTarget)
        assertSame(locked, selection.displayedTarget(hover))
        assertSame(DebugPickingTargetMode.LOCKED, selection.mode())
    }

    @Test
    fun `hover remains the displayed target until a click locks one`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val hover = PickedTarget.Entity(entity, Rectangle())
        val selection = DebugPickingSelection()

        assertSame(hover, selection.displayedTarget(hover))
        assertSame(DebugPickingTargetMode.HOVER, selection.mode())
        assertNull(selection.lockedTarget)
    }

    @Test
    fun `disabling picking clears the locked target`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val selection = DebugPickingSelection()
        selection.selectFromClick(PickedTarget.Object(placed, Rectangle()))

        selection.syncEnabled(false)

        assertNull(selection.lockedTarget)
        assertSame(DebugPickingTargetMode.HOVER, selection.mode())
    }

    @Test
    fun `enabled picking retains selection without configured child visuals`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val settings = DebugPickingSettings().apply { enabled = true }
        val selection = DebugPickingSelection()
        selection.selectFromClick(PickedTarget.Object(placed, Rectangle()))

        selection.syncEnabled(settings.enabled)

        assertFalse(settings.hasConfiguredVisuals)
        assertFalse(settings.hasActiveVisuals)
        assertSame(placed, assertIs<PickedTarget.Object>(selection.lockedTarget).placedObject)
    }

    @Test
    fun `refresh keeps a moving entity locked while updating its bounds`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val selection = DebugPickingSelection()
        selection.selectFromClick(PickedTarget.Entity(entity, Rectangle()))
        val movedBounds = Rectangle(10f, 20f, 8f, 12f)

        selection.refresh { target ->
            (target as PickedTarget.Entity).copy(bounds = movedBounds)
        }

        assertSame(
            entity,
            assertIs<PickedTarget.Entity>(selection.lockedTarget).worldEntity
        )
        assertEquals(movedBounds, selection.lockedTarget?.bounds)
    }
}
