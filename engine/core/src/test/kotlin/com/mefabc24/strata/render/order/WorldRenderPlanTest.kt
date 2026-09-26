package com.mefabc24.strata.render.order

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
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

    private data class Walker(val id: Int) : Entity

    private val projection = IsoProjection(
        TileGeometry(width = 32f, height = 24f)
    )

    @Test
    fun `flat terrain orders back cells before front cells`() {
        val metrics = IsoRenderOrderMetrics()
        val plan = WorldRenderPlan.create(
            world = world(size = 3),
            projection = projection,
            metrics = metrics
        )

        assertEquals(9, plan.size)
        assertTrue(plan.all { it is TerrainCell })
        assertTrue(plan.indexOfCell(0, 0) < plan.indexOfCell(1, 0))
        assertTrue(plan.indexOfCell(0, 0) < plan.indexOfCell(0, 1))
        assertTrue(plan.indexOfCell(1, 0) < plan.indexOfCell(2, 0))
        assertEquals(0, metrics.relationChecks)
    }

    @Test
    fun `each world coordinate creates one flat terrain cell`() {
        val world = world()

        val plan = WorldRenderPlan.create(world, projection)

        assertEquals(world.width * world.height, plan.size)
        assertEquals(
            TerrainCell(x = 1, y = 1),
            plan.single { it is TerrainCell && it.x == 1 && it.y == 1 }
        )
    }

    @Test
    fun `house on flat ground follows every supporting terrain cell`() {
        val world = world()
        val house = requireNotNull(world.place(House(), 1, 1))

        assertSupportingTerrainBeforeObject(world, house)
    }

    @Test
    fun `tree in front of large house renders after house`() {
        val world = world(size = 10)
        val house = requireNotNull(world.place(House(), 2, 2))
        val tree = requireNotNull(world.place(Tree(), 6, 3))
        val plan = WorldRenderPlan.create(world, projection)

        assertTrue(plan.indexOfObject(house) < plan.indexOfObject(tree))
    }

    @Test
    fun `tree behind large house renders before house`() {
        val world = world(size = 10)
        val house = requireNotNull(world.place(House(), 2, 2))
        val tree = requireNotNull(world.place(Tree(), 1, 3))
        val plan = WorldRenderPlan.create(world, projection)

        assertTrue(plan.indexOfObject(tree) < plan.indexOfObject(house))
    }

    @Test
    fun `entity changes from behind to in front of the same object`() {
        val world = world(size = 10)
        val house = requireNotNull(world.place(House(), 2, 2))
        val entity = world.addEntity(Walker(1), EntityPosition(0.5f, 0.5f))

        val behindPlan = WorldRenderPlan.create(world, projection)
        assertTrue(
            behindPlan.indexOfEntity(entity) < behindPlan.indexOfObject(house)
        )

        entity.position = EntityPosition(7.5f, 7.5f)
        val frontPlan = WorldRenderPlan.create(world, projection)
        assertTrue(
            frontPlan.indexOfObject(house) < frontPlan.indexOfEntity(entity)
        )
    }

    @Test
    fun `entities follow their supporting terrain and order deterministically`() {
        val world = world(size = 8)
        val first = world.addEntity(Walker(1), EntityPosition(2.5f, 3.5f))
        val second = world.addEntity(Walker(2), EntityPosition(2.5f, 3.5f))

        val plan = WorldRenderPlan.create(world, projection)
        val terrainIndex = plan.indexOfCell(2, 3)

        assertTrue(terrainIndex < plan.indexOfEntity(first))
        assertTrue(terrainIndex < plan.indexOfEntity(second))
        repeat(5) {
            assertEquals(plan, WorldRenderPlan.create(world, projection))
        }
    }

    @Test
    fun `object behind foreground terrain renders before its cell`() {
        val world = world()
        val tree = requireNotNull(world.place(Tree(), 1, 2))

        val plan = WorldRenderPlan.create(world, projection)
        val treeIndex = plan.indexOfObject(tree)
        val terrainIndices = plan.indices.filter { index ->
            plan[index].isTerrainAt(4, 2)
        }

        assertTrue(terrainIndices.all { treeIndex < it })
    }

    @Test
    fun `object in front of background terrain renders after its cell`() {
        val world = world()
        val tree = requireNotNull(world.place(Tree(), 4, 2))

        val plan = WorldRenderPlan.create(world, projection)
        val treeIndex = plan.indexOfObject(tree)
        val terrainIndices = plan.indices.filter { index ->
            plan[index].isTerrainAt(1, 2)
        }

        assertTrue(terrainIndices.all { it < treeIndex })
    }

    @Test
    fun `flat terrain produces a stable complete plan`() {
        val world = world(size = 12)

        val first = WorldRenderPlan.create(world, projection)
        repeat(5) {
            assertEquals(first, WorldRenderPlan.create(world, projection))
        }
        assertEquals(first.size, first.toSet().size)
    }

    @Test
    fun `large world checks only object relationship candidates`() {
        val world = world(size = 50)
        val objects = listOf(
            requireNotNull(world.place(Tree(), 2, 2)),
            requireNotNull(world.place(Tree(), 8, 5)),
            requireNotNull(world.place(Tree(), 15, 12)),
            requireNotNull(world.place(Tree(), 23, 30)),
            requireNotNull(world.place(Tree(), 35, 18)),
            requireNotNull(world.place(Tree(), 44, 44))
        )

        val metrics = IsoRenderOrderMetrics()
        val first = WorldRenderPlan.create(world, projection, metrics = metrics)

        assertEquals(2_500 + objects.size, first.size)
        assertEquals(objects.size * (objects.size - 1) / 2, metrics.relationChecks)
        assertTrue(metrics.relationChecks < 100)

        repeat(10) {
            val nextMetrics = IsoRenderOrderMetrics()
            val firstPlan = WorldRenderPlan.create(
                world,
                projection,
                metrics = nextMetrics
            )
            val secondPlan = WorldRenderPlan.create(world, projection)

            assertEquals(firstPlan, secondPlan)
            assertEquals(firstPlan.size, firstPlan.toSet().size)
            assertEquals(15, nextMetrics.relationChecks)
        }
    }

    @Test
    fun `placement previews are appended last in input order`() {
        val world = world()
        requireNotNull(world.place(House(), 1, 1))
        val previews = listOf(
            PlacementPreview(
                placedObject = PlacedObject(Tree(), 5, 5),
                valid = true
            ),
            PlacementPreview(
                placedObject = PlacedObject(Tree(), 6, 5),
                valid = false
            )
        )

        val normalItems = WorldRenderPlan.create(world, projection)
        val plan = WorldRenderPlan.withPreviews(
            normalItems = normalItems,
            previews = previews
        )

        assertTrue(plan.take(normalItems.size).all { it is WorldRenderPrimitive })
        assertTrue(plan.takeLast(2).all { it is PreviewRenderItem })
        assertEquals(
            previews,
            plan.takeLast(2).map { (it as PreviewRenderItem).preview }
        )
    }

    private fun assertSupportingTerrainBeforeObject(
        world: World,
        placed: PlacedObject
    ) {
        val plan = WorldRenderPlan.create(world, projection)
        val objectIndex = plan.indexOfObject(placed)

        for (position in placed.occupiedTiles()) {
            val supportingIndices = plan.indices.filter { index ->
                plan[index].isTerrainAt(position.x, position.y)
            }

            assertTrue(supportingIndices.isNotEmpty())
            assertTrue(
                supportingIndices.all { it < objectIndex },
                "Supporting terrain $position rendered after its object."
            )
        }
    }

    private fun WorldRenderPrimitive.isTerrainAt(x: Int, y: Int): Boolean {
        return when (this) {
            is TerrainCell -> this.x == x && this.y == y
            is WorldObjectPrimitive -> false
            is WorldEntityPrimitive -> false
        }
    }

    private fun List<WorldRenderPrimitive>.indexOfCell(x: Int, y: Int): Int {
        return indexOfFirst {
            it is TerrainCell && it.x == x && it.y == y
        }.also { check(it >= 0) }
    }

    private fun List<WorldRenderPrimitive>.indexOfObject(
        placed: PlacedObject
    ): Int {
        return indexOfFirst {
            it is WorldObjectPrimitive && it.placedObject === placed
        }.also { check(it >= 0) }
    }

    private fun List<WorldRenderPrimitive>.indexOfEntity(
        entity: com.mefabc24.strata.world.WorldEntity
    ): Int {
        return indexOfFirst {
            it is WorldEntityPrimitive && it.worldEntity === entity
        }.also { check(it >= 0) }
    }

    private fun world(size: Int = 8): World {
        return World(size, size) { _, _ -> TestTile() }
    }
}
