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
                objects.showOccupiedTiles = false
                objects.showOriginTile = false
                grid.enabled = false
                grid.color = Color.CYAN
            }
        )
        val settings = DebugSettings().apply {
            presets { storage("test.debug") }
            objects.showOccupiedTiles = true
            objects.showOriginTile = true
            grid.enabled = true
            grid.color = Color.RED
        }

        settings.initializeDefaultVisualConfiguration()

        assertFalse(settings.objects.showOccupiedTiles)
        assertFalse(settings.objects.showOriginTile)
        assertFalse(settings.grid.enabled)
        assertEquals(Color.CYAN, settings.grid.color)
    }

    @Test
    fun `automatic startup application can be disabled`() {
        storeDefault(DebugSettings().apply { entities.showPath = false })
        val settings = DebugSettings().apply {
            presets {
                storage("test.debug")
                applySavedDefaultOnStartup = false
            }
            entities.showPath = true
        }

        settings.initializeDefaultVisualConfiguration()

        assertTrue(settings.entities.showPath)
        settings.applyDefaultVisualConfiguration()
        assertFalse(settings.entities.showPath)
    }

    @Test
    fun `missing storage preserves game visual configuration`() {
        val settings = DebugSettings().apply {
            presets { storage("test.debug") }
            worldInfo.showTileCoordinates = true
        }

        settings.initializeDefaultVisualConfiguration()

        assertTrue(settings.worldInfo.showTileCoordinates)
        settings.worldInfo.showTileCoordinates = false
        settings.applyDefaultVisualConfiguration()
        assertTrue(settings.worldInfo.showTileCoordinates)
    }

    @Test
    fun `malformed storage falls back to game configuration and reports a diagnostic`() {
        storedValues[DEFAULT_PRESET_KEY] = "not-json"
        val settings = DebugSettings().apply {
            presets { storage("test.debug") }
            culling.showVisibleArea = true
        }

        settings.initializeDefaultVisualConfiguration()

        assertTrue(settings.culling.showVisibleArea)
        assertTrue(diagnostics.single().contains("Failed to load saved DEFAULT debug preset"))
    }

    @Test
    fun `startup default is independent of windows and excludes operational state`() {
        storeDefault(
            DebugSettings().apply {
                picking.showCursorHit = true
                toolsWindow.enabled = true
                toolsWindow.visibleOnStartup = true
                debugWindow.enabled = true
                debugWindow.visibleOnStartup = true
                simulation.enabled = true
                simulation.freezeVisualAnimations = true
                camera.disableRestrictions = true
                pathfinding.enabled = false
            }
        )
        val settings = DebugSettings().apply {
            presets { storage("test.debug") }
            toolsWindow.enabled = false
            debugWindow.enabled = false
            simulation.enabled = false
            simulation.freezeVisualAnimations = false
            camera.disableRestrictions = false
            pathfinding.enabled = true
        }

        settings.initializeDefaultVisualConfiguration()

        assertTrue(settings.picking.showCursorHit)
        assertFalse(settings.toolsWindow.enabled)
        assertFalse(settings.toolsWindow.visibleOnStartup)
        assertFalse(settings.debugWindow.enabled)
        assertFalse(settings.debugWindow.visibleOnStartup)
        assertFalse(settings.simulation.enabled)
        assertFalse(settings.simulation.freezeVisualAnimations)
        assertFalse(settings.camera.disableRestrictions)
        assertTrue(settings.pathfinding.enabled)
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
