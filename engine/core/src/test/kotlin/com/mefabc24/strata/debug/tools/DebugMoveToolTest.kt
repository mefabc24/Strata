package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.placement.PlacementEntityPreviewSettings
import com.mefabc24.strata.placement.PlacementObjectPreviewSettings
import com.mefabc24.strata.render.preview.EntityPreviewVisual
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugMoveToolTest {
    @Test
    fun `object move succeeds and updates occupancy version and identity`() {
        val world = world()
        val placed = requireNotNull(world.place(TestObject(Footprint.square(1)), 1, 1))
        val initialVersion = world.objectVersion
        val tool = tool(world)

        tool.begin(PickedTarget.Object(placed, null), TilePosition(1, 1))
        val outcome = tool.finish(TilePosition(3, 2))

        assertTrue(requireNotNull(outcome).success)
        assertSame(placed, world.getObjectAt(3, 2))
        assertNull(world.getObjectAt(1, 1))
        assertEquals(TilePosition(3, 2), TilePosition(placed.x, placed.y))
        assertEquals(initialVersion + 1, world.objectVersion)
    }

    @Test
    fun `object may overlap its own previous footprint`() {
        val world = world()
        val placed = requireNotNull(
            world.place(TestObject(Footprint.rectangle(2, 1)), 1, 1)
        )
        val tool = tool(world)

        tool.begin(PickedTarget.Object(placed, null), TilePosition(1, 1))
        assertTrue(requireNotNull(tool.preview).valid)
        val outcome = tool.finish(TilePosition(2, 1))

        assertTrue(requireNotNull(outcome).success)
        assertSame(placed, world.getObjectAt(2, 1))
        assertSame(placed, world.getObjectAt(3, 1))
        assertNull(world.getObjectAt(1, 1))
    }

    @Test
    fun `occupied object target is rejected atomically`() {
        val world = world()
        val moving = requireNotNull(world.place(TestObject(Footprint.square(1)), 1, 1))
        val blocker = requireNotNull(world.place(TestObject(Footprint.square(1)), 3, 3))
        val initialVersion = world.objectVersion
        val tool = tool(world)
        tool.begin(PickedTarget.Object(moving, null), TilePosition(1, 1))

        val outcome = tool.finish(TilePosition(3, 3))

        assertFalse(requireNotNull(outcome).success)
        assertSame(moving, world.getObjectAt(1, 1))
        assertSame(blocker, world.getObjectAt(3, 3))
        assertEquals(initialVersion, world.objectVersion)
    }

    @Test
    fun `out of world object target is rejected atomically`() {
        val world = world()
        val moving = requireNotNull(world.place(TestObject(Footprint.square(1)), 1, 1))
        val initialVersion = world.objectVersion
        val tool = tool(world)
        tool.begin(PickedTarget.Object(moving, null), TilePosition(1, 1))

        val outcome = tool.finish(TilePosition(-1, 1))

        assertFalse(requireNotNull(outcome).success)
        assertSame(moving, world.getObjectAt(1, 1))
        assertEquals(initialVersion, world.objectVersion)
    }

    @Test
    fun `entity move teleports same instance and cancels its route`() {
        val world = world()
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        entity.followPath(listOf(TilePosition(4, 4)), 1f)
        val tool = tool(world)
        tool.begin(PickedTarget.Entity(entity, null), entity.currentTile)

        val outcome = tool.finish(TilePosition(2, 3))

        assertTrue(requireNotNull(outcome).success)
        assertSame(entity, world.getEntities().single())
        assertEquals(EntityPosition(2.5f, 3.5f), entity.position)
        assertFalse(entity.isMoving)
    }

    @Test
    fun `entity move outside world does not commit`() {
        val world = world()
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val tool = tool(world)
        tool.begin(PickedTarget.Entity(entity, null), entity.currentTile)

        val outcome = tool.finish(TilePosition(9, 9))

        assertFalse(requireNotNull(outcome).success)
        assertEquals(EntityPosition(0.5f, 0.5f), entity.position)
    }

    @Test
    fun `entity drag previews existing visual at target without teleporting`() {
        val world = world()
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val settings = PlacementEntityPreviewSettings().apply {
            validColor = Color(0.2f, 0.3f, 0.4f, 0.5f)
            invalidColor = Color(0.7f, 0.6f, 0.5f, 0.4f)
        }
        val tool = DebugMoveTool(
            world,
            DebugWorldState(),
            entityPreviewSettings = settings
        )

        tool.begin(PickedTarget.Entity(entity, null), entity.currentTile)
        tool.dragTo(TilePosition(3, 2))

        val preview = requireNotNull(tool.preview?.entityPreview)
        assertEquals(EntityPosition(3.5f, 2.5f), preview.position)
        assertTrue(preview.valid)
        assertEquals(settings.validColor, preview.style.validColor)
        assertSame(entity, (preview.visual as EntityPreviewVisual.Existing).entity)
        assertEquals(EntityPosition(0.5f, 0.5f), entity.position)

        tool.dragTo(TilePosition(9, 9))
        val invalidPreview = requireNotNull(tool.preview?.entityPreview)
        assertFalse(invalidPreview.valid)
        assertEquals(settings.invalidColor, invalidPreview.style.invalidColor)
        assertEquals(EntityPosition(0.5f, 0.5f), entity.position)
    }

    @Test
    fun `disabled object and entity previews retain move diagnostics`() {
        val world = world()
        val placed = requireNotNull(world.place(TestObject(Footprint.square(1)), 1, 1))
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val tool = DebugMoveTool(
            world,
            DebugWorldState(),
            objectPreviewSettings = PlacementObjectPreviewSettings().apply {
                enabled = false
            },
            entityPreviewSettings = PlacementEntityPreviewSettings().apply {
                enabled = false
            }
        )

        tool.begin(PickedTarget.Object(placed, null), TilePosition(1, 1))
        assertTrue(requireNotNull(tool.preview).valid)
        assertFalse(requireNotNull(tool.preview).visible)
        assertNull(tool.preview?.objectPreview)
        tool.cancel()

        tool.begin(PickedTarget.Entity(entity, null), entity.currentTile)
        assertTrue(requireNotNull(tool.preview).valid)
        assertFalse(requireNotNull(tool.preview).visible)
        assertNull(tool.preview?.entityPreview)
    }

    @Test
    fun `drag cancellation clears preview without mutating world`() {
        val world = world()
        val placed = requireNotNull(world.place(TestObject(Footprint.square(1)), 1, 1))
        val tool = tool(world)
        tool.begin(PickedTarget.Object(placed, null), TilePosition(1, 1))
        tool.dragTo(TilePosition(4, 4))

        assertTrue(tool.cancel())

        assertFalse(tool.active)
        assertNull(tool.preview)
        assertSame(placed, world.getObjectAt(1, 1))
        assertNull(world.getObjectAt(4, 4))
    }

    private fun tool(world: World) = DebugMoveTool(world, DebugWorldState())
    private fun world() = World(5, 5) { _, _ -> TestTile }

    private data object TestTile : Tile
    private data object TestEntity : Entity
    private class TestObject(override val footprint: Footprint) : Placeable
}
