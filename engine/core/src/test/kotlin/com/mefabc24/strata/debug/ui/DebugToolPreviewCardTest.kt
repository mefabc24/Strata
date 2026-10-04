package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugToolPreviewCardTest {
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

    private fun Table.texts(): List<String> = buildList {
        children.forEach { child ->
            when (child) {
                is Label -> add(child.text.toString())
                is Table -> addAll(child.texts())
            }
        }
    }
}
