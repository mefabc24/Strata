package com.mefabc24.strata.terrain

import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TerrainRegistryTest {

    private enum class Terrain {
        GRASS,
        WATER,
        SAND,
        UNREGISTERED
    }

    @Test
    fun `entries enumerate only registrations in declaration order`() {
        val queued = mutableListOf<String>()
        val registry = registry(queued)

        registry.register(Terrain.WATER, "water.png")
        registry.register(Terrain.GRASS, "grass.png")

        assertEquals(
            listOf(Terrain.WATER, Terrain.GRASS),
            registry.entries.map(TerrainEntry<Terrain>::type)
        )

        assertEquals(
            listOf("tiles/water.png", "tiles/grass.png"),
            queued
        )

        assertFalse(
            registry.entries.any {
                it.type == Terrain.UNREGISTERED
            }
        )
    }

    @Test
    fun `entry snapshots cannot mutate registry contents`() {
        val registry = registry()
        registry.register(Terrain.GRASS)
        registry.register(Terrain.WATER)

        val snapshot = registry.entries as MutableList<TerrainEntry<Terrain>>
        snapshot.clear()

        assertEquals(
            listOf(Terrain.GRASS, Terrain.WATER),
            registry.entries.map(TerrainEntry<Terrain>::type)
        )
    }

    @Test
    fun `prepare exposes the registry-owned texture region`() {
        val texture = TextureRegion()
        val registry = TerrainRegistry<Terrain>(
            directory = "tiles",
            queueTexture = {},
            regionFor = { texture }
        )

        registry.register(Terrain.GRASS)
        val entry = registry.entries.single()

        assertFalse(entry.isPrepared)
        assertFailsWith<IllegalStateException> {
            entry.texture
        }

        registry.freeze()
        registry.prepare()

        assertTrue(entry.isPrepared)
        assertSame(texture, entry.texture)
        assertSame(texture, registry[Terrain.GRASS])
    }

    @Test
    fun `maximum visual height uses registered terrain sprites`() {
        val grass = region(width = 32, height = 24)
        val water = region(width = 32, height = 48)
        val registry = TerrainRegistry<Terrain>(
            directory = "tiles",
            queueTexture = {},
            regionFor = { path ->
                if (path.endsWith("water.png")) water else grass
            }
        )

        registry.register(Terrain.GRASS, "grass.png")
        registry.register(Terrain.WATER, "water.png")
        registry.freeze()
        registry.prepare()

        assertEquals(48f, registry.maxSpriteHeight(tileWidth = 32f))
    }

    @Test
    fun `frozen registry rejects registration and keeps prepared entries readable`() {
        val texture = TextureRegion()
        val registry = TerrainRegistry<Terrain>(
            directory = "tiles",
            queueTexture = {},
            regionFor = { texture }
        )

        registry.register(Terrain.GRASS)
        registry.freeze()

        val failure = assertFailsWith<IllegalStateException> {
            registry.register(Terrain.WATER)
        }

        assertEquals(
            "Terrain registry registration is already closed.",
            failure.message
        )

        registry.prepare()

        assertEquals(listOf(Terrain.GRASS), registry.entries.map { it.type })
        assertSame(texture, registry[Terrain.GRASS])
    }

    @Test
    fun `duplicate and blank registrations are rejected`() {
        val registry = registry()
        registry.register(Terrain.GRASS)

        assertFailsWith<IllegalArgumentException> {
            registry.register(Terrain.GRASS, "other.png")
        }

        assertFailsWith<IllegalArgumentException> {
            registry.register(Terrain.WATER, " ")
        }

        assertFailsWith<IllegalStateException> {
            registry[Terrain.UNREGISTERED]
        }

    }

    private fun registry(
        queued: MutableList<String> = mutableListOf()
    ) = TerrainRegistry<Terrain>(
        directory = "tiles",
        queueTexture = queued::add,
        regionFor = { TextureRegion() }
    )

    private fun region(
        width: Int,
        height: Int
    ): TextureRegion {
        return object : TextureRegion() {
            override fun getRegionWidth(): Int = width
            override fun getRegionHeight(): Int = height
        }
    }
}
