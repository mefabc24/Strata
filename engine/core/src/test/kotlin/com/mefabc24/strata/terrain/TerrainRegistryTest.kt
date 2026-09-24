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
    fun `cliff sprites are queued and prepared with their terrain`() {
        val queued = mutableListOf<String>()
        val grass = TextureRegion()
        val dirtLeft = TextureRegion()
        val dirtRight = TextureRegion()
        val regions = mapOf(
            "tiles/grass.png" to grass,
            "tiles/dirt-left.png" to dirtLeft,
            "tiles/dirt-right.png" to dirtRight
        )
        val registry = TerrainRegistry<Terrain>(
            directory = "tiles",
            queueTexture = queued::add,
            regionFor = regions::getValue
        )

        registry.register(Terrain.GRASS, "grass.png") {
            cliffs {
                left = "dirt-left.png"
                right = "dirt-right.png"
            }
        }

        assertEquals(
            listOf(
                "tiles/grass.png",
                "tiles/dirt-left.png",
                "tiles/dirt-right.png"
            ),
            queued
        )

        registry.freeze()
        registry.prepare()

        assertSame(dirtLeft, registry.cliffs(Terrain.GRASS)?.left)
        assertSame(dirtRight, registry.cliffs(Terrain.GRASS)?.right)
    }

    @Test
    fun `terrain types retain their own optional cliff materials`() {
        val regions = mutableMapOf<String, TextureRegion>()
        val registry = TerrainRegistry<Terrain>(
            directory = "tiles",
            queueTexture = { path -> regions[path] = TextureRegion() },
            regionFor = regions::getValue
        )

        registry.register(Terrain.GRASS, "grass.png") {
            cliffs {
                left = "dirt-left.png"
                right = "dirt-right.png"
            }
        }
        registry.register(Terrain.SAND, "sand.png") {
            cliffs {
                left = "sand-left.png"
                right = "sand-right.png"
            }
        }
        registry.register(Terrain.WATER, "water.png")
        registry.freeze()
        registry.prepare()

        assertSame(
            regions.getValue("tiles/dirt-left.png"),
            registry.cliffs(Terrain.GRASS)?.left
        )
        assertSame(
            regions.getValue("tiles/dirt-right.png"),
            registry.cliffs(Terrain.GRASS)?.right
        )
        assertSame(
            regions.getValue("tiles/sand-left.png"),
            registry.cliffs(Terrain.SAND)?.left
        )
        assertSame(
            regions.getValue("tiles/sand-right.png"),
            registry.cliffs(Terrain.SAND)?.right
        )
        assertEquals(null, registry.cliffs(Terrain.WATER))
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

        assertFailsWith<IllegalArgumentException> {
            registry.register(Terrain.WATER) {
                cliffs.left = " "
            }
        }

        assertFailsWith<IllegalStateException> {
            registry.cliffs(Terrain.UNREGISTERED)
        }
    }

    private fun registry(
        queued: MutableList<String> = mutableListOf()
    ) = TerrainRegistry<Terrain>(
        directory = "tiles",
        queueTexture = queued::add,
        regionFor = { TextureRegion() }
    )
}
