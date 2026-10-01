package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.pathfinding.PathMovementMode
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.math.sqrt
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
    fun `consume reached waypoints defaults to enabled`() {
        assertTrue(DebugSettings().pathfinding.consumeReachedWaypoints)
    }

    @Test
    fun `switching movement mode changes standalone route generation`() {
        val settings = DebugSettings()
        val tool = DebugPathfindingTool(
            world = world(),
            state = settings.worldState,
            movementMode = { settings.pathfinding.movementMode }
        )
        val start = TilePosition(0, 0)
        val goal = TilePosition(2, 2)

        tool.click(start)
        tool.click(goal)
        assertEquals(5, tool.result?.path?.size)
        assertEquals(4f, tool.result?.totalCost)

        tool.clear()
        settings.pathfinding.movementMode = PathMovementMode.EIGHT_WAY
        tool.click(start)
        tool.click(goal)

        assertEquals(
            listOf(start, TilePosition(1, 1), goal),
            tool.result?.path
        )
        assertEquals(2f * sqrt(2f), tool.result?.totalCost)
        assertTrue(tool.result!!.explored.isNotEmpty())
    }

    @Test
    fun `entity assigned multi waypoint route uses selected movement mode`() {
        val settings = DebugSettings().apply {
            pathfinding.movementMode = PathMovementMode.EIGHT_WAY
        }
        val world = world()
        val tool = DebugPathfindingTool(
            world = world,
            state = settings.worldState,
            movementMode = { settings.pathfinding.movementMode }
        )
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))

        tool.selectEntity(entity)
        tool.click(TilePosition(2, 2))
        tool.click(TilePosition(4, 0))

        assertEquals(
            listOf(
                TilePosition(1, 1),
                TilePosition(2, 2),
                TilePosition(3, 1),
                TilePosition(4, 0)
            ),
            entity.remainingPath
        )
        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(2, 2), TilePosition(4, 0)),
            tool.waypoints
        )
        assertEquals(4f * sqrt(2f), tool.result?.totalCost)
    }

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

    @Test
    fun `entity route consumes completed sections and retains the current node`() {
        val world = world()
        val tool = DebugPathfindingTool(world)
        val entity = world.addEntity(TestEntity, EntityPosition.centerOf(TilePosition(0, 0)))
        val a = TilePosition(0, 0)
        val b = TilePosition(2, 0)
        val c = TilePosition(2, 2)
        tool.selectEntity(entity)
        tool.click(b)
        tool.click(c)

        assertEquals(listOf(a, b, c), tool.waypoints)
        assertEquals(a, tool.result?.path?.first())

        world.updateEntities(1f)
        tool.update()

        assertEquals(listOf(b, c), tool.waypoints)
        assertEquals(b, tool.result?.path?.first())
        assertEquals(c, tool.result?.path?.last())

        world.updateEntities(1f)
        tool.update()

        assertEquals(listOf(c), tool.waypoints)
        assertEquals(listOf(c), tool.result?.path)
    }

    @Test
    fun `disabled consumption preserves the complete assigned route`() {
        val world = world()
        val tool = DebugPathfindingTool(
            world = world,
            state = DebugWorldState(),
            consumeReachedWaypoints = { false }
        )
        val entity = world.addEntity(TestEntity, EntityPosition.centerOf(TilePosition(0, 0)))
        val route = listOf(TilePosition(0, 0), TilePosition(2, 0), TilePosition(2, 2))
        tool.selectEntity(entity)
        tool.click(route[1])
        tool.click(route[2])

        world.updateEntities(2f)
        tool.update()

        assertEquals(route, tool.waypoints)
        assertEquals(route.first(), tool.result?.path?.first())
        assertEquals(route.last(), tool.result?.path?.last())
    }

    @Test
    fun `standalone routes do not consume while entities move`() {
        val world = world()
        val tool = DebugPathfindingTool(world)
        val entity = world.addEntity(TestEntity, EntityPosition.centerOf(TilePosition(0, 0)))
        entity.followPath(listOf(TilePosition(0, 0), TilePosition(2, 0)), 2f)
        val route = listOf(TilePosition(0, 1), TilePosition(2, 1), TilePosition(2, 2))
        route.forEach(tool::click)

        world.updateEntities(1f)
        tool.update()

        assertEquals(route, tool.waypoints)
        assertEquals(route.first(), tool.result?.path?.first())
    }

    @Test
    fun `selected entity stays held at its destination and accepts another goal`() {
        val world = world()
        val settings = DebugSettings()
        val tool = DebugPathfindingTool(
            world = world,
            state = settings.worldState,
            entityFreezeState = settings.entityFreezeState
        )
        val entity = world.addEntity(TestEntity, EntityPosition.centerOf(TilePosition(0, 0)))
        tool.selectEntity(entity)
        tool.click(TilePosition(2, 0))

        world.updateEntities(1f)
        tool.update()

        assertFalse(entity.isMoving)
        assertTrue(settings.isEntityHeld(entity))
        assertTrue(settings.worldState.pathfindingEntityWaiting)

        tool.click(TilePosition(2, 2))

        assertTrue(entity.isMoving)
        assertFalse(settings.worldState.pathfindingEntityWaiting)
        assertEquals(TilePosition(2, 1), entity.remainingPath.first())
    }

    @Test
    fun `clear and entity replacement release only pathfinding-owned holds`() {
        val world = world()
        val settings = DebugSettings()
        val tool = DebugPathfindingTool(
            world = world,
            state = settings.worldState,
            entityFreezeState = settings.entityFreezeState
        )
        val first = world.addEntity(TestEntity, EntityPosition.centerOf(TilePosition(0, 0)))
        val second = world.addEntity(TestEntity, EntityPosition.centerOf(TilePosition(3, 3)))
        settings.setEntityFrozen(first, true)

        tool.selectEntity(first)
        assertTrue(settings.isEntityHeld(first))
        tool.selectEntity(second)

        assertTrue(settings.isEntityFrozen(first))
        assertTrue(settings.isEntityHeld(first))
        assertTrue(settings.isEntityHeld(second))

        tool.clear()

        assertFalse(settings.isEntityHeld(second))
        assertTrue(settings.isEntityFrozen(first))
    }

    private fun world() = World(5, 5) { _, _ -> TestTile }
}
