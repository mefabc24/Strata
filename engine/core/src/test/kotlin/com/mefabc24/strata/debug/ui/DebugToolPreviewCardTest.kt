package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugToolPreviewCardTest {
    private class DefaultObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    private class PriorityObject : Placeable {
        override val footprint = Footprint.square(2)
    }

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `preview content follows the selected registry entry`() {
        val skin = DebugPanelSkin.create()
        val preview = DebugToolPreviewCard(skin)

        preview.show(DebugToolPreview(
            key = "house",
            displayName = "House",
            texture = TextureRegion(),
            details = listOf("Footprint" to "2 x 2")
        ))
        assertEquals(listOf("House", "Footprint", "2 x 2"), preview.texts())

        preview.show(DebugToolPreview(
            key = "villa",
            displayName = "Villa",
            texture = TextureRegion(),
            details = listOf("Frames" to "4")
        ))
        assertEquals(listOf("Villa", "Frames", "4"), preview.texts())

        skin.dispose()
    }

    @Test
    fun `build preview details use the registered render priority`() {
        val texture = TextureRegion()
        val registry = ObjectRegistry(
            directory = "objects",
            queueTexture = {},
            regionFor = { texture },
            loadAlphaMask = { null }
        )
        registry.register(
            sprite = "default.png",
            factory = ::DefaultObject
        )
        registry.register(
            sprite = "priority.png",
            factory = ::PriorityObject
        ) {
            renderPriority = 10
        }
        registry.prepare()

        val defaultDetails = buildObjectPreviewDetails(registry.entries[0])
        val priorityDetails = buildObjectPreviewDetails(registry.entries[1])

        assertEquals("0", defaultDetails.single { it.first == "Priority" }.second)
        assertEquals("10", priorityDetails.single { it.first == "Priority" }.second)
    }

    private fun Table.texts(): List<String> = buildList {
        children.forEach { child ->
            when (child) {
                is Label -> add(child.text.toString())
                is Table -> addAll(child.texts())
            }
        }
    }
}
