package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.Input
import com.mefabc24.strata.debug.DebugDeleteToolSettings
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputProcessor
import com.mefabc24.strata.input.WorldInputTrigger
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugWorldRemoverTest {
    private data object Ground : Tile
    private data object LowerOverlay : Tile
    private data object UpperOverlay : Tile
    private data object TestEntity : Entity
    private class TestPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `frontmost targets are removed one at a time before terrain overlays`() {
        val world = worldWithOverlays()
        val position = TilePosition(0, 0)
        val placed = requireNotNull(world.place(TestPlaceable(), position))
        val entity = world.addEntity(TestEntity, EntityPosition.centerOf(position))
        val remover = DebugWorldRemover(world)

        assertEquals(
            DebugRemovalKind.ENTITY,
            remover.remove(PickedTarget.Entity(entity, null))
        )
        assertEquals(0, world.entityCount)
        assertSame(placed, world.getObjectAt(position))
        assertSame(UpperOverlay, world.getOverlayTile("upper", position))

        assertEquals(
            DebugRemovalKind.OBJECT,
            remover.remove(PickedTarget.Object(placed, null))
        )
        assertNull(world.getObjectAt(position))
        assertSame(UpperOverlay, world.getOverlayTile("upper", position))

        assertEquals(DebugRemovalKind.TERRAIN_OVERLAY, remover.remove(PickedTarget.Tile(position)))
        assertNull(world.getOverlayTile("upper", position))
        assertSame(LowerOverlay, world.getOverlayTile("lower", position))

        assertEquals(DebugRemovalKind.TERRAIN_OVERLAY, remover.remove(PickedTarget.Tile(position)))
        assertNull(world.getOverlayTile("lower", position))
        assertNull(remover.remove(PickedTarget.Tile(position)))
    }

    @Test
    fun `empty and stale targets are ignored`() {
        val world = worldWithOverlays()
        val remover = DebugWorldRemover(world)
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        assertTrue(world.removeEntity(entity))

        assertNull(remover.remove(null))
        assertNull(remover.removeEntity(entity))
        assertNull(remover.remove(PickedTarget.Tile(TilePosition(1, 1))))
        assertNull(remover.remove(PickedTarget.Tile(TilePosition(-1, 0))))
    }

    @Test
    fun `entity tool right click removes without invoking left click spawn`() {
        val world = World(1, 1) { _, _ -> Ground }
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val remover = DebugWorldRemover(world)
        var spawnCount = 0
        val input = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Entity(
                    WorldInputTrigger.MouseDown(Input.Buttons.RIGHT)
                ) { remover.removeEntity(it) != null },
                WorldInputBinding.Tile(
                    WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) { _, _ -> spawnCount++; true }
            ),
            pickTile = { _, _ -> TilePosition(0, 0) },
            pickObject = { _, _, _ -> null },
            pickEntity = { _, _, _ -> entity }
        )

        assertTrue(input.touchDown(0, 0, 0, Input.Buttons.RIGHT))
        assertEquals(0, spawnCount)
        assertEquals(0, world.entityCount)
        assertFalse(input.touchDown(0, 0, 0, Input.Buttons.RIGHT))
    }

    @Test
    fun `brush size affects click deletion when dragging is disabled`() {
        val world = World(5, 5) { _, _ -> Ground }.apply { addOverlayLayer("overlay") }
        val inside = requireNotNull(world.place(TestPlaceable(), 1, 1))
        val outside = requireNotNull(world.place(TestPlaceable(), 4, 4))
        val entity = world.addEntity(TestEntity, EntityPosition.centerOf(TilePosition(2, 2)))
        world.setOverlayTile("overlay", 0, 0, UpperOverlay)
        val settings = DebugDeleteToolSettings().apply {
            brushSize = 3
            dragEnabled = false
        }
        val remover = DebugWorldRemover(world, settings)

        val removed = remover.beginDelete(TilePosition(1, 1))

        assertEquals(
            setOf(DebugRemovalKind.OBJECT, DebugRemovalKind.ENTITY, DebugRemovalKind.TERRAIN_OVERLAY),
            removed
        )
        assertFalse(inside in world.getObjects())
        assertFalse(entity in world.getEntities())
        assertNull(world.getOverlayTile("overlay", 0, 0))
        assertSame(outside, world.getObjectAt(4, 4))

        assertTrue(remover.dragDelete(TilePosition(4, 4)).isEmpty())
        assertSame(outside, world.getObjectAt(4, 4))
    }

    @Test
    fun `enabled drag deletion fills gaps across quickly sampled positions`() {
        val world = World(7, 1) { _, _ -> Ground }.apply { addOverlayLayer("overlay") }
        repeat(7) { x ->
            requireNotNull(world.place(TestPlaceable(), x, 0))
            world.addEntity(TestEntity, EntityPosition.centerOf(TilePosition(x, 0)))
            world.setOverlayTile("overlay", x, 0, UpperOverlay)
        }
        val settings = DebugDeleteToolSettings().apply { dragEnabled = true }
        val remover = DebugWorldRemover(world, settings)

        remover.beginDelete(TilePosition(0, 0))
        val removedByDrag = remover.dragDelete(TilePosition(6, 0))

        assertEquals(
            setOf(DebugRemovalKind.OBJECT, DebugRemovalKind.ENTITY, DebugRemovalKind.TERRAIN_OVERLAY),
            removedByDrag
        )
        assertEquals(0, world.placedObjectCount)
        assertEquals(0, world.entityCount)
        assertEquals(0, world.overlayTileCount)
        assertTrue(remover.endDelete())
        assertFalse(remover.endDelete())
    }

    private fun worldWithOverlays() = World(2, 2) { _, _ -> Ground }.apply {
        addOverlayLayer("lower")
        addOverlayLayer("upper")
        setOverlayTile("lower", 0, 0, LowerOverlay)
        setOverlayTile("upper", 0, 0, UpperOverlay)
    }
}
