package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class DebugEntitySpawnerTest {
    private data object TestTile : Tile
    private class EntityA : Entity
    private class EntityB : Entity

    @Test
    fun `selection creates through registry entry at tile center`() {
        val world = World(4, 4) { _, _ -> TestTile }
        val registry = registry()
        var active = true
        val spawned = mutableListOf<Entity>()
        val spawner = DebugEntitySpawner(
            world, registry.spawnableEntries, { active }
        ) { spawned += it.entity }
        spawner.selectedEntry = spawner.entries.last()

        val result = requireNotNull(spawner.spawn(TilePosition(2, 3)))
        assertIs<EntityB>(result.entity)
        assertEquals(EntityPosition.centerOf(TilePosition(2, 3)), result.position)
        assertEquals(1, spawned.size)
        assertSame(result.entity, spawned.single())

        active = false
        assertNull(spawner.spawn(TilePosition(1, 1)))
        active = true
        assertNull(spawner.spawn(TilePosition(-1, 1)))
    }

    private fun registry() = EntityRegistry(
        directory = "",
        queueTexture = {},
        regionFor = { TextureRegion() },
        loadAlphaMask = { null }
    ).apply {
        register<EntityA>("a", factory = ::EntityA)
        register<EntityB>("b", factory = ::EntityB)
    }
}
