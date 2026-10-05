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
            visuals.grid.enabled = true
            visuals.grid.lineWidth = 3f
            visuals.grid.color = Color.CYAN

            visuals.entities.showPath = true

            visuals.filter = DebugVisualizationFilter.HOVERED

            visuals.worldVisibility.setOverlayLayerVisible(
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

        assertTrue(target.visuals.grid.enabled)
        assertEquals(3f, target.visuals.grid.lineWidth)
        assertEquals(Color.CYAN, target.visuals.grid.color)

        assertTrue(target.visuals.entities.showPath)

        assertEquals(
            DebugVisualizationFilter.HOVERED,
            target.visuals.filter
        )

        assertFalse(
            target.visuals.worldVisibility.isOverlayLayerVisible("roads")
        )
    }

    @Test
    fun `version one disabled gates keep dormant child options inactive`() {
        val legacy = DebugVisualConfigurationCodec.decode(
            """{"version":1,"values":{"entities.enabled":"boolean:false","entities.path":"boolean:true"}}"""
        )
        val target = DebugSettings()

        target.applyVisualConfiguration(legacy)

        assertFalse(target.visuals.entities.showPath)
        assertFalse(target.visuals.entities.hasActiveVisuals)
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

        assertFalse(settings.visuals.objects.showOccupiedTileFill)
        assertFalse(settings.visuals.grid.showBackground)

        assertTrue(settings.visuals.entities.showCurrentTileFill)
        assertEquals(0.4f, settings.visuals.entities.currentTileFillColor.a)
    }

    @Test
    fun `version four picking children migrate to active picking diagnostics`() {
        val legacy = DebugVisualConfigurationCodec.decode(
            """{"version":4,"values":{"picking.cursor":"boolean:true"}}"""
        )
        val settings = DebugSettings()

        settings.applyVisualConfiguration(legacy)

        assertTrue(settings.visuals.picking.enabled)
        assertTrue(settings.visuals.picking.showCursorHit)
        assertTrue(settings.visuals.picking.hasActiveVisuals)
    }
}
