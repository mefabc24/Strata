package com.mefabc24.strata.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugSettingsTest {
    @Test
    fun `all debug functionality is disabled by default`() {
        val settings = DebugSettings()
        assertFalse(settings.panel.enabled)
        assertTrue(settings.panel.visible)
        assertFalse(settings.performance.enabled)
        assertFalse(settings.simulation.enabled)
        assertFalse(settings.grid.enabled)
        assertFalse(settings.objects.enabled)
        assertFalse(settings.entities.enabled)
        assertFalse(settings.picking.enabled)
        assertFalse(settings.renderOrder.enabled)
        assertFalse(settings.culling.enabled)
        assertFalse(settings.camera.enabled)
        assertFalse(settings.worldStats.enabled)
        assertFalse(settings.pathfinding.enabled)
        assertFalse(settings.placement.enabled)
    }

    @Test
    fun `panel visibility is independent from feature settings`() {
        val settings = DebugSettings().apply {
            panel { enabled = true; visible = false }
            grid { enabled = true }
        }
        assertTrue(settings.panel.enabled)
        assertFalse(settings.panel.visible)
        assertTrue(settings.grid.enabled)
    }

    @Test
    fun `presets only change diagnostic settings`() {
        val settings = DebugSettings()
        settings.applyPreset(DebugPreset.PLACEMENT)
        assertTrue(settings.grid.enabled)
        assertTrue(settings.objects.enabled)
        assertTrue(settings.placement.enabled)
        assertFalse(settings.entities.enabled)

        settings.applyPreset(DebugPreset.EVERYTHING)
        assertTrue(settings.picking.enabled)
        assertTrue(settings.renderOrder.enabled)
        assertTrue(settings.culling.enabled)
        assertTrue(settings.camera.enabled)
        assertEquals(true, settings.worldStats.enabled)
    }
}
