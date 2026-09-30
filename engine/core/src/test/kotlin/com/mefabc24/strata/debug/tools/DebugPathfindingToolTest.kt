package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugPathfindingToolTest {
    private data object TestTile : Tile
    private data object TestEntity : Entity

    @Test
    fun `standalone clicks append waypoints and update the combined path`() {
        val tool = DebugPathfindingTool(world())
        val a = TilePosition(0, 0)
        val b = TilePosition(2, 0)
        val c = TilePosition(2, 2)

        assertTrue(tool.click(a))
        assertTrue(tool.click(b))
        assertTrue(tool.click(c))

        assertEquals(listOf(a, b, c), tool.waypoints)
        assertEquals(
            listOf(
                a,
                TilePosition(1, 0),
                b,
                TilePosition(2, 1),
                c
            ),
            tool.result?.path
        )
        assertTrue(tool.result!!.explored.isNotEmpty())
    }

    @Test
    fun `clear resets waypoints result and selected entity`() {
        val world = world()
        val state = DebugWorldState()
        val tool = DebugPathfindingTool(world, state)
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        tool.selectEntity(entity)
        tool.click(TilePosition(2, 0))

        assertTrue(tool.clear())

        assertTrue(tool.waypoints.isEmpty())
        assertNull(tool.result)
        assertNull(tool.selectedEntity)
        assertTrue(state.pathfindingWaypoints.isEmpty())
        assertNull(state.pathfindingEntity)
    }

    @Test
    fun `selecting entity uses its current tile as route start`() {
        val world = world()
        val tool = DebugPathfindingTool(world)
        val entity = world.addEntity(TestEntity, EntityPosition(1.2f, 2.8f))

        assertTrue(tool.selectEntity(entity))

        assertSame(entity, tool.selectedEntity)
        assertEquals(listOf(TilePosition(1, 2)), tool.waypoints)
        assertNull(tool.result)
    }

    @Test
    fun `entity speed multiplier scales the debug fallback speed`() {
        listOf(
            0.5f to 1f,
            1f to 2f,
            2f to 4f
        ).forEach { (multiplier, expectedSpeed) ->
            val world = world()
            val tool = DebugPathfindingTool(
                world = world,
                state = DebugWorldState(),
                entitySpeedMultiplier = { multiplier }
            )
            val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))

            tool.selectEntity(entity)
            tool.click(TilePosition(2, 0))

            assertEquals(expectedSpeed, entity.movementSpeed, "${multiplier}x")
        }
    }

    @Test
    fun `pending entity start follows its live tile and starts the first search there`() {
        val world = world()
        val tool = DebugPathfindingTool(world)
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        entity.followPath(listOf(TilePosition(4, 0)), speed = 2f)
        tool.selectEntity(entity)
        assertEquals(listOf(TilePosition(0, 0)), tool.waypoints)
        world.updateEntities(0.5f)

        tool.update()

        assertEquals(listOf(TilePosition(1, 0)), tool.waypoints)
        tool.click(TilePosition(1, 2))
        assertEquals(TilePosition(1, 0), tool.result?.start)
        assertEquals(TilePosition(1, 0), tool.result?.path?.first())
    }

    @Test
    fun `committed entity waypoint history does not follow later movement`() {
        val world = world()
        val tool = DebugPathfindingTool(world)
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        tool.selectEntity(entity)
        tool.click(TilePosition(4, 0))
        val committedWaypoints = tool.waypoints

        world.updateEntities(0.5f)
        tool.update()

        assertEquals(TilePosition(1, 0), entity.currentTile)
        assertEquals(committedWaypoints, tool.waypoints)
    }

    @Test
    fun `standalone waypoint state remains static during synchronization`() {
        val world = world()
        val tool = DebugPathfindingTool(world)
        val movingEntity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        movingEntity.followPath(listOf(TilePosition(4, 0)), speed = 2f)
        tool.click(TilePosition(2, 2))

        world.updateEntities(0.5f)
        tool.update()

        assertEquals(listOf(TilePosition(2, 2)), tool.waypoints)
        assertNull(tool.selectedEntity)
    }

    @Test
    fun `first entity destination replaces its route and preserves movement speed`() {
        val world = world()
        val tool = DebugPathfindingTool(world)
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        entity.followPath(listOf(TilePosition(4, 0)), speed = 2f)

        tool.selectEntity(entity)
        tool.click(TilePosition(0, 2))

        assertEquals(
            listOf(TilePosition(0, 1), TilePosition(0, 2)),
            entity.remainingPath
        )
        assertEquals(2f, entity.movementSpeed)
        assertEquals(2f, tool.result?.totalCost)
    }

    @Test
    fun `later entity destinations extend the live remaining route`() {
        val world = world()
        val tool = DebugPathfindingTool(world)
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        tool.selectEntity(entity)
        tool.click(TilePosition(2, 0))
        world.updateEntities(0.5f)

        tool.click(TilePosition(2, 2))

        assertEquals(
            listOf(
                TilePosition(2, 0),
                TilePosition(2, 1),
                TilePosition(2, 2)
            ),
            entity.remainingPath
        )
        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(2, 0), TilePosition(2, 2)),
            tool.waypoints
        )
        assertEquals(2f, entity.movementSpeed)
    }

    @Test
    fun `assigning a route does not change frozen state`() {
        val world = world()
        val settings = DebugSettings()
        val tool = DebugPathfindingTool(world, settings.worldState)
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        settings.setEntityFrozen(entity, true)

        tool.selectEntity(entity)
        tool.click(TilePosition(2, 0))

        assertTrue(settings.isEntityFrozen(entity))
        assertTrue(entity.isMoving)
    }

    @Test
    fun `failed entity segment leaves the previous valid route intact`() {
        val world = world()
        var blocked = emptySet<TilePosition>()
        val tool = DebugPathfindingTool(
            world = world,
            state = DebugWorldState(),
            canEnter = { it !in blocked }
        )
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        tool.selectEntity(entity)
        tool.click(TilePosition(2, 0))
        val validRoute = entity.remainingPath
        blocked = (0 until 5).mapTo(mutableSetOf()) { TilePosition(it, 1) }

        tool.click(TilePosition(2, 2))

        assertFalse(tool.result!!.success)
        assertEquals(validRoute, entity.remainingPath)
        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(2, 0)),
            tool.waypoints
        )
    }

    @Test
    fun `selecting a different entity resets pending route state`() {
        val world = world()
        val tool = DebugPathfindingTool(world)
        val first = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val second = world.addEntity(TestEntity, EntityPosition(3.5f, 3.5f))
        tool.selectEntity(first)
        tool.click(TilePosition(2, 0))

        tool.selectEntity(second)

        assertSame(second, tool.selectedEntity)
        assertEquals(listOf(TilePosition(3, 3)), tool.waypoints)
        assertNull(tool.result)
        assertTrue(first.isMoving)
    }

    private fun world() = World(5, 5) { _, _ -> TestTile }
}
