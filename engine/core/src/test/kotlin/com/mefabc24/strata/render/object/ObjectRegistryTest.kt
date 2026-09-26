package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.mefabc24.strata.render.sprite.SpriteSource
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.ui.StrataSelectableImageButton
import com.mefabc24.strata.ui.StrataSelectionGroup
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ObjectRegistryTest {

    private class House : Placeable {
        override val footprint = Footprint.square(2)
    }

    private class Tree : Placeable {
        override val footprint = Footprint.square(1)
    }

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `entries preserve registration order and visual metadata`() {
        val queued = mutableListOf<String>()
        val texture = TextureRegion()
        val registry = registry(
            queued = queued,
            texture = texture
        )

        registry.register<House>(
            sprite = "house.png",
            factory = ::House
        ) {
            offsetX = 2f
            offsetY = 3f
            scale = 1.5f
        }

        registry.register<Tree>(
            sprite = "tree.png"
        )

        assertEquals(
            listOf(House::class, Tree::class),
            registry.entries.map(ObjectEntry::type)
        )

        assertEquals(
            listOf("objects/house.png", "objects/tree.png"),
            queued
        )

        val house = registry.entries.first()
        assertEquals("objects/house.png", house.spritePath)
        assertFalse(house.isPrepared)

        registry.freeze()
        registry.prepare()

        assertTrue(house.isPrepared)
        assertSame(texture, house.visual.texture)
        assertEquals(2f, house.visual.offsetX)
        assertEquals(3f, house.visual.offsetY)
        assertEquals(1.5f, house.visual.scale)
    }

    @Test
    fun `multi file animation prepares ordered frames masks settings and factory`() {
        val queued = mutableListOf<String>()
        val textures = mapOf(
            "objects/fountain_0.png" to region(32, 48),
            "objects/fountain_1.png" to region(32, 48),
            "objects/fountain_2.png" to region(32, 48)
        )
        val masks = textures.keys.associateWith { alphaMask() }
        val registry = ObjectRegistry(
            directory = "objects",
            queueTexture = queued::add,
            regionFor = textures::getValue,
            loadAlphaMask = masks::get
        )

        registry.registerAnimated(
            frames = listOf(
                "fountain_0.png",
                "fountain_1.png",
                "fountain_2.png"
            ),
            frameDuration = 0.2f,
            factory = ::House
        ) {
            offsetX = 2f
            offsetY = 3f
            width = 40f
            height = 50f
            scale = 1.5f
        }

        assertEquals(textures.keys.toList(), queued)

        registry.freeze()
        registry.prepare()

        val entry = registry.entries.single()
        val visual = entry.visual
        assertEquals(3, visual.sprite.frameCount)
        textures.values.forEachIndexed { index, texture ->
            assertSame(texture, visual.sprite.frameAtIndex(index))
            assertSame(masks.values.elementAt(index), visual.frameAt(index * 0.2f).alphaMask)
        }
        assertEquals(2f, visual.offsetX)
        assertEquals(3f, visual.offsetY)
        assertEquals(40f, visual.width)
        assertEquals(50f, visual.height)
        assertEquals(1.5f, visual.scale)
        assertNotSame(entry.create(), entry.create())
    }

    @Test
    fun `spritesheet animation prepares row major frames with matching masks`() {
        val pixmap = Pixmap(48, 24, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val masks = List(5) { alphaMask() }

        try {
            val queued = mutableListOf<String>()
            val registry = ObjectRegistry(
                directory = "objects",
                queueTexture = queued::add,
                regionFor = { TextureRegion(texture) },
                loadAlphaMask = { null },
                loadSpriteSheetAlphaMasks = { path, width, height, count ->
                    assertEquals("objects/fountain.png", path)
                    assertEquals(16, width)
                    assertEquals(12, height)
                    assertEquals(5, count)
                    masks
                }
            )
            registry.registerAnimated<House>(
                spriteSheet = "fountain.png",
                frameWidth = 16,
                frameHeight = 12,
                frameCount = 5,
                frameDuration = 0.1f,
                factory = ::House
            )
            registry.prepare()

            assertEquals(listOf("objects/fountain.png"), queued)

            val visual = registry.entries.single().visual
            assertEquals(
                listOf(0 to 0, 16 to 0, 32 to 0, 0 to 12, 16 to 12),
                List(visual.sprite.frameCount) { index ->
                    visual.sprite.frameAtIndex(index).let { it.regionX to it.regionY }
                }
            )
            masks.forEachIndexed { index, mask ->
                assertSame(mask, visual.frameAt(index * 0.1f).alphaMask)
            }
        } finally {
            texture.dispose()
        }
    }

    @Test
    fun `atlas registrations preserve settings factories frames and masks`() {
        val pixmap = Pixmap(6, 2, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val atlas = TextureAtlas()
        atlas.addRegion("house", texture, 0, 0, 2, 2)
        atlas.addRegion("tree", texture, 2, 0, 2, 2).index = 0
        atlas.addRegion("tree", texture, 4, 0, 2, 2).index = 1
        val queued = mutableListOf<String>()
        val firstMask = alphaMask()
        val secondMask = alphaMask()

        try {
            val registry = ObjectRegistry(
                directory = "objects",
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
            registry.registerAtlas<House>(
                atlas = "atlas/world.atlas",
                region = "house",
                factory = ::House
            ) { scale = 1.5f }
            registry.registerAnimatedAtlas<Tree>(
                atlas = "atlas/world.atlas",
                region = "tree",
                frameDuration = 0.2f,
                factory = ::Tree
            )
            registry.prepare()

            assertEquals(listOf("atlas/world.atlas", "atlas/world.atlas"), queued)
            assertEquals("atlas/world.atlas", registry.entries.first().spritePath)
            assertEquals(1.5f, registry.entries.first().visual.scale)
            assertIs<House>(registry.entries.first().create())
            val animated = registry.entries.last().visual
            assertEquals(2, animated.sprite.frameCount)
            assertSame(secondMask, animated.frameAt(0.2f).alphaMask)
            assertEquals(4, animated.frameAt(0.2f).texture.regionX)
        } finally {
            atlas.dispose()
        }
    }

    @Test
    fun `animated object rejects mismatched frame dimensions`() {
        val registry = ObjectRegistry(
            directory = "objects",
            queueTexture = {},
            regionFor = { path ->
                if (path.endsWith("0.png")) region(32, 48) else region(32, 64)
            },
            loadAlphaMask = { null }
        )
        registry.registerAnimated<House>(
            frames = listOf("house_0.png", "house_1.png"),
            frameDuration = 0.1f,
            factory = ::House
        )

        assertFailsWith<IllegalArgumentException> {
            registry.prepare()
        }
    }

    @Test
    fun `frozen registry rejects registration and keeps constructible visuals readable`() {
        val texture = TextureRegion()
        val registry = registry(texture = texture)

        registry.register<House>(
            sprite = "house.png",
            factory = ::House
        )
        registry.freeze()

        val failure = assertFailsWith<IllegalStateException> {
            registry.register<Tree>("tree.png", ::Tree)
        }

        assertEquals(
            "Object registry registration is already closed.",
            failure.message
        )

        registry.prepare()

        val entry = registry.constructibleEntries.single()
        assertSame(texture, entry.visual.texture)
        assertIs<House>(entry.create())
    }

    @Test
    fun `constructible entries use explicit factories only`() {
        val registry = registry()

        registry.register<House>(
            sprite = "house.png",
            factory = ::House
        )

        registry.register<Tree>(
            sprite = "tree.png"
        )

        assertEquals(
            listOf(House::class),
            registry.constructibleEntries.map(ObjectEntry::type)
        )

        assertIs<House>(registry.constructibleEntries.single().create())
        assertFalse(registry.entries.last().isConstructible)

        assertFailsWith<IllegalStateException> {
            registry.entries.last().create()
        }
    }

    @Test
    fun `static alpha masks remain cached by asset path`() {
        var loads = 0
        val registry = ObjectRegistry(
            directory = "objects",
            queueTexture = {},
            regionFor = { TextureRegion() },
            loadAlphaMask = {
                loads++
                null
            }
        )
        registry.register<House>("shared.png")
        registry.register<Tree>("shared.png")

        registry.prepare()

        assertEquals(1, loads)
    }

    @Test
    fun `entry snapshots cannot mutate registry contents`() {
        val registry = registry()
        registry.register<House>("house.png", ::House)
        registry.register<Tree>("tree.png", ::Tree)

        val snapshot = registry.entries as MutableList<ObjectEntry>
        snapshot.clear()

        val constructibleSnapshot =
            registry.constructibleEntries as MutableList<ObjectEntry>
        constructibleSnapshot.removeAt(0)

        assertEquals(
            listOf(House::class, Tree::class),
            registry.entries.map(ObjectEntry::type)
        )

        assertEquals(2, registry.constructibleEntries.size)
    }

    @Test
    fun `registered entries bind directly to image selection controls`() {
        val registry = registry()
        registry.register<House>("house.png", ::House)
        registry.register<Tree>("tree.png", ::Tree)
        registry.freeze()
        registry.prepare()

        val entries = registry.constructibleEntries
        val group = StrataSelectionGroup(entries)
        val skin = Skin().apply {
            add("default", ImageButton.ImageButtonStyle())
        }

        val firstDrawable = TextureRegionDrawable(
            entries.first().visual.texture
        )

        val first = StrataSelectableImageButton(
            drawable = firstDrawable,
            value = entries.first(),
            selectionGroup = group,
            skin = skin
        )

        val second = StrataSelectableImageButton(
            drawable = TextureRegionDrawable(
                entries.last().visual.texture
            ),
            value = entries.last(),
            selectionGroup = group,
            skin = skin
        )

        try {
            assertSame(firstDrawable, first.style.imageUp)
            assertTrue(first.isChecked)

            group.select(entries.last())

            assertFalse(first.isChecked)
            assertTrue(second.isChecked)
            assertIs<Tree>(group.selected?.create())
        } finally {
            first.detach()
            second.detach()
            skin.dispose()
        }
    }

    @Test
    fun `duplicates blank sprites and invalid settings are rejected`() {
        val queued = mutableListOf<String>()
        val registry = registry(queued = queued)

        registry.register<House>("house.png")

        assertFailsWith<IllegalArgumentException> {
            registry.register<House>("other.png")
        }

        assertFailsWith<IllegalArgumentException> {
            registry.register<Tree>(" ")
        }

        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated<Tree>(emptyList(), 0.1f)
        }

        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated<Tree>(listOf(" "), 0.1f)
        }

        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated<Tree>(listOf("tree.png"), Float.NaN)
        }

        assertFailsWith<IllegalArgumentException> {
            registry.registerAnimated<Tree>(
                spriteSheet = "tree.png",
                frameWidth = -1,
                frameHeight = 16,
                frameDuration = 0.1f
            )
        }

        for (
            configure in listOf<ObjectSpriteSettings.() -> Unit>(
                { offsetX = Float.NaN },
                { offsetY = Float.POSITIVE_INFINITY },
                { width = 0f },
                { width = Float.NaN },
                { height = -1f },
                { height = Float.POSITIVE_INFINITY },
                { scale = 0f },
                { scale = Float.NEGATIVE_INFINITY }
            )
        ) {
            val invalidRegistry = registry()

            assertFailsWith<IllegalArgumentException> {
                invalidRegistry.register(
                    type = Tree::class,
                    sprite = "tree.png",
                    configure = configure
                )
            }
        }

        assertEquals(listOf("objects/house.png"), queued)
    }

    private fun registry(
        queued: MutableList<String> = mutableListOf(),
        texture: TextureRegion = TextureRegion()
    ) = ObjectRegistry(
        directory = "objects",
        queueTexture = queued::add,
        regionFor = { texture },
        loadAlphaMask = { null }
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

    private fun alphaMask(): AlphaMask {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(1f, 1f, 1f, 1f)
        pixmap.drawPixel(0, 0)

        return try {
            AlphaMask.fromPixmap(pixmap)
        } finally {
            pixmap.dispose()
        }
    }
}
