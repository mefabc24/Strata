package com.mefabc24.strata.debug

import com.badlogic.gdx.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugVisualPresetsTest {
    @Test
    fun `custom configuration restores visual settings and preserves operational state`() {
        val settings = DebugSettings().apply {
            visualizationFilter = DebugVisualizationFilter.HOVERED
            grid.enabled = true
            grid.lineWidth = 3f
            grid.color = Color.CYAN
            worldVisibility.entitiesVisible = false
            worldVisibility.setOverlayLayerVisible("roads", false)
            entities.showMovementTrail = true
            renderOrder.showSortVolumes = true
            notifications.position = DebugNotificationPosition.BOTTOM_RIGHT
        }
        val captured = settings.captureVisualConfiguration()

        settings.apply {
            visualizationFilter = DebugVisualizationFilter.ALL
            grid.enabled = false
            grid.lineWidth = 1f
            grid.color = Color.RED
            worldVisibility.showAll()
            entities.showMovementTrail = false
            renderOrder.showSortVolumes = false
            notifications.position = DebugNotificationPosition.TOP_CENTER
            performance.terminalLoggingEnabled = true
            performance.historyRecording = true
            simulation.freezeVisualAnimations = true
            camera.disableRestrictions = true
            inspect.showEntityPath = false
            pathfinding.showFinalPath = false
        }

        settings.applyVisualConfiguration(captured)

        assertEquals(DebugVisualizationFilter.HOVERED, settings.visualizationFilter)
        assertTrue(settings.grid.enabled)
        assertEquals(3f, settings.grid.lineWidth)
        assertEquals(Color.CYAN, settings.grid.color)
        assertFalse(settings.worldVisibility.entitiesVisible)
        assertFalse(settings.worldVisibility.isOverlayLayerVisible("roads"))
        assertTrue(settings.entities.showMovementTrail)
        assertTrue(settings.renderOrder.showSortVolumes)
        assertEquals(DebugNotificationPosition.BOTTOM_RIGHT, settings.notifications.position)

        assertTrue(settings.performance.terminalLoggingEnabled)
        assertTrue(settings.performance.historyRecording)
        assertTrue(settings.simulation.freezeVisualAnimations)
        assertTrue(settings.camera.disableRestrictions)
        assertFalse(settings.inspect.showEntityPath)
        assertFalse(settings.pathfinding.showFinalPath)
    }

    @Test
    fun `category reset changes only that visual category`() {
        val settings = DebugSettings().apply {
            grid.enabled = true
            grid.lineWidth = 4f
            entities.showCurrentTile = true
            entities.showPath = false
            performance.terminalLoggingEnabled = true
        }

        settings.resetVisualCategory(DebugVisualCategory.GRID)

        assertFalse(settings.grid.enabled)
        assertEquals(1f, settings.grid.lineWidth)
        assertTrue(settings.entities.showCurrentTile)
        assertFalse(settings.entities.showPath)
        assertTrue(settings.performance.terminalLoggingEnabled)
    }

    @Test
    fun `reset all visual settings preserves operational settings`() {
        val settings = DebugSettings().apply {
            applyPreset(DebugPreset.EVERYTHING)
            performance.terminalLoggingEnabled = true
            camera.disableRestrictions = true
            simulation.freezeVisualAnimations = true
            inspect.showTile = false
        }

        settings.resetVisualConfiguration()

        assertFalse(settings.grid.enabled)
        assertFalse(settings.entities.hasActiveVisuals)
        assertFalse(settings.renderOrder.hasActiveVisuals)
        assertTrue(settings.worldVisibility.entitiesVisible)
        assertTrue(settings.performance.terminalLoggingEnabled)
        assertTrue(settings.camera.disableRestrictions)
        assertTrue(settings.simulation.freezeVisualAnimations)
        assertFalse(settings.inspect.showTile)
    }

    @Test
    fun `saving default replaces the previous visual configuration`() {
        val settings = DebugSettings()

        settings.grid.enabled = true
        settings.grid.lineWidth = 3f
        settings.saveDefaultVisualConfiguration()

        settings.grid.enabled = false
        settings.grid.lineWidth = 1f

        settings.applyDefaultVisualConfiguration()

        assertTrue(settings.grid.enabled)
        assertEquals(3f, settings.grid.lineWidth)

        settings.grid.enabled = false
        settings.saveDefaultVisualConfiguration()

        settings.grid.enabled = true
        settings.applyDefaultVisualConfiguration()

        assertFalse(settings.grid.enabled)
    }
}
