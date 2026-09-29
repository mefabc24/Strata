package com.mefabc24.strata.iso

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.render.order.WorldEntityPrimitive
import com.mefabc24.strata.render.order.WorldObjectPrimitive
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class PickingDebugSnapshotTest {
    private data object TestTile : Tile
    private data object TestEntity : Entity
    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `picked debug target follows authoritative render order`() {
        val world = World(2, 2) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val objectBounds = Rectangle(1f, 2f, 3f, 4f)
        val entityBounds = Rectangle(5f, 6f, 7f, 8f)
        val objectResult = SpritePickDiagnostic(
            picked = placed,
            tested = placed,
            testedBounds = objectBounds,
            alphaAccepted = true,
            pickedBounds = objectBounds
        )
        val entityResult = SpritePickDiagnostic(
            picked = entity,
            tested = entity,
            testedBounds = entityBounds,
            alphaAccepted = true,
            pickedBounds = entityBounds,
            pickedAlphaAccepted = true
        )

        val picked = frontmostPickedSprite(
            renderPlan = listOf(WorldObjectPrimitive(placed), WorldEntityPrimitive(entity)),
            objectResult = objectResult,
            entityResult = entityResult
        )

        val pickedEntity = assertIs<PickedSpriteTarget.Entity>(picked)
        assertSame(entity, pickedEntity.worldEntity)
        assertEquals(entityBounds, pickedEntity.bounds)
        assertEquals(true, pickedEntity.alphaAccepted)
    }

    @Test
    fun `picked bounds belong to the selected candidate`() {
        val world = World(2, 2) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(TestObject(), 0, 0))
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val objectBounds = Rectangle(1f, 2f, 3f, 4f)

        val picked = frontmostPickedSprite(
            renderPlan = listOf(WorldEntityPrimitive(entity), WorldObjectPrimitive(placed)),
            objectResult = SpritePickDiagnostic(placed, placed, objectBounds, true, objectBounds),
            entityResult = SpritePickDiagnostic(entity, entity, Rectangle(), true, Rectangle())
        )

        val pickedObject = assertIs<PickedSpriteTarget.Object>(picked)
        assertSame(placed, pickedObject.placedObject)
        assertEquals(objectBounds, pickedObject.bounds)
    }
}
