package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
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
    fun `surface precedes same-anchor fill before foreground cell`() {
        val world = world()
        world.terrain.setHeight(1, 1, 1)

        val plan = WorldRenderPlan.create(world, projection)
        val cell = plan.indexOfCell(1, 1)
        val fill = plan.indexOfFill(1, 1, 0, TerrainFace.RIGHT)
        val foreground = plan.indexOfCell(2, 1)

        assertTrue(cell < fill)
        assertTrue(fill < foreground)
    }

    @Test
    fun `height four fills order from surface toward deepest level`() {
        val world = world()
        world.terrain.setHeight(1, 1, 4)

        val plan = WorldRenderPlan.create(world, projection)
        val indices = listOf(
            plan.indexOfCell(1, 1),
            plan.indexOfFill(1, 1, 0, TerrainFace.RIGHT),
            plan.indexOfFill(1, 1, 1, TerrainFace.RIGHT),
            plan.indexOfFill(1, 1, 2, TerrainFace.RIGHT),
            plan.indexOfFill(1, 1, 3, TerrainFace.RIGHT)
        )

        assertEquals(indices.sorted(), indices)
        assertEquals(
            listOf(3, 2, 1, 0),
            plan.filterIsInstance<TerrainFill>()
                .filter { it.x == 1 && it.y == 1 }
                .filter { it.face == TerrainFace.RIGHT }
                .sortedBy { it.levelBelowSurface }
                .map(TerrainFill::bottomElevation)
        )
    }

    @Test
    fun `ascending and descending staircases remain deterministic`() {
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
            val cells = heights.indices.map { first.indexOfCell(it, 0) }

            assertEquals(first, second)
            assertEquals(cells.sorted(), cells)
        }
    }

    @Test
    fun `plateau has no internal fill primitives`() {
        val world = world(size = 3)
        world.terrain.setHeight(0..2, 0..2, 3)

        val plan = WorldRenderPlan.create(world, projection)
        val internalFills = plan.filterIsInstance<TerrainFill>().filter {
            it.x < 2 && it.y < 2
        }

        assertEquals(emptyList(), internalFills)
        assertTrue(plan.any { it is TerrainFill && it.x == 2 })
        assertTrue(plan.any { it is TerrainFill && it.y == 2 })
    }

    @Test
    fun `asymmetric exposure creates only the required fill face`() {
        val world = world()
        world.terrain.setHeight(1, 1, 3)
        world.terrain.setHeight(1, 2, 3)

        val fills = WorldRenderPlan.create(world, projection)
            .filterIsInstance<TerrainFill>()
            .filter { it.x == 1 && it.y == 1 }

        assertEquals(3, fills.size)
        assertTrue(fills.all { it.face == TerrainFace.RIGHT })
    }

    @Test
    fun `terrain without fill visual creates no fill primitives`() {
        val world = world()
        world.terrain.setHeight(1, 1, 3)

        val plan = WorldRenderPlan.create(
            world = world,
            projection = projection,
            hasFillFor = { false }
        )

        assertTrue(plan.none { it is TerrainFill })
    }

    @Test
    fun `house on flat ground follows every supporting terrain primitive`() {
        val world = world()
        val house = requireNotNull(world.place(House(), 1, 1))

        assertSupportingTerrainBeforeObject(world, house)
    }

    @Test
    fun `house on height three follows every supporting terrain primitive`() {
        val world = world()
        world.terrain.setHeight(1..4, 1..4, 3)
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
    fun `object behind high terrain renders before its fills and cell`() {
        val world = world()
        val tree = requireNotNull(world.place(Tree(), 1, 2))
        world.terrain.setHeight(4, 2, 3)

        val plan = WorldRenderPlan.create(world, projection)
        val treeIndex = plan.indexOfObject(tree)
        val terrainIndices = plan.indices.filter { index ->
            plan[index].isTerrainAt(4, 2)
        }

        assertTrue(terrainIndices.all { treeIndex < it })
    }

    @Test
    fun `object in front of high terrain renders after its fills and cell`() {
        val world = world()
        world.terrain.setHeight(1, 2, 3)
        val tree = requireNotNull(world.place(Tree(), 4, 2))

        val plan = WorldRenderPlan.create(world, projection)
        val treeIndex = plan.indexOfObject(tree)
        val terrainIndices = plan.indices.filter { index ->
            plan[index].isTerrainAt(1, 2)
        }

        assertTrue(terrainIndices.all { it < treeIndex })
    }

    @Test
    fun `random uneven terrain produces a stable complete plan`() {
        val world = world(size = 12)

        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                world.terrain.setHeight(x, y, (x * 7 + y * 11) % 6)
            }
        }

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
        val occupied = objects.flatMap(PlacedObject::occupiedTiles).toSet()

        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                if (TilePosition(x, y) !in occupied) {
                    world.terrain.setHeight(x, y, (x * 3 + y * 5) % 5)
                }
            }
        }

        val metrics = IsoRenderOrderMetrics()
        val first = WorldRenderPlan.create(world, projection, metrics = metrics)

        assertTrue(first.size >= 2_500)
        assertEquals(objects.size * (objects.size - 1) / 2, metrics.relationChecks)
        assertTrue(metrics.relationChecks < 100)

        repeat(10) { edit ->
            val position = TilePosition(10 + edit, 40)
            world.terrain.setHeight(position.x, position.y, edit % 7)

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
            is TerrainFill -> this.x == x && this.y == y
            is WorldObjectPrimitive -> false
        }
    }

    private fun List<WorldRenderPrimitive>.indexOfCell(x: Int, y: Int): Int {
        return indexOfFirst {
            it is TerrainCell && it.x == x && it.y == y
        }.also { check(it >= 0) }
    }

    private fun List<WorldRenderPrimitive>.indexOfFill(
        x: Int,
        y: Int,
        level: Int,
        face: TerrainFace
    ): Int {
        return indexOfFirst {
            it is TerrainFill &&
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
