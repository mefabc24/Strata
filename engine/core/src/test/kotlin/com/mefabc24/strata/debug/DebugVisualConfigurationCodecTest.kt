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

            entities.showPath = true

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

        assertTrue(target.entities.showPath)

        assertEquals(
            DebugVisualizationFilter.HOVERED,
            target.visualizationFilter
        )

        assertFalse(
            target.worldVisibility.isOverlayLayerVisible("roads")
        )
    }

    @Test
    fun `version one disabled gates keep dormant child options inactive`() {
        val legacy = DebugVisualConfigurationCodec.decode(
            """{"version":1,"values":{"entities.enabled":"boolean:false","entities.path":"boolean:true"}}"""
        )
        val target = DebugSettings()

        target.applyVisualConfiguration(legacy)

        assertFalse(target.entities.showPath)
        assertFalse(target.entities.hasActiveVisuals)
    }

    @Test
    fun `legacy nullable fills migrate to independent visibility flags`() {
        val legacy = DebugVisualConfigurationCodec.decode(
            """
        {
            "version": 3,
            "values": {
                "objects.fill": "null:",
                "entities.fill": "color:0.3,1.0,0.3,0.4",
                "grid.background": "null:"
            }
        }
        """.trimIndent()
        )

        val settings = DebugSettings()

        settings.applyVisualConfiguration(legacy)

        assertFalse(settings.objects.showOccupiedTileFill)
        assertFalse(settings.grid.showBackground)

        assertTrue(settings.entities.showCurrentTileFill)
        assertEquals(0.4f, settings.entities.currentTileFillColor.a)
    }
}
