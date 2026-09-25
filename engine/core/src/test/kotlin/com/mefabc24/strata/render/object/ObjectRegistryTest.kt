package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.graphics.g2d.TextureRegion
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
}
