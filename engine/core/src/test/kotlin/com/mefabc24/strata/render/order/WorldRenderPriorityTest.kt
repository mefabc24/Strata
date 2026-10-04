package com.mefabc24.strata.render.order

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldRenderPriorityTest {

    private data object TestTile : Tile

    private data class Marker(val id: Int) : Placeable {
        override val footprint = Footprint.square(1)
    }

    private data class LargeMarker(val id: Int) : Placeable {
        override val footprint = Footprint.square(2)
    }

    private data class Walker(val id: Int) : Entity

    private val projection = IsoProjection(
        TileGeometry(width = 32f, height = 24f)
    )

    @Test
    fun `default priorities preserve the existing render plan`() {
        val world = world()
        world.place(LargeMarker(1), 2, 2)
        world.place(Marker(2), 6, 3)
        world.addEntity(Walker(1), EntityPosition(4.5f, 4.5f))

        val existing = WorldRenderPlan.create(world, projection)
        val explicitDefaults = WorldRenderPlan.create(
            world = world,
            projection = projection,
            objectPriorityFor = { 0 },
            entityPriorityFor = { 0 }
        )

        assertEquals(existing, explicitDefaults)
        assertTrue(existing.visuals().all { it.renderPriority == 0 })
    }

    @Test
    fun `negative default and positive object groups are strict`() {
        val world = world()
        val negative = requireNotNull(world.place(Marker(1), 7, 7))
        val default = requireNotNull(world.place(Marker(2), 4, 4))
        val positive = requireNotNull(world.place(Marker(3), 0, 0))
        val priorities = mapOf(
            negative to -2,
            default to 0,
            positive to 3
        )

        val objects = WorldRenderPlan.create(
            world,
            projection,
            objectPriorityFor = priorities::getValue
        ).objects()

        assertEquals(listOf(negative, default, positive), objects)
    }

    @Test
    fun `same priority objects retain spatial ordering`() {
        val world = world()
        val large = requireNotNull(world.place(LargeMarker(1), 2, 2))
        val front = requireNotNull(world.place(Marker(2), 6, 3))

        val objects = WorldRenderPlan.create(
            world,
            projection,
            objectPriorityFor = { 4 }
        ).objects()

        assertTrue(objects.indexOf(large) < objects.indexOf(front))
    }

    @Test
    fun `priority overrides multi tile spatial dependencies`() {
        val world = world()
        val lowFront = requireNotNull(world.place(LargeMarker(1), 6, 6))
        val highBack = requireNotNull(world.place(LargeMarker(2), 1, 1))

        val objects = WorldRenderPlan.create(
            world,
            projection,
            objectPriorityFor = { placed ->
                if (placed === lowFront) -1 else 1
            }
        ).objects()

        assertEquals(listOf(lowFront, highBack), objects)
    }

    @Test
    fun `object and entity priorities override their spatial relationship`() {
        val world = world()
        val lowFront = requireNotNull(world.place(Marker(1), 7, 7))
        val defaultBack = world.addEntity(
            Walker(1),
            EntityPosition(0.5f, 0.5f)
        )

        val plan = WorldRenderPlan.create(
            world,
            projection,
            objectPriorityFor = { -1 }
        )

        assertTrue(plan.indexOfObject(lowFront) < plan.indexOfEntity(defaultBack))

        val positivePlan = WorldRenderPlan.create(
            world,
            projection,
            objectPriorityFor = { 1 }
        )

        assertTrue(
            positivePlan.indexOfEntity(defaultBack) <
                    positivePlan.indexOfObject(lowFront)
        )
    }

    @Test
    fun `entity priority groups are strict`() {
        val world = world()
        val lowFront = world.addEntity(Walker(1), EntityPosition(7.5f, 7.5f))
        val highBack = world.addEntity(Walker(2), EntityPosition(0.5f, 0.5f))

        val entities = WorldRenderPlan.create(
            world,
            projection,
            entityPriorityFor = { entity ->
                if (entity === lowFront) -3 else 2
            }
        ).entities()

        assertEquals(listOf(lowFront, highBack), entities)
    }

    @Test
    fun `incremental static update inserts a new priority group`() {
        val world = world()
        val existing = requireNotNull(world.place(LargeMarker(1), 1, 1))
        val priorities = mutableMapOf(existing to 2)
        val initial = WorldRenderPlan.prepareStatic(
            world,
            projection,
            objectPriorityFor = priorities::getValue
        )
        val added = requireNotNull(world.place(LargeMarker(2), 6, 6))
        priorities[added] = -2
        val metrics = IsoRenderOrderMetrics()

        val updated = WorldRenderPlan.updateStatic(
            previous = initial,
            world = world,
            projection = projection,
            metrics = metrics,
            objectPriorityFor = priorities::getValue
        )

        assertEquals(listOf(added, existing), updated.orderedItems.objects())
        assertEquals(0, metrics.relationChecks)
    }

    @Test
    fun `moving entity remains above a lower priority object`() {
        val world = world()
        val road = requireNotNull(world.place(Marker(1), 4, 4))
        val staticPlan = WorldRenderPlan.prepareStatic(
            world,
            projection,
            objectPriorityFor = { -1 }
        )
        val walker = world.addEntity(Walker(1), EntityPosition(0.5f, 0.5f))

        repeat(2) { step ->
            if (step == 1) {
                walker.teleport(EntityPosition(7.5f, 7.5f))
            }
            val metrics = IsoRenderOrderMetrics()
            val plan = WorldRenderPlan.withEntities(
                staticPlan = staticPlan,
                world = world,
                projection = projection,
                metrics = metrics
            )

            assertTrue(plan.indexOfObject(road) < plan.indexOfEntity(walker))
            assertEquals(0, metrics.relationChecks)
        }
    }

    @Test
    fun `priority ordering is deterministic across repeated plans`() {
        val world = world()
        val objects = (0 until 6).map { index ->
            requireNotNull(world.place(Marker(index), index, 7 - index))
        }
        val entities = (0 until 4).map { index ->
            world.addEntity(
                Walker(index),
                EntityPosition(index + 0.5f, index + 0.5f)
            )
        }
        val objectPriorities = objects.withIndex().associate { (index, placed) ->
            placed to listOf(2, -1, 0)[index % 3]
        }
        val entityPriorities = entities.withIndex().associate { (index, entity) ->
            entity to listOf(1, -1)[index % 2]
        }
        val first = WorldRenderPlan.create(
            world,
            projection,
            objectPriorityFor = objectPriorities::getValue,
            entityPriorityFor = entityPriorities::getValue
        )

        repeat(5) {
            assertEquals(
                first,
                WorldRenderPlan.create(
                    world,
                    projection,
                    objectPriorityFor = objectPriorities::getValue,
                    entityPriorityFor = entityPriorities::getValue
                )
            )
        }

        assertEquals(
            first.visuals().map(PrioritizedWorldRenderPrimitive::renderPriority),
            first.visuals().map(PrioritizedWorldRenderPrimitive::renderPriority).sorted()
        )
    }

    private fun world(): World = World(10, 10) { _, _ -> TestTile }

    private fun List<WorldRenderPrimitive>.visuals():
            List<PrioritizedWorldRenderPrimitive> =
        filterIsInstance<PrioritizedWorldRenderPrimitive>()

    private fun List<WorldRenderPrimitive>.objects(): List<PlacedObject> =
        mapNotNull { (it as? WorldObjectPrimitive)?.placedObject }

    private fun List<WorldRenderPrimitive>.entities(): List<WorldEntity> =
        mapNotNull { (it as? WorldEntityPrimitive)?.worldEntity }

    private fun List<WorldRenderPrimitive>.indexOfObject(
        placed: PlacedObject
    ): Int = indexOfFirst {
        it is WorldObjectPrimitive && it.placedObject === placed
    }.also { check(it >= 0) }

    private fun List<WorldRenderPrimitive>.indexOfEntity(
        entity: WorldEntity
    ): Int = indexOfFirst {
        it is WorldEntityPrimitive && it.worldEntity === entity
    }.also { check(it >= 0) }
}
