package com.mefabc24.strata.render.entity

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.render.sprite.AlphaMask
import com.mefabc24.strata.render.sprite.SpriteSource
import com.mefabc24.strata.render.sprite.VisualStateId
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.EntityDirection
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

    private enum class WolfState : VisualStateId {
        RESTING,
        MOVING,
        UNKNOWN
    }

    private class Wolf(var state: WolfState) : Entity

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
        assertSame(visual, registry.entries.single().selectionVisual)
        assertSame(texture, visual.texture)
        assertEquals(2f, visual.offsetX)
        assertEquals(3f, visual.offsetY)
        assertEquals(16f, visual.width)
        assertEquals(24f, visual.height)
        assertEquals(1.5f, visual.scale)
    }

    @Test
    fun `spawnable entries expose optional validated factories`() {
        val registry = registry()
        registry.register<Citizen>("citizen.png", factory = ::Citizen)
        registry.register<Trader>("trader.png")
        assertEquals(listOf(Citizen::class), registry.spawnableEntries.map { it.type })
        assertTrue(registry.spawnableEntries.single().create() is Citizen)
        assertFailsWith<IllegalStateException> { registry.entries.last().create() }

        @Suppress("UNCHECKED_CAST")
        val invalidFactory = ({ Trader() } as () -> Citizen)
        val invalid = registry()
        invalid.register<Citizen>("citizen.png", factory = invalidFactory)
        assertFailsWith<IllegalStateException> {
            invalid.spawnableEntries.single().create()
        }
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
    fun `atlas entity registrations preserve settings frames and masks`() {
        val pixmap = Pixmap(6, 2, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val atlas = TextureAtlas()
        atlas.addRegion("citizen", texture, 0, 0, 2, 2)
        atlas.addRegion("trader", texture, 2, 0, 2, 2).index = 0
        atlas.addRegion("trader", texture, 4, 0, 2, 2).index = 1
        val queued = mutableListOf<String>()
        val firstMask = mask(true)
        val secondMask = mask(false)

        try {
            val registry = EntityRegistry(
                directory = "entities",
                queueTexture = {},
                regionFor = { TextureRegion() },
                loadAlphaMask = { null },
                queueAtlas = queued::add,
                atlasFor = { atlas },
                loadAtlasAlphaMasks = { sources ->
                    sources.associateWith { source ->
                        when (source) {
                            is SpriteSource.AtlasRegion -> listOf(firstMask)
                            is SpriteSource.AtlasAnimation -> {
                                listOf(firstMask, secondMask)
                            }
                            else -> error("Unexpected source")
                        }
                    }
                }
            )
            registry.registerAtlas<Citizen>(
                "atlas/world.atlas",
                "citizen"
            ) { scale = 1.25f }
            registry.registerAnimatedAtlas<Trader>(
                "atlas/world.atlas",
                "trader",
                0.2f
            )
            registry.prepare()

            assertEquals(listOf("atlas/world.atlas", "atlas/world.atlas"), queued)
            assertEquals(1.25f, registry.entries.first().visual.scale)
            val animated = registry.entries.last().visual
            assertEquals(2, animated.sprite.frameCount)
            assertSame(secondMask, animated.frameAt(0.2f).alphaMask)
            assertEquals(4, animated.frameAt(0.2f).texture.regionX)
        } finally {
            atlas.dispose()
        }
    }

    @Test
    fun `game-defined entity state resolves with independent local playback`() {
        val textures = mapOf(
            "entities/rest.png" to region(16, 24),
            "entities/move_0.png" to region(20, 30),
            "entities/move_1.png" to region(20, 30)
        )
        val registry = EntityRegistry(
            directory = "entities",
            queueTexture = {},
            regionFor = textures::getValue,
            loadAlphaMask = { null }
        )
        registry.registerStateful<Wolf>(
            stateFor = { _, wolf -> wolf.state }
        ) {
            state(WolfState.RESTING) { sprite("rest.png") }
            state(WolfState.MOVING) {
                animated(
                    listOf("move_0.png", "move_1.png"),
                    frameDuration = 0.1f
                )
            }
        }
        registry.prepare()

        assertSame(
            textures.getValue("entities/rest.png"),
            registry.entries.single().selectionVisual.texture
        )

        val wolf = Wolf(WolfState.MOVING)
        val runtime = WorldEntity(wolf, EntityPosition(0.5f, 0.5f))
        assertSame(
            textures.getValue("entities/move_0.png"),
            registry.resolve(runtime, 4f)?.frame?.texture
        )
        assertSame(
            textures.getValue("entities/move_1.png"),
            registry.resolve(runtime, 4.11f)?.frame?.texture
        )

        wolf.state = WolfState.RESTING
        assertSame(
            textures.getValue("entities/rest.png"),
            registry.resolve(runtime, 5f)?.frame?.texture
        )
        wolf.state = WolfState.MOVING
        assertSame(
            textures.getValue("entities/move_0.png"),
            registry.resolve(runtime, 6f)?.frame?.texture
        )

        val other = WorldEntity(
            Wolf(WolfState.MOVING),
            EntityPosition(1.5f, 1.5f)
        )
        assertSame(
            textures.getValue("entities/move_0.png"),
            registry.resolve(other, 6.11f)?.frame?.texture
        )
        assertSame(
            textures.getValue("entities/move_1.png"),
            registry.resolve(runtime, 6.11f)?.frame?.texture
        )

        wolf.state = WolfState.UNKNOWN
        val failure = assertFailsWith<IllegalStateException> {
            registry.resolve(runtime, 7f)
        }
        assertTrue(failure.message.orEmpty().contains("UNKNOWN"))
    }

    @Test
    fun `stateful entity can define representative visual state`() {
        val textures = mapOf(
            "entities/rest.png" to region(16, 24),
            "entities/move.png" to region(16, 24)
        )

        val registry = EntityRegistry(
            directory = "entities",
            queueTexture = {},
            regionFor = textures::getValue,
            loadAlphaMask = { null }
        )

        registry.registerStateful<Wolf>(
            stateFor = { _, wolf -> wolf.state }
        ) {
            state(WolfState.RESTING) {
                sprite("rest.png")
            }

            state(WolfState.MOVING) {
                sprite("move.png")
            }

            representativeState(WolfState.MOVING)
        }

        registry.prepare()

        assertSame(
            textures.getValue("entities/move.png"),
            registry.entries.single().selectionVisual.texture
        )
    }

    @Test
    fun `directional sheet uses arbitrary game-defined row order`() {
        val pixmap = Pixmap(32, 40, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val masks = List(8) { mask(it % 2 == 0) }
        val queued = mutableListOf<String>()

        try {
            val registry = EntityRegistry(
                directory = "entities",
                queueTexture = queued::add,
                regionFor = { TextureRegion(texture) },
                loadAlphaMask = { null },
                loadSpriteSheetAlphaMasks = { _, _, _, _ -> masks }
            )
            registry.registerDirectional<Wolf> {
                directionalSpriteSheet(
                    path = "wolf.png",
                    frameWidth = 16,
                    frameHeight = 10,
                    frameDuration = 0.1f,
                    framesPerDirection = 2,
                    directionRows = mapOf(
                        EntityDirection.SOUTH_WEST to 2,
                        EntityDirection.SOUTH_EAST to 0,
                        EntityDirection.NORTH_WEST to 3,
                        EntityDirection.NORTH_EAST to 1
                    )
                )
            }
            registry.prepare()

            assertEquals(listOf("entities/wolf.png"), queued)
            val runtime = WorldEntity(
                Wolf(WolfState.RESTING),
                EntityPosition(0.5f, 0.5f)
            )
            val expectedRows = mapOf(
                EntityDirection.SOUTH_WEST to 20,
                EntityDirection.SOUTH_EAST to 0,
                EntityDirection.NORTH_WEST to 30,
                EntityDirection.NORTH_EAST to 10
            )
            expectedRows.forEach { (direction, rowY) ->
                runtime.face(direction)
                val first = requireNotNull(registry.resolve(runtime, 0f))
                assertEquals(0, first.frame.texture.regionX)
                assertEquals(rowY, first.frame.texture.regionY)

                val second = requireNotNull(registry.resolve(runtime, 0.11f))
                assertEquals(16, second.frame.texture.regionX)
                assertEquals(rowY, second.frame.texture.regionY)
                val row = rowY / 10
                assertSame(masks[row * 2 + 1], second.frame.alphaMask)
            }
        } finally {
            texture.dispose()
        }
    }

    @Test
    fun `entity state and direction select separate visual dimensions`() {
        val textures = EntityDirection.entries.flatMap { direction ->
            listOf(
                "entities/rest-$direction.png" to region(10, 20),
                "entities/move-$direction.png" to region(30, 40)
            )
        }.toMap()
        val registry = EntityRegistry(
            directory = "entities",
            queueTexture = {},
            regionFor = textures::getValue,
            loadAlphaMask = { null }
        )
        registry.registerStateful<Wolf>(
            stateFor = { _, wolf -> wolf.state }
        ) {
            state(WolfState.RESTING) {
                EntityDirection.entries.forEach { direction ->
                    direction(direction) { sprite("rest-$direction.png") }
                }
            }
            state(WolfState.MOVING) {
                EntityDirection.entries.forEach { direction ->
                    direction(direction) { sprite("move-$direction.png") }
                }
            }
        }
        registry.prepare()

        val wolf = Wolf(WolfState.RESTING)
        val runtime = WorldEntity(wolf, EntityPosition(0.5f, 0.5f))
        runtime.face(EntityDirection.NORTH_WEST)
        assertSame(
            textures.getValue("entities/rest-NORTH_WEST.png"),
            registry.resolve(runtime, 2f)?.frame?.texture
        )

        wolf.state = WolfState.MOVING
        runtime.face(EntityDirection.SOUTH_WEST)
        val resolved = requireNotNull(registry.resolve(runtime, 3f))
        assertSame(
            textures.getValue("entities/move-SOUTH_WEST.png"),
            resolved.frame.texture
        )
        assertEquals(30, resolved.frame.texture.regionWidth)
        assertEquals(40, resolved.frame.texture.regionHeight)
        val bounds = IsoEntityBounds.calculate(
            IsoProjection(TileGeometry(32f, 24f)),
            runtime,
            resolved,
            Rectangle()
        )
        assertEquals(30f, bounds.width)
        assertEquals(40f, bounds.height)
    }

    @Test
    fun `directional registrations validate directions rows and sheet cells`() {
        fun registry() = EntityRegistry(
            directory = "",
            queueTexture = {},
            regionFor = { region(32, 40) },
            loadAlphaMask = { null },
            loadSpriteSheetAlphaMasks = { _, _, _, _ -> List(8) { null } }
        )

        assertFailsWith<IllegalArgumentException> {
            registry().registerDirectional<Wolf> {
                direction(EntityDirection.NORTH_EAST) { sprite("ne.png") }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            registry().registerDirectional<Wolf> {
                directionalSpriteSheet(
                    "wolf.png",
                    16,
                    10,
                    0.1f,
                    EntityDirection.entries.associateWith { 0 }
                )
            }
        }
        val invalidCount = registry()
        invalidCount.registerDirectional<Wolf> {
            directionalSpriteSheet(
                "wolf.png",
                16,
                10,
                0.1f,
                EntityDirection.entries.withIndex().associate { it.value to it.index },
                framesPerDirection = 3
            )
        }
        assertFailsWith<IllegalArgumentException> { invalidCount.prepare() }

        val invalidRow = registry()
        invalidRow.registerDirectional<Wolf> {
            directionalSpriteSheet(
                "wolf.png",
                16,
                10,
                0.1f,
                mapOf(
                    EntityDirection.NORTH_EAST to 4,
                    EntityDirection.SOUTH_EAST to 1,
                    EntityDirection.SOUTH_WEST to 2,
                    EntityDirection.NORTH_WEST to 3
                )
            )
        }
        assertFailsWith<IllegalArgumentException> { invalidRow.prepare() }
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

    private fun registry() = EntityRegistry(
        directory = "entities",
        queueTexture = {},
        regionFor = { region(16, 16) },
        loadAlphaMask = { null }
    )

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
