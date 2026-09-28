package com.mefabc24.sandbox

import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SandboxRoamingControllerTest {
    @Test
    fun `new wolf idles before starting one movement path`() {
        val world = world()
        val wolf = world.addEntity(
            Wolf(),
            EntityPosition.centerOf(TilePosition(10, 10))
        )
        var pathRequests = 0
        val controller = controller(world) { start, goal ->
            pathRequests++
            listOf(start, goal)
        }
        controller.control(wolf)

        repeat(10) {
            controller.update(0f)
        }
        assertFalse(wolf.isMoving)
        assertEquals(0, pathRequests)

        controller.update(1.49f)
        assertFalse(wolf.isMoving)
        assertEquals(0, pathRequests)

        controller.update(0.01f)
        assertTrue(wolf.isMoving)
        assertEquals(1, pathRequests)

        controller.update(10f)
        assertEquals(1, pathRequests)
    }

    @Test
    fun `finishing movement starts another idle period`() {
        val world = world()
        val wolf = world.addEntity(
            Wolf(),
            EntityPosition.centerOf(TilePosition(10, 10))
        )
        var pathRequests = 0
        val controller = controller(world) { start, goal ->
            pathRequests++
            listOf(start, goal)
        }
        controller.control(wolf)
        controller.update(1.5f)
        wolf.cancelMovement()

        controller.update(0.01f)
        controller.update(1.49f)

        assertFalse(wolf.isMoving)
        assertEquals(1, pathRequests)

        controller.update(0.01f)
        assertTrue(wolf.isMoving)
        assertEquals(2, pathRequests)
    }

    @Test
    fun `multiple wolves keep independent behavior state`() {
        val world = world()
        val first = world.addEntity(
            Wolf(),
            EntityPosition.centerOf(TilePosition(10, 10))
        )
        val second = world.addEntity(
            Wolf(),
            EntityPosition.centerOf(TilePosition(12, 12))
        )
        first.followPath(
            listOf(first.currentTile, TilePosition(11, 10)),
            speed = 1f
        )
        var pathRequests = 0
        val controller = controller(world) { start, goal ->
            pathRequests++
            listOf(start, goal)
        }
        controller.control(first)
        controller.control(second)

        controller.update(1.5f)

        assertTrue(first.isMoving)
        assertTrue(second.isMoving)
        assertEquals(1, pathRequests)

        first.cancelMovement()
        controller.update(0f)
        assertFalse(first.isMoving)
        assertTrue(second.isMoving)
        assertEquals(1, pathRequests)
    }

    @Test
    fun `failed destinations are bounded and retried after idling`() {
        val world = world()
        val wolf = world.addEntity(
            Wolf(),
            EntityPosition.centerOf(TilePosition(10, 10))
        )
        var pathRequests = 0
        val controller = controller(world) { _, _ ->
            pathRequests++
            null
        }
        controller.control(wolf)

        controller.update(1.5f)

        assertFalse(wolf.isMoving)
        assertEquals(8, pathRequests)

        controller.update(0f)
        assertEquals(8, pathRequests)
    }

    private fun controller(
        world: World,
        pathFor: (TilePosition, TilePosition) -> List<TilePosition>?
    ): SandboxRoamingController {
        return SandboxRoamingController(
            world = world,
            random = ZeroRandom,
            pathFor = pathFor
        )
    }

    private fun world(): World {
        return World(24, 24) { _, _ -> TestTile }
    }

    private data object TestTile : Tile

    private data object ZeroRandom : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }
}
