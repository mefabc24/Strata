package com.mefabc24.strata.debug

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.PickedSpriteTarget
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
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
    fun `clicks lock objects and entities and replace the previous target`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val selection = DebugPickingSelection()

        assertTrue(selection.selectFromClick(PickedSpriteTarget.Object(placed, Rectangle())))
        assertSame(
            placed,
            assertIs<PickedSpriteTarget.Object>(selection.lockedTarget).placedObject
        )

        assertTrue(selection.selectFromClick(PickedSpriteTarget.Entity(entity, Rectangle())))
        assertSame(
            entity,
            assertIs<PickedSpriteTarget.Entity>(selection.lockedTarget).worldEntity
        )
    }

    @Test
    fun `empty clicks and hover do not replace a locked target`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val locked = PickedSpriteTarget.Object(placed, Rectangle())
        val hover = PickedSpriteTarget.Entity(entity, Rectangle())
        val selection = DebugPickingSelection()
        selection.selectFromClick(locked)

        assertFalse(selection.selectFromClick(null))
        assertSame(locked, selection.lockedTarget)
        assertSame(locked, selection.displayedTarget(hover))
        assertSame(DebugPickingTargetMode.LOCKED, selection.mode(hover))
    }

    @Test
    fun `hover remains the displayed target until a click locks one`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val hover = PickedSpriteTarget.Entity(entity, Rectangle())
        val selection = DebugPickingSelection()

        assertSame(hover, selection.displayedTarget(hover))
        assertSame(DebugPickingTargetMode.HOVER, selection.mode(hover))
        assertNull(selection.lockedTarget)
    }

    @Test
    fun `disabling picking clears the locked target`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val selection = DebugPickingSelection()
        selection.selectFromClick(PickedSpriteTarget.Object(placed, Rectangle()))

        selection.syncEnabled(false)

        assertNull(selection.lockedTarget)
        assertSame(DebugPickingTargetMode.NONE, selection.mode(null))
    }

    @Test
    fun `refresh keeps a moving entity locked while updating its bounds`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val selection = DebugPickingSelection()
        selection.selectFromClick(PickedSpriteTarget.Entity(entity, Rectangle()))
        val movedBounds = Rectangle(10f, 20f, 8f, 12f)

        selection.refresh { target ->
            (target as PickedSpriteTarget.Entity).copy(bounds = movedBounds)
        }

        assertSame(
            entity,
            assertIs<PickedSpriteTarget.Entity>(selection.lockedTarget).worldEntity
        )
        assertEquals(movedBounds, selection.lockedTarget?.bounds)
    }
}
