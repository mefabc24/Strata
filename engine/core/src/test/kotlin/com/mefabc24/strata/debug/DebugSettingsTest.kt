package com.mefabc24.strata.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugSettingsTest {
    @Test
    fun `optional overlays are disabled while tool context defaults are ready`() {
        val settings = DebugSettings()
        assertFalse(settings.panel.enabled)
        assertTrue(settings.panel.visible)
        assertFalse(settings.panel.expanded)
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
        assertTrue(settings.pathfinding.enabled)
        assertNull(settings.worldState.pickingSelection.lockedTarget)
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
        val settings = DebugSettings().apply {
            pathfinding.enabled = false
            pathfinding.showExploredNodes = false
            pathfinding.showFinalPath = true
        }
        settings.applyPreset(DebugPreset.PLACEMENT)
        assertTrue(settings.grid.enabled)
        assertTrue(settings.objects.enabled)
        assertFalse(settings.entities.enabled)

        settings.applyPreset(DebugPreset.EVERYTHING)
        assertTrue(settings.picking.enabled)
        assertTrue(settings.renderOrder.enabled)
        assertTrue(settings.culling.enabled)
        assertTrue(settings.camera.enabled)
        assertEquals(true, settings.worldStats.enabled)
        assertFalse(settings.pathfinding.enabled)
        assertFalse(settings.pathfinding.showExploredNodes)
        assertTrue(settings.pathfinding.showFinalPath)
    }

    @Test
    fun `off preset disables every optional diagnostic overlay`() {
        val settings = DebugSettings().apply {
            DebugPreset.EVERYTHING.let(::applyPreset)
            simulation.enabled = true
            panel.enabled = true
            panel.visible = true
        }

        settings.applyPreset(DebugPreset.OFF)

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
        assertTrue(settings.pathfinding.enabled)
        assertTrue(settings.pathfinding.showExploredNodes)
        assertTrue(settings.pathfinding.showFinalPath)
        assertTrue(settings.panel.enabled)
        assertTrue(settings.panel.visible)
    }

    @Test
    fun `every preset preserves pathfinding tool visualization`() {
        DebugPreset.entries.forEach { preset ->
            val settings = DebugSettings().apply {
                pathfinding.enabled = false
                pathfinding.showExploredNodes = false
                pathfinding.showFinalPath = true
            }

            settings.applyPreset(preset)

            assertFalse(settings.pathfinding.enabled, preset.name)
            assertFalse(settings.pathfinding.showExploredNodes, preset.name)
            assertTrue(settings.pathfinding.showFinalPath, preset.name)
        }
    }

    @Test
    fun `every preset starts from a disabled diagnostic state`() {
        val settings = DebugSettings()
        DebugPreset.entries.forEach { preset ->
            settings.applyPreset(DebugPreset.EVERYTHING)
            settings.simulation.enabled = true
            settings.applyPreset(preset)
            assertFalse(settings.simulation.enabled)
        }
        settings.applyPreset(DebugPreset.MINIMAL)
        assertTrue(settings.performance.enabled)
        assertTrue(settings.grid.enabled)
        assertFalse(settings.worldStats.enabled)
        settings.applyPreset(DebugPreset.RENDERING)
        assertTrue(settings.renderOrder.enabled)
        assertTrue(settings.culling.enabled)
        assertTrue(settings.camera.enabled)
    }
}
