package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.Input
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

    private fun worldWithOverlays() = World(2, 2) { _, _ -> Ground }.apply {
        addOverlayLayer("lower")
        addOverlayLayer("upper")
        setOverlayTile("lower", 0, 0, LowerOverlay)
        setOverlayTile("upper", 0, 0, UpperOverlay)
    }
}
