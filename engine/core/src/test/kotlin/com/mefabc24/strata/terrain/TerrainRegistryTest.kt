package com.mefabc24.strata.terrain

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
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

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
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
    fun `multi file animation queues and preserves every frame in order`() {
        val queued = mutableListOf<String>()
        val textures = mapOf(
            "tiles/water_0.png" to region(32, 24),
            "tiles/water_1.png" to region(32, 24),
            "tiles/water_2.png" to region(32, 24)
        )
        val registry = TerrainRegistry<Terrain>(
            directory = "tiles",
            queueTexture = queued::add,
            regionFor = textures::getValue
        )

        registry.registerAnimated(
            type = Terrain.WATER,
            frames = listOf("water_0.png", "water_1.png", "water_2.png"),
            frameDuration = 0.2f
        )

        assertEquals(textures.keys.toList(), queued)

        registry.freeze()
        registry.prepare()

        val sprite = registry.entries.single().sprite
        assertEquals(3, sprite.frameCount)
        textures.values.forEachIndexed { index, texture ->
            assertSame(texture, sprite.frameAtIndex(index))
        }
        assertSame(textures.values.elementAt(1), registry.frameAt(Terrain.WATER, 0.2f))
    }

    @Test
    fun `multi file animation rejects mismatched dimensions when prepared`() {
        val registry = TerrainRegistry<Terrain>(
            directory = "tiles",
            queueTexture = {},
            regionFor = { path ->
                if (path.endsWith("0.png")) region(32, 24) else region(64, 24)
            }
        )
        registry.registerAnimated(
            Terrain.WATER,
            frames = listOf("water_0.png", "water_1.png"),
            frameDuration = 0.2f
        )

        assertFailsWith<IllegalArgumentException> {
            registry.prepare()
        }
    }

    @Test
    fun `spritesheet animation prepares frames in row major order`() {
        val pixmap = Pixmap(48, 24, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()

        try {
            val registry = TerrainRegistry<Terrain>(
                directory = "tiles",
                queueTexture = {},
                regionFor = { TextureRegion(texture) }
            )
            registry.registerAnimated(
                type = Terrain.WATER,
                spriteSheet = "water.png",
                frameWidth = 16,
                frameHeight = 12,
                frameCount = 5,
                frameDuration = 0.1f
            )
            registry.prepare()

            val frames = registry.entries.single().sprite
            assertEquals(5, frames.frameCount)
            assertEquals(
                listOf(0 to 0, 16 to 0, 32 to 0, 0 to 12, 16 to 12),
                List(frames.frameCount) { index ->
                    frames.frameAtIndex(index).let { it.regionX to it.regionY }
                }
            )
        } finally {
            texture.dispose()
        }
    }

    @Test
    fun `invalid animated registrations and sheet configurations fail clearly`() {
        val registry = registry()

        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated(Terrain.WATER, emptyList(), 0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated(Terrain.WATER, listOf(" "), 0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated(Terrain.WATER, listOf("water.png"), 0f)
        }
        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated(
                Terrain.WATER,
                spriteSheet = "water.png",
                frameWidth = 0,
                frameHeight = 16,
                frameDuration = 0.1f
            )
        }
        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated(
                Terrain.WATER,
                spriteSheet = "water.png",
                frameWidth = 16,
                frameHeight = 0,
                frameDuration = 0.1f
            )
        }
        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated(
                Terrain.WATER,
                spriteSheet = "water.png",
                frameWidth = 16,
                frameHeight = 16,
                frameDuration = 0.1f,
                frameCount = 0
            )
        }

        val invalidSheet = TerrainRegistry<Terrain>(
            directory = "",
            queueTexture = {},
            regionFor = { region(30, 32) }
        )
        invalidSheet.registerAnimated(
            Terrain.WATER,
            spriteSheet = "water.png",
            frameWidth = 16,
            frameHeight = 16,
            frameDuration = 0.1f
        )

        assertFailsWith<IllegalArgumentException> {
            invalidSheet.prepare()
        }

        val excessiveCount = TerrainRegistry<Terrain>(
            directory = "",
            queueTexture = {},
            regionFor = { region(32, 32) }
        )
        excessiveCount.registerAnimated(
            Terrain.WATER,
            spriteSheet = "water.png",
            frameWidth = 16,
            frameHeight = 16,
            frameDuration = 0.1f,
            frameCount = 5
        )

        assertFailsWith<IllegalArgumentException> {
            excessiveCount.prepare()
        }
    }

    @Test
    fun `maximum visual height uses registered terrain sprites`() {
        val grass = region(width = 32, height = 24)
        val water0 = region(width = 16, height = 24)
        val water1 = region(width = 16, height = 24)
        val registry = TerrainRegistry<Terrain>(
            directory = "tiles",
            queueTexture = {},
            regionFor = { path ->
                when {
                    path.endsWith("water_0.png") -> water0
                    path.endsWith("water_1.png") -> water1
                    else -> grass
                }
            }
        )

        registry.register(Terrain.GRASS, "grass.png")
        registry.registerAnimated(
            Terrain.WATER,
            frames = listOf("water_0.png", "water_1.png"),
            frameDuration = 0.1f
        )
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
