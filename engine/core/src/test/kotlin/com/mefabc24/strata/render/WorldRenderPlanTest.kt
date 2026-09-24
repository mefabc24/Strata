package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorldRenderPlanTest {

    private class TestTile : Tile

    private class Tree : Placeable {
        override val footprint = Footprint.square(1)
    }

    private class House : Placeable {
        override val footprint = Footprint.square(4)
    }

    private val projection = IsoProjection(
        TileGeometry(width = 32f, height = 24f)
    )

    @Test
    fun `tree behind foreground elevated terrain precedes its fill`() {
        val world = world()
        val tree = requireNotNull(world.place(Tree(), 1, 2))
        world.terrain.setHeight(3, 2, 3)

        val plan = WorldRenderPlan.create(world, projection)

        assertTrue(
            plan.indexOfObject(tree) < plan.indexOfFill(x = 3, y = 2)
        )
    }

    @Test
    fun `tree in front of elevated terrain follows its fill`() {
        val world = world()
        world.terrain.setHeight(1, 2, 3)
        val tree = requireNotNull(world.place(Tree(), 3, 2))

        val plan = WorldRenderPlan.create(world, projection)

        assertTrue(
            plan.indexOfFill(x = 1, y = 2) < plan.indexOfObject(tree)
        )
    }

    @Test
    fun `footprint ordering keeps house behind tree`() {
        val world = world()
        val house = requireNotNull(world.place(House(), 1, 1))
        val tree = requireNotNull(world.place(Tree(), 5, 2))

        val plan = WorldRenderPlan.create(world, projection)

        assertTrue(plan.indexOfObject(house) < plan.indexOfObject(tree))
    }

    @Test
    fun `fill levels remain below their terrain surface`() {
        val world = world()
        world.terrain.setHeight(2, 2, 4)

        val plan = WorldRenderPlan.create(world, projection)
        val columnItems = plan.filter { item ->
            when (item) {
                is WorldRenderItem.TerrainFill -> item.x == 2 && item.y == 2
                is WorldRenderItem.TerrainSurface -> item.x == 2 && item.y == 2
                else -> false
            }
        }

        assertEquals(
            listOf(3, 2, 1, 0),
            columnItems.filterIsInstance<WorldRenderItem.TerrainFill>()
                .map { it.part.levelBelowSurface }
        )
        assertIs<WorldRenderItem.TerrainSurface>(columnItems.last())
    }

    @Test
    fun `placement preview is always the final world item`() {
        val world = world()
        requireNotNull(world.place(House(), 1, 1))
        requireNotNull(world.place(Tree(), 5, 2))
        world.terrain.setHeight(2, 5, 3)
        val preview = PlacementPreview(
            placedObject = PlacedObject(Tree(), 4, 4),
            valid = true
        )

        val plan = WorldRenderPlan.withPreview(
            normalItems = WorldRenderPlan.create(world, projection),
            preview = preview
        )

        assertIs<WorldRenderItem.Preview>(plan.last())
        assertEquals(preview, (plan.last() as WorldRenderItem.Preview).preview)
    }

    private fun List<WorldRenderItem>.indexOfObject(
        placed: PlacedObject
    ): Int {
        return indexOfFirst { item ->
            item is WorldRenderItem.WorldObject &&
                    item.placedObject === placed
        }.also { index ->
            check(index >= 0)
        }
    }

    private fun List<WorldRenderItem>.indexOfFill(
        x: Int,
        y: Int
    ): Int {
        return indexOfFirst { item ->
            item is WorldRenderItem.TerrainFill &&
                    item.x == x &&
                    item.y == y
        }.also { index ->
            check(index >= 0)
        }
    }

    private fun world(): World {
        return World(width = 8, height = 8) { _, _ -> TestTile() }
    }
}
