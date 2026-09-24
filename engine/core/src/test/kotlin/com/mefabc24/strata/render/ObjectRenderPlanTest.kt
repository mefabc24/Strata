package com.mefabc24.strata.render

import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObjectRenderPlanTest {

    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    @Test
    fun `placement preview is always the final object draw item`() {
        val worldObjects = listOf(
            PlacedObject(TestObject(), 0, 0),
            PlacedObject(TestObject(), 1, 0),
            PlacedObject(TestObject(), 2, 0)
        )
        val preview = PlacementPreview(
            placedObject = PlacedObject(TestObject(), 1, 1),
            valid = true
        )

        val plan = ObjectRenderPlan.create(worldObjects, preview)

        assertEquals(4, plan.size)
        assertEquals(
            worldObjects,
            plan.dropLast(1).map(ObjectRenderItem::placedObject)
        )
        assertIs<ObjectRenderItem.Preview>(plan.last())
    }
}
