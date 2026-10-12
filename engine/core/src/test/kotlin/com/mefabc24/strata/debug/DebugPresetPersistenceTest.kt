package com.mefabc24.strata.debug

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.testing.defaultValue
import com.mefabc24.strata.testing.proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugPresetPersistenceTest {
    private val storedValues = mutableMapOf<String, Any?>()
    private val diagnostics = mutableListOf<String>()

    @BeforeTest
    fun installPreferences() {
        val preferences = proxy(Preferences::class.java) { instance, method, arguments ->
            when (method.name) {
                "putString" -> {
                    storedValues[arguments!![0] as String] = arguments[1] as String
                    instance
                }
                "getString" -> storedValues[arguments!![0] as String]
                    ?: arguments.getOrNull(1)
                    ?: ""
                "contains" -> storedValues.containsKey(arguments!![0] as String)
                "remove" -> {
                    storedValues.remove(arguments!![0] as String)
                    instance
                }
                "clear" -> {
                    storedValues.clear()
                    instance
                }
                "get" -> storedValues.toMap()
                else -> defaultValue(method.returnType)
            }
        }
        Gdx.app = proxy(Application::class.java) { _, method, arguments ->
            when (method.name) {
                "getPreferences" -> preferences
                "error" -> {
                    diagnostics += arguments!![1] as String
                    null
                }
                else -> defaultValue(method.returnType)
            }
        }
    }

    @Test
    fun `saved default overrides game visuals during startup`() {
        storeDefault(
            DebugSettings().apply {
                visuals.objects.showOccupiedTiles = false
                visuals.objects.showOriginTile = false
                visuals.grid.enabled = false
                visuals.grid.color = Color.CYAN
            }
        )
        val settings = DebugSettings().apply {
            presets { storage("test.debug") }
            visuals.objects.showOccupiedTiles = true
            visuals.objects.showOriginTile = true
            visuals.grid.enabled = true
            visuals.grid.color = Color.RED
        }

        settings.initializeDefaultVisualConfiguration()

        assertFalse(settings.visuals.objects.showOccupiedTiles)
        assertFalse(settings.visuals.objects.showOriginTile)
        assertFalse(settings.visuals.grid.enabled)
        assertEquals(Color.CYAN, settings.visuals.grid.color)
    }

    @Test
    fun `automatic startup application can be disabled`() {
        storeDefault(DebugSettings().apply { visuals.entities.showPath = false })
        val settings = DebugSettings().apply {
            presets {
                storage("test.debug")
                applySavedDefaultOnStartup = false
            }
            visuals.entities.showPath = true
        }

        settings.initializeDefaultVisualConfiguration()

        assertTrue(settings.visuals.entities.showPath)
        settings.applyDefaultVisualConfiguration()
        assertFalse(settings.visuals.entities.showPath)
    }

    @Test
    fun `missing storage preserves game visual configuration`() {
        val settings = DebugSettings().apply {
            presets { storage("test.debug") }
            visuals.worldInfo.showTileCoordinates = true
        }

        settings.initializeDefaultVisualConfiguration()

        assertTrue(settings.visuals.worldInfo.showTileCoordinates)
        settings.visuals.worldInfo.showTileCoordinates = false
        settings.applyDefaultVisualConfiguration()
        assertTrue(settings.visuals.worldInfo.showTileCoordinates)
    }

    @Test
    fun `malformed storage falls back to game configuration and reports a diagnostic`() {
        storedValues[DEFAULT_PRESET_KEY] = "not-json"
        val settings = DebugSettings().apply {
            presets { storage("test.debug") }
            visuals.culling.showVisibleArea = true
        }

        settings.initializeDefaultVisualConfiguration()

        assertTrue(settings.visuals.culling.showVisibleArea)
        assertTrue(diagnostics.single().contains("Failed to load saved DEFAULT debug preset"))
    }

    @Test
    fun `startup default is independent of windows and excludes operational state`() {
        storeDefault(
            DebugSettings().apply {
                visuals.picking.enabled = true
                visuals.picking.showCursorHit = true
                ui.toolRail.enabled = true
                ui.toolRail.visibleOnStartup = true
                ui.settingsWindow.enabled = true
                ui.settingsWindow.visibleOnStartup = true
                operations.simulation.enabled = true
                operations.simulation.freezeVisualAnimations = true
                operations.disableCameraRestrictions = true
                tools.pathfinding.enabled = false
            }
        )
        val settings = DebugSettings().apply {
            presets { storage("test.debug") }
            ui.toolRail.enabled = false
            ui.settingsWindow.enabled = false
            operations.simulation.enabled = false
            operations.simulation.freezeVisualAnimations = false
            operations.disableCameraRestrictions = false
            tools.pathfinding.enabled = true
        }

        settings.initializeDefaultVisualConfiguration()

        assertTrue(settings.visuals.picking.enabled)
        assertTrue(settings.visuals.picking.showCursorHit)
        assertFalse(settings.ui.toolRail.enabled)
        assertFalse(settings.ui.toolRail.visibleOnStartup)
        assertFalse(settings.ui.settingsWindow.enabled)
        assertFalse(settings.ui.settingsWindow.visibleOnStartup)
        assertFalse(settings.operations.simulation.enabled)
        assertFalse(settings.operations.simulation.freezeVisualAnimations)
        assertFalse(settings.operations.disableCameraRestrictions)
        assertTrue(settings.tools.pathfinding.enabled)
    }

    private fun storeDefault(settings: DebugSettings) {
        storedValues[DEFAULT_PRESET_KEY] = DebugVisualConfigurationCodec.encode(
            settings.captureVisualConfiguration()
        )
    }

    private companion object {
        const val DEFAULT_PRESET_KEY = "default-visual-configuration"
    }
}
