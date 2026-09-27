package com.mefabc24.sandbox

import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class SandboxEntitySpawnerTest {
    @Test
    fun `selected entry spawns its entity at the tile center`() {
        val fixture = fixture()
        fixture.tools.select(SandboxMode.SPAWN)
        fixture.spawner.selectedEntry = fixture.spawner.entries.last()

        val spawned = requireNotNull(
            fixture.spawner.spawn(TilePosition(2, 3))
        )

        assertIs<TestEntityB>(spawned.entity)
        assertEquals(
            EntityPosition.centerOf(TilePosition(2, 3)),
            spawned.position
        )
    }

    @Test
    fun `spawning is disabled outside spawn mode and without a selection`() {
        val fixture = fixture()

        assertNull(fixture.spawner.spawn(TilePosition(1, 1)))
        assertEquals(0, fixture.world.getEntities().size)

        fixture.tools.select(SandboxMode.SPAWN)
        fixture.spawner.selectedEntry = null

        assertNull(fixture.spawner.spawn(TilePosition(1, 1)))
        assertNull(fixture.spawner.spawn(TilePosition(-1, 1)))
        assertEquals(0, fixture.world.getEntities().size)
    }

    @Test
    fun `multiple clicks create independent runtime entities`() {
        val fixture = fixture()
        fixture.tools.select(SandboxMode.SPAWN)

        val first = fixture.spawner.spawn(TilePosition(1, 1))
        val second = fixture.spawner.spawn(TilePosition(3, 2))

        assertNotSame(first, second)
        assertNotSame(first?.entity, second?.entity)
        assertEquals(2, fixture.world.getEntities().size)
        assertEquals(
            setOf(
                EntityPosition.centerOf(TilePosition(1, 1)),
                EntityPosition.centerOf(TilePosition(3, 2))
            ),
            fixture.world.getEntities().map { it.position }.toSet()
        )
    }

    private fun fixture(): Fixture {
        val world = World(5, 5) { _, _ -> TestTile }
        val painter = SandboxTerrainPainter(world)
        val placement = PlacementController(world)
        val buildDrag = SandboxBuildDragController(placement)
        val tools = SandboxToolController(painter, placement, buildDrag)
        val entries = listOf(
            SandboxSpawnEntry("Entity A", ::TestEntityA),
            SandboxSpawnEntry("Entity B", ::TestEntityB)
        )
        return Fixture(
            world,
            tools,
            SandboxEntitySpawner(world, tools, entries)
        )
    }

    private data class Fixture(
        val world: World,
        val tools: SandboxToolController,
        val spawner: SandboxEntitySpawner
    )

    private data object TestTile : Tile
    private class TestEntityA : Entity
    private class TestEntityB : Entity
}
