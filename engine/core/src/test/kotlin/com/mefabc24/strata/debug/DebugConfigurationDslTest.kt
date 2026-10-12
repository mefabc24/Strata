package com.mefabc24.strata.debug

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.pathfinding.PathMovementMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugConfigurationDslTest {
    @Test
    fun `nested debug DSL configures each responsibility group`() {
        lateinit var configuredGrid: DebugGridSettings
        lateinit var configuredPaint: DebugPaintToolSettings

        val settings = DebugSettings().apply {
            ui {
                toolRail {
                    enabled = true
                    visibleOnStartup = true
                    toggleKey = Input.Keys.TAB
                }
                settingsWindow {
                    enabled = true
                    toggleKey = Input.Keys.F4
                }
            }
            visuals {
                filter = DebugVisualizationFilter.HOVERED
                grid {
                    configuredGrid = this
                    enabled = true
                    color = Color.CYAN
                }
                entities { showPath = true }
            }
            tools {
                paint {
                    configuredPaint = this
                    brushSize = 3
                }
                delete { brushSize = 5 }
                pathfinding { movementMode = PathMovementMode.EIGHT_WAY }
            }
            operations {
                performance { terminalLoggingIntervalSeconds = 2f }
                eventBus {
                    visible = true
                    captureEnabled = true
                }
                notifications { position = DebugNotificationPosition.BOTTOM_RIGHT }
                disableCameraRestrictions = true
            }
            presets {
                storage("test.debug")
                applySavedDefaultOnStartup = false
            }
        }

        assertTrue(settings.ui.toolRail.enabled)
        assertTrue(settings.ui.toolRail.visibleOnStartup)
        assertEquals(Input.Keys.TAB, settings.ui.toolRail.toggleKey)
        assertTrue(settings.ui.settingsWindow.enabled)
        assertEquals(Input.Keys.F4, settings.ui.settingsWindow.toggleKey)
        assertEquals(DebugVisualizationFilter.HOVERED, settings.visuals.filter)
        assertTrue(settings.visuals.grid.enabled)
        assertEquals(Color.CYAN, settings.visuals.grid.color)
        assertTrue(settings.visuals.entities.showPath)
        assertSame(settings.visuals.grid, configuredGrid)
        assertEquals(3, settings.tools.paint.brushSize)
        assertEquals(5, settings.tools.delete.brushSize)
        assertSame(settings.tools.paint, configuredPaint)
        assertEquals(PathMovementMode.EIGHT_WAY, settings.tools.pathfinding.movementMode)
        assertEquals(2f, settings.operations.performance.terminalLoggingIntervalSeconds)
        assertTrue(settings.operations.eventBus.visible)
        assertTrue(settings.operations.eventBus.captureEnabled)
        assertEquals(
            DebugNotificationPosition.BOTTOM_RIGHT,
            settings.operations.notifications.position
        )
        assertTrue(settings.operations.disableCameraRestrictions)
        assertEquals("test.debug", settings.presets.storageNamespace)
        assertFalse(settings.presets.applySavedDefaultOnStartup)
    }

    @Test
    fun `disabled debug interfaces do not disable other debug capabilities`() {
        val settings = DebugSettings().apply {
            ui {
                toolRail { enabled = false }
                settingsWindow { enabled = false }
            }
            visuals { objects { showSpriteBounds = true } }
            tools { pathfinding { enabled = true } }
            operations { eventBus { visible = true } }
        }

        assertFalse(settings.ui.toolRail.enabled)
        assertFalse(settings.ui.settingsWindow.enabled)
        assertTrue(settings.visuals.objects.showSpriteBounds)
        assertTrue(settings.tools.pathfinding.enabled)
        assertTrue(settings.operations.eventBus.visible)
    }
}
