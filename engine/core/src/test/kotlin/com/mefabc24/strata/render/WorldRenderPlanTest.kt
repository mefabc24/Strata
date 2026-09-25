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
    fun `flat terrain orders back cells before front cells`() {
        val plan = WorldRenderPlan.create(world(size = 3), projection)

        assertEquals(9, plan.size)
        assertTrue(plan.all { it is TerrainTop })
        assertTrue(plan.indexOfTop(0, 0) < plan.indexOfTop(1, 0))
        assertTrue(plan.indexOfTop(0, 0) < plan.indexOfTop(0, 1))
        assertTrue(plan.indexOfTop(1, 0) < plan.indexOfTop(2, 0))
    }

    @Test
    fun `height one side renders before flat foreground top`() {
        val world = world()
        world.terrain.setHeight(1, 1, 1)

        val plan = WorldRenderPlan.create(world, projection)

        assertTrue(
            plan.indexOfSide(1, 1, 0, TerrainFace.RIGHT) <
                    plan.indexOfTop(2, 1)
        )
    }

    @Test
    fun `height two sides order deepest to highest to top`() {
        val world = world()
        world.terrain.setHeight(1, 1, 2)

        val plan = WorldRenderPlan.create(world, projection)
        val deepest = plan.indexOfSide(1, 1, 1, TerrainFace.RIGHT)
        val highest = plan.indexOfSide(1, 1, 0, TerrainFace.RIGHT)
        val top = plan.indexOfTop(1, 1)

        assertTrue(deepest < highest)
        assertTrue(highest < top)
    }

    @Test
    fun `ascending and descending staircases obey shared spatial relations`() {
        for (heights in listOf(
            listOf(0, 1, 2, 3),
            listOf(3, 2, 1, 0)
        )) {
            val world = world(size = 4)

            for ((x, height) in heights.withIndex()) {
                world.terrain.setHeight(x, 0, height)
            }

            val first = WorldRenderPlan.create(world, projection)
            val second = WorldRenderPlan.create(world, projection)

            assertEquals(first, second)
            assertDefiniteRelations(first)
        }
    }

    @Test
    fun `plateau has no internal side primitives`() {
        val world = world(size = 3)
        world.terrain.setHeight(0..2, 0..2, 3)

        val plan = WorldRenderPlan.create(world, projection)
        val internalSides = plan.filterIsInstance<TerrainSide>().filter {
            it.x < 2 && it.y < 2
        }

        assertEquals(emptyList(), internalSides)
        assertTrue(plan.any { it is TerrainSide && it.x == 2 })
        assertTrue(plan.any { it is TerrainSide && it.y == 2 })
    }

    @Test
    fun `asymmetric exposure creates only the required terrain face`() {
        val world = world()
        world.terrain.setHeight(1, 1, 3)
        world.terrain.setHeight(1, 2, 3)

        val sides = WorldRenderPlan.create(world, projection)
            .filterIsInstance<TerrainSide>()
            .filter { it.x == 1 && it.y == 1 }

        assertEquals(3, sides.size)
        assertTrue(sides.all { it.face == TerrainFace.RIGHT })
    }

    @Test
    fun `terrain without fill visual creates no side primitives`() {
        val world = world()
        world.terrain.setHeight(1, 1, 3)

        val plan = WorldRenderPlan.create(
            world = world,
            projection = projection,
            hasFillFor = { false }
        )

        assertTrue(plan.none { it is TerrainSide })
    }

    @Test
    fun `house on flat ground follows every supporting top`() {
        val world = world()
        val house = requireNotNull(world.place(House(), 1, 1))

        assertSupportingTopsBeforeObject(world, house)
    }

    @Test
    fun `house on height three follows every supporting top`() {
        val world = world()
        world.terrain.setHeight(1..4, 1..4, 3)
        val house = requireNotNull(world.place(House(), 1, 1))

        assertSupportingTopsBeforeObject(world, house)
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
    fun `object behind high terrain renders before its wall`() {
        val world = world()
        val tree = requireNotNull(world.place(Tree(), 1, 2))
        world.terrain.setHeight(4, 2, 3)

        val plan = WorldRenderPlan.create(world, projection)
        val treeIndex = plan.indexOfObject(tree)
        val terrainIndices = plan.indices.filter { index ->
            when (val primitive = plan[index]) {
                is TerrainSide -> primitive.x == 4 && primitive.y == 2
                is TerrainTop -> primitive.x == 4 && primitive.y == 2
                else -> false
            }
        }

        assertTrue(terrainIndices.all { treeIndex < it })
    }

    @Test
    fun `object in front of high terrain renders after its wall`() {
        val world = world()
        world.terrain.setHeight(1, 2, 3)
        val tree = requireNotNull(world.place(Tree(), 4, 2))

        val plan = WorldRenderPlan.create(world, projection)
        val treeIndex = plan.indexOfObject(tree)
        val terrainIndices = plan.indices.filter { index ->
            when (val primitive = plan[index]) {
                is TerrainSide -> primitive.x == 1 && primitive.y == 2
                is TerrainTop -> primitive.x == 1 && primitive.y == 2
                else -> false
            }
        }

        assertTrue(terrainIndices.all { it < treeIndex })
    }

    @Test
    fun `ambiguous object placement uses deterministic fallback`() {
        val world = world(size = 10)
        val house = requireNotNull(world.place(House(), 2, 2))
        val tree = requireNotNull(world.place(Tree(), 6, 1))

        val housePrimitive = WorldObjectPrimitive(house, 0)
        val treePrimitive = WorldObjectPrimitive(tree, 0)

        assertEquals(
            IsoSpatialRelation.AMBIGUOUS,
            housePrimitive.sortVolume.relationTo(treePrimitive.sortVolume)
        )

        val first = WorldRenderPlan.create(world, projection)
        repeat(5) {
            assertEquals(first, WorldRenderPlan.create(world, projection))
        }
    }

    @Test
    fun `terrain edits around objects retain every primitive exactly once`() {
        val world = world(size = 10)
        requireNotNull(world.place(House(), 1, 1))
        requireNotNull(world.place(Tree(), 6, 2))
        requireNotNull(world.place(Tree(), 2, 6))
        requireNotNull(world.place(Tree(), 8, 7))

        val edits = listOf(
            Triple(5, 5, 1),
            Triple(6, 5, 2),
            Triple(7, 5, 4),
            Triple(5, 6, 3),
            Triple(6, 5, 0),
            Triple(7, 5, 1),
            Triple(5, 5, 4),
            Triple(5, 5, 0)
        )

        for ((x, y, height) in edits) {
            assertTrue(world.terrain.setHeight(x, y, height))

            val first = WorldRenderPlan.create(world, projection)
            val second = WorldRenderPlan.create(world, projection)

            assertEquals(first, second)
            assertEquals(first.size, first.toSet().size)
        }
    }

    @Test
    fun `placement preview is always the final render item`() {
        val world = world()
        requireNotNull(world.place(House(), 1, 1))
        val preview = PlacementPreview(
            placedObject = PlacedObject(Tree(), 5, 5),
            valid = true
        )

        val plan = WorldRenderPlan.withPreview(
            normalItems = WorldRenderPlan.create(world, projection),
            preview = preview
        )

        assertIs<PreviewRenderItem>(plan.last())
        assertEquals(preview, (plan.last() as PreviewRenderItem).preview)
    }

    private fun assertSupportingTopsBeforeObject(
        world: World,
        placed: PlacedObject
    ) {
        val plan = WorldRenderPlan.create(world, projection)
        val objectIndex = plan.indexOfObject(placed)

        for (position in placed.occupiedTiles()) {
            assertTrue(
                plan.indexOfTop(position.x, position.y) < objectIndex,
                "Supporting top $position rendered after its object."
            )
        }
    }

    private fun assertDefiniteRelations(
        plan: List<WorldRenderPrimitive>
    ) {
        for (firstIndex in plan.indices) {
            for (secondIndex in firstIndex + 1 until plan.size) {
                val relation = plan[firstIndex].sortVolume.relationTo(
                    plan[secondIndex].sortVolume
                )

                assertTrue(relation != IsoSpatialRelation.IN_FRONT)
            }
        }
    }

    private fun List<WorldRenderPrimitive>.indexOfTop(x: Int, y: Int): Int {
        return indexOfFirst {
            it is TerrainTop && it.x == x && it.y == y
        }.also { check(it >= 0) }
    }

    private fun List<WorldRenderPrimitive>.indexOfSide(
        x: Int,
        y: Int,
        level: Int,
        face: TerrainFace
    ): Int {
        return indexOfFirst {
            it is TerrainSide &&
                    it.x == x &&
                    it.y == y &&
                    it.levelBelowSurface == level &&
                    it.face == face
        }.also { check(it >= 0) }
    }

    private fun List<WorldRenderPrimitive>.indexOfObject(
        placed: PlacedObject
    ): Int {
        return indexOfFirst {
            it is WorldObjectPrimitive && it.placedObject === placed
        }.also { check(it >= 0) }
    }

    private fun world(size: Int = 8): World {
        return World(size, size) { _, _ -> TestTile() }
    }
}
