package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.placement.PlacementEntityPreviewSettings
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.render.preview.EntityPreviewVisual
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
import kotlin.test.assertTrue

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

    @Test
    fun `hover preview uses selection visual without creating an entity`() {
        val world = World(4, 4) { _, _ -> TestTile }
        var factoryCalls = 0
        val registry = EntityRegistry(
            directory = "",
            queueTexture = {},
            regionFor = { TextureRegion() },
            loadAlphaMask = { null }
        ).apply {
            register<EntityA>("a", factory = {
                factoryCalls++
                EntityA()
            })
            prepare()
        }
        val entry = registry.spawnableEntries.first()
        val previewSettings = PlacementEntityPreviewSettings().apply {
            validColor = Color(0.2f, 0.4f, 0.6f, 0.8f)
            invalidColor = Color(0.9f, 0.3f, 0.1f, 0.7f)
        }
        val spawner = DebugEntitySpawner(
            world = world,
            entries = listOf(entry),
            isActive = { true },
            previewSettings = previewSettings
        )

        spawner.update(TilePosition(2, 3))

        val validPreview = requireNotNull(spawner.preview)
        assertEquals(EntityPosition(2.5f, 3.5f), validPreview.position)
        assertTrue(validPreview.valid)
        assertEquals(previewSettings.validColor, validPreview.style.validColor)
        assertSame(
            entry.selectionVisual,
            (validPreview.visual as EntityPreviewVisual.Representative).value
        )
        assertEquals(0, factoryCalls)
        assertTrue(world.getEntities().isEmpty())

        spawner.update(TilePosition(-1, 3))

        val invalidPreview = requireNotNull(spawner.preview)
        assertEquals(EntityPosition(-0.5f, 3.5f), invalidPreview.position)
        assertEquals(false, invalidPreview.valid)
        assertEquals(previewSettings.invalidColor, invalidPreview.style.invalidColor)
        assertEquals(0, factoryCalls)
        assertTrue(world.getEntities().isEmpty())
    }

    @Test
    fun `disabled entity previews leave spawning available`() {
        val world = World(4, 4) { _, _ -> TestTile }
        val registry = registry().apply { prepare() }
        val spawner = DebugEntitySpawner(
            world = world,
            entries = registry.spawnableEntries,
            isActive = { true },
            previewSettings = PlacementEntityPreviewSettings().apply {
                enabled = false
            }
        )

        spawner.update(TilePosition(1, 1))

        assertNull(spawner.preview)
        assertIs<EntityA>(requireNotNull(spawner.spawn(TilePosition(1, 1))).entity)
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
