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
            visuals.filter = DebugVisualizationFilter.HOVERED
            visuals.grid.enabled = true
            visuals.grid.lineWidth = 3f
            visuals.grid.color = Color.CYAN
            visuals.worldVisibility.entitiesVisible = false
            visuals.worldVisibility.setOverlayLayerVisible("roads", false)
            visuals.entities.showMovementTrail = true
            visuals.picking.enabled = true
            visuals.picking.showCursorHit = true
            visuals.renderOrder.showSortVolumes = true
            operations.performance.overlayRefreshIntervalSeconds = 0.75f
            operations.eventBus.visible = true
            operations.notifications.enabled = true
            operations.notifications.position = DebugNotificationPosition.BOTTOM_RIGHT
        }
        val captured = settings.captureVisualConfiguration()

        settings.apply {
            visuals.filter = DebugVisualizationFilter.ALL
            visuals.grid.enabled = false
            visuals.grid.lineWidth = 1f
            visuals.grid.color = Color.RED
            visuals.worldVisibility.showAll()
            visuals.entities.showMovementTrail = false
            visuals.picking.enabled = false
            visuals.picking.showCursorHit = false
            visuals.renderOrder.showSortVolumes = false
            operations.performance.overlayRefreshIntervalSeconds = 0.1f
            operations.eventBus.visible = false
            operations.notifications.enabled = false
            operations.notifications.position = DebugNotificationPosition.TOP_CENTER
            operations.performance.terminalLoggingEnabled = true
            operations.performance.historyRecording = true
            operations.simulation.freezeVisualAnimations = true
            operations.disableCameraRestrictions = true
            tools.inspect.showEntityPath = false
            tools.pathfinding.showFinalPath = false
        }

        settings.applyVisualConfiguration(captured)

        assertEquals(DebugVisualizationFilter.HOVERED, settings.visuals.filter)
        assertTrue(settings.visuals.grid.enabled)
        assertEquals(3f, settings.visuals.grid.lineWidth)
        assertEquals(Color.CYAN, settings.visuals.grid.color)
        assertFalse(settings.visuals.worldVisibility.entitiesVisible)
        assertFalse(settings.visuals.worldVisibility.isOverlayLayerVisible("roads"))
        assertTrue(settings.visuals.entities.showMovementTrail)
        assertTrue(settings.visuals.picking.enabled)
        assertTrue(settings.visuals.picking.showCursorHit)
        assertTrue(settings.visuals.renderOrder.showSortVolumes)
        assertEquals(0.75f, settings.operations.performance.overlayRefreshIntervalSeconds)
        assertFalse(settings.operations.eventBus.visible)
        assertFalse(settings.operations.notifications.enabled)
        assertEquals(DebugNotificationPosition.BOTTOM_RIGHT, settings.operations.notifications.position)

        assertTrue(settings.operations.performance.terminalLoggingEnabled)
        assertTrue(settings.operations.performance.historyRecording)
        assertTrue(settings.operations.simulation.freezeVisualAnimations)
        assertTrue(settings.operations.disableCameraRestrictions)
        assertFalse(settings.tools.inspect.showEntityPath)
        assertFalse(settings.tools.pathfinding.showFinalPath)
    }

    @Test
    fun `category reset changes only that visual category`() {
        val settings = DebugSettings().apply {
            visuals.grid.enabled = true
            visuals.grid.lineWidth = 4f
            visuals.entities.showCurrentTile = true
            visuals.entities.showPath = false
            operations.performance.terminalLoggingEnabled = true
        }

        settings.resetVisualCategory(DebugVisualCategory.GRID)

        assertFalse(settings.visuals.grid.enabled)
        assertEquals(1f, settings.visuals.grid.lineWidth)
        assertTrue(settings.visuals.entities.showCurrentTile)
        assertFalse(settings.visuals.entities.showPath)
        assertTrue(settings.operations.performance.terminalLoggingEnabled)
    }

    @Test
    fun `reset all visual settings preserves operational settings`() {
        val settings = DebugSettings().apply {
            applyPreset(DebugPreset.EVERYTHING)
            operations.performance.terminalLoggingEnabled = true
            operations.disableCameraRestrictions = true
            operations.simulation.freezeVisualAnimations = true
            tools.inspect.showTile = false
        }

        settings.resetVisualConfiguration()

        assertFalse(settings.visuals.grid.enabled)
        assertFalse(settings.visuals.entities.hasActiveVisuals)
        assertFalse(settings.visuals.picking.enabled)
        assertFalse(settings.visuals.renderOrder.hasActiveVisuals)
        assertTrue(settings.visuals.worldVisibility.entitiesVisible)
        assertTrue(settings.operations.performance.terminalLoggingEnabled)
        assertTrue(settings.operations.disableCameraRestrictions)
        assertTrue(settings.operations.simulation.freezeVisualAnimations)
        assertFalse(settings.tools.inspect.showTile)
    }

    @Test
    fun `saving default replaces the previous visual configuration`() {
        val settings = DebugSettings()

        settings.visuals.grid.enabled = true
        settings.visuals.grid.lineWidth = 3f
        settings.saveDefaultVisualConfiguration()

        settings.visuals.grid.enabled = false
        settings.visuals.grid.lineWidth = 1f

        settings.applyDefaultVisualConfiguration()

        assertTrue(settings.visuals.grid.enabled)
        assertEquals(3f, settings.visuals.grid.lineWidth)

        settings.visuals.grid.enabled = false
        settings.saveDefaultVisualConfiguration()

        settings.visuals.grid.enabled = true
        settings.applyDefaultVisualConfiguration()

        assertFalse(settings.visuals.grid.enabled)
    }

    @Test
    fun `disabling fills preserves configured colors`() {
        val settings = DebugSettings()

        settings.visuals.objects.showOccupiedTileFill = true
        settings.visuals.objects.occupiedTileFillColor =
            Color(0.2f, 0.5f, 0.8f, 0.35f)

        val expected = settings.visuals.objects.occupiedTileFillColor

        settings.visuals.objects.showOccupiedTileFill = false

        assertFalse(settings.visuals.objects.showOccupiedTileFill)
        assertEquals(expected, settings.visuals.objects.occupiedTileFillColor)

        settings.visuals.objects.showOccupiedTileFill = true

        assertEquals(expected, settings.visuals.objects.occupiedTileFillColor)
    }
}
