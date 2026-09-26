package com.mefabc24.strata.render.entity

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.render.`object`.AlphaMask
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.WorldEntity
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EntityRegistryTest {

    private class Citizen : Entity
    private class Trader : Entity

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `static entity visual resolves by game entity type`() {
        val queued = mutableListOf<String>()
        val texture = region(20, 30)
        val registry = EntityRegistry(
            directory = "entities",
            queueTexture = queued::add,
            regionFor = { texture },
            loadAlphaMask = { null }
        )

        registry.register<Citizen>("citizen.png") {
            offsetX = 2f
            offsetY = 3f
            width = 16f
            height = 24f
            scale = 1.5f
        }
        registry.freeze()
        registry.prepare()

        assertEquals(listOf("entities/citizen.png"), queued)
        assertTrue(registry.entries.single().isPrepared)

        val runtime = WorldEntity(
            Citizen(),
            EntityPosition(0.5f, 0.5f)
        )
        val visual = requireNotNull(registry.get(runtime))
        assertSame(texture, visual.texture)
        assertEquals(2f, visual.offsetX)
        assertEquals(3f, visual.offsetY)
        assertEquals(16f, visual.width)
        assertEquals(24f, visual.height)
        assertEquals(1.5f, visual.scale)
    }

    @Test
    fun `animated entity visual reuses sprite frames and matching masks`() {
        val textures = mapOf(
            "entities/citizen_0.png" to region(16, 24),
            "entities/citizen_1.png" to region(16, 24)
        )
        val solid = mask(true)
        val transparent = mask(false)
        val masks = mapOf(
            "entities/citizen_0.png" to solid,
            "entities/citizen_1.png" to transparent
        )
        val registry = EntityRegistry(
            directory = "entities",
            queueTexture = {},
            regionFor = textures::getValue,
            loadAlphaMask = masks::get
        )

        registry.registerAnimated<Citizen>(
            frames = listOf("citizen_0.png", "citizen_1.png"),
            frameDuration = 0.25f
        )
        registry.freeze()
        registry.prepare()

        val visual = registry.entries.single().visual
        assertEquals(2, visual.sprite.frameCount)
        assertSame(textures.values.first(), visual.frameAt(0f).texture)
        assertSame(solid, visual.frameAt(0f).alphaMask)
        assertSame(textures.values.last(), visual.frameAt(0.25f).texture)
        assertSame(transparent, visual.frameAt(0.25f).alphaMask)
    }

    @Test
    fun `registry rejects duplicate late and invalid registrations`() {
        val registry = EntityRegistry(
            directory = "",
            queueTexture = {},
            regionFor = { region(1, 1) },
            loadAlphaMask = { null }
        )

        registry.register<Citizen>("citizen.png")
        assertFailsWith<IllegalArgumentException> {
            registry.register<Citizen>("other.png")
        }
        assertFailsWith<IllegalArgumentException> {
            registry.register<Trader>("trader.png") { scale = 0f }
        }

        registry.freeze()
        assertFailsWith<IllegalStateException> {
            registry.register<Trader>("trader.png")
        }
    }

    private fun region(width: Int, height: Int): TextureRegion {
        return object : TextureRegion() {
            override fun getRegionWidth(): Int = width
            override fun getRegionHeight(): Int = height
        }
    }

    private fun mask(solid: Boolean): AlphaMask {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        if (solid) {
            pixmap.setColor(1f, 1f, 1f, 1f)
            pixmap.drawPixel(0, 0)
        }
        return try {
            AlphaMask.fromPixmap(pixmap)
        } finally {
            pixmap.dispose()
        }
    }
}
