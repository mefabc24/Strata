package com.mefabc24.strata.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        assertTrue(settings.inspect.showTile)
        assertTrue(settings.inspect.showObjectFootprint)
        assertTrue(settings.inspect.showObjectOrigin)
        assertTrue(settings.inspect.showObjectSpriteBounds)
        assertTrue(settings.inspect.showEntityTile)
        assertTrue(settings.inspect.showEntityPosition)
        assertTrue(settings.inspect.showEntityPath)
        assertTrue(settings.inspect.showEntityDirection)
        assertTrue(settings.inspect.showEntitySpriteBounds)
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
            inspect.showEntityPath = false
            inspect.showObjectOrigin = false
        }
        settings.applyPreset(DebugPreset.PLACEMENT)
        assertTrue(settings.grid.enabled)
        assertTrue(settings.objects.enabled)
        assertFalse(settings.entities.enabled)

        settings.applyPreset(DebugPreset.EVERYTHING)
        assertTrue(settings.performance.enabled)
        assertTrue(settings.simulation.enabled)
        assertTrue(settings.grid.enabled)
        assertTrue(settings.objects.enabled)
        assertTrue(settings.entities.enabled)
        assertTrue(settings.picking.enabled)
        assertTrue(settings.renderOrder.enabled)
        assertTrue(settings.culling.enabled)
        assertTrue(settings.camera.enabled)
        assertTrue(settings.worldStats.enabled)
        assertNotNull(settings.grid.backgroundColor)
        assertNotNull(settings.grid.hoverBackgroundColor)
        assertTrue(settings.objects.showOccupiedTiles)
        assertTrue(settings.objects.showOriginTile)
        assertTrue(settings.objects.showSpriteBounds)
        assertNotNull(settings.objects.occupiedTileFillColor)
        assertTrue(settings.entities.showCurrentTile)
        assertTrue(settings.entities.showPosition)
        assertTrue(settings.entities.showPath)
        assertTrue(settings.entities.showDirection)
        assertTrue(settings.entities.showSpriteBounds)
        assertNotNull(settings.entities.currentTileFillColor)
        assertTrue(settings.picking.showSpriteBounds)
        assertTrue(settings.picking.showCursorHit)
        assertTrue(settings.renderOrder.showLabels)
        assertTrue(settings.culling.showVisibleArea)
        assertTrue(settings.culling.showObjectBounds)
        assertTrue(settings.culling.showEntityBounds)
        assertTrue(settings.camera.showVisibleArea)
        assertTrue(settings.camera.showWorldBounds)
        assertTrue(settings.camera.showClampBounds)
        assertFalse(settings.pathfinding.enabled)
        assertFalse(settings.pathfinding.showExploredNodes)
        assertTrue(settings.pathfinding.showFinalPath)
        assertFalse(settings.inspect.showEntityPath)
        assertFalse(settings.inspect.showObjectOrigin)
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
    fun `every preset preserves tool visualization settings`() {
        DebugPreset.entries.forEach { preset ->
            val settings = DebugSettings().apply {
                pathfinding.enabled = false
                pathfinding.showExploredNodes = false
                pathfinding.showFinalPath = true
                inspect.showTile = false
                inspect.showEntityDirection = false
            }

            settings.applyPreset(preset)

            assertFalse(settings.pathfinding.enabled, preset.name)
            assertFalse(settings.pathfinding.showExploredNodes, preset.name)
            assertTrue(settings.pathfinding.showFinalPath, preset.name)
            assertFalse(settings.inspect.showTile, preset.name)
            assertFalse(settings.inspect.showEntityDirection, preset.name)
        }
    }

    @Test
    fun `every preset starts from a disabled diagnostic state`() {
        val settings = DebugSettings()
        DebugPreset.entries.forEach { preset ->
            settings.applyPreset(DebugPreset.EVERYTHING)
            settings.simulation.enabled = true
            settings.applyPreset(preset)
            assertEquals(
                preset == DebugPreset.EVERYTHING,
                settings.simulation.enabled,
                preset.name
            )
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
