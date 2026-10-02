package com.mefabc24.strata.debug

import com.badlogic.gdx.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugVisualConfigurationCodecTest {

    @Test
    fun `visual configuration survives json round trip`() {
        val original = DebugSettings().apply {
            grid.enabled = true
            grid.lineWidth = 3f
            grid.color = Color.CYAN

            entities.enabled = true
            entities.showPath = false

            visualizationFilter = DebugVisualizationFilter.HOVERED

            worldVisibility.setOverlayLayerVisible(
                "roads",
                false
            )
        }

        val json = DebugVisualConfigurationCodec.encode(
            original.captureVisualConfiguration()
        )

        val restored = DebugVisualConfigurationCodec.decode(json)

        val target = DebugSettings()
        target.applyVisualConfiguration(restored)

        assertTrue(target.grid.enabled)
        assertEquals(3f, target.grid.lineWidth)
        assertEquals(Color.CYAN, target.grid.color)

        assertTrue(target.entities.enabled)
        assertFalse(target.entities.showPath)

        assertEquals(
            DebugVisualizationFilter.HOVERED,
            target.visualizationFilter
        )

        assertFalse(
            target.worldVisibility.isOverlayLayerVisible("roads")
        )
    }
}