package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.mefabc24.strata.debug.DebugEventMonitor
import com.mefabc24.strata.debug.DebugPerformanceMetric
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.event.EventBus
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.ui.StrataDropdown
import com.mefabc24.strata.ui.StrataExpander
import com.mefabc24.strata.ui.StrataNumericStepper
import com.mefabc24.strata.ui.StrataToggleButton
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugPerformanceControlsTest {
    @Test
    fun `debug window only toggles performance history visibility`() {
        TestGdxEnvironment.install()
        val skin = DebugPanelSkin.create()
        val ui = StrataUi(skin, DebugPanelSkin.theme())
        val settings = DebugSettings()
        val monitor = DebugEventMonitor(EventBus())
        val bindings = DebugControlBindings()
        try {
            DebugSettingsWindowBuilder(ui, settings, SimulationController(), monitor,
                World(1, 1) { _, _ -> object : Tile {} }, bindings).build(ui.root)
            val labels = descendants(ui.root).filterIsInstance<Label>()
            fun toggle(label: String): StrataToggleButton = labels.single { it.text.toString() == label }
                .parent.children.filterIsInstance<StrataToggleButton>().single()
            val history = toggle("Performance history overlay")
            val performance = settings.operations.performance
            assertFalse(history.isChecked)
            history.isChecked = true
            assertTrue(performance.historyOverlayEnabled)
            assertFalse(performance.historyRecording, "Showing the panel does not start recording")
            performance.startHistoryRecording()
            performance.record(RenderStats(), 0.016f)
            history.isChecked = false
            assertFalse(performance.historyOverlayEnabled)
            assertTrue(performance.historyRecording, "Hiding the panel does not stop recording")
            assertEquals(1, performance.history.size, "Hiding the panel does not clear history")
            assertFalse(performance.overlayEnabled)
            performance.historyOverlayEnabled = true
            bindings.sync()
            assertTrue(history.isChecked)

            val texts = labels.map { it.text.toString() }
            for (removed in listOf(
                "History & logging", "History recording", "History length", "Graph metric",
                "Clear performance history", "Start", "Stop", "Clear"
            )) {
                assertFalse(removed in texts, "$removed belongs to the Performance History panel")
            }
            val metricSelectors = descendants(ui.root).filterIsInstance<StrataDropdown<*>>()
                .filter { dropdown -> dropdown.items.any { it.value is DebugPerformanceMetric } }
            assertTrue(metricSelectors.isEmpty(), "Graph metrics are selected inside the panel")
            assertEquals(
                listOf("Performance history overlay"),
                texts.filter { it.contains("performance history", ignoreCase = true) }
            )
        } finally {
            monitor.dispose()
            ui.dispose()
            skin.dispose()
        }
    }

    @Test
    fun `remaining performance settings live in accurately named sections`() {
        TestGdxEnvironment.install()
        val skin = DebugPanelSkin.create()
        val ui = StrataUi(skin, DebugPanelSkin.theme())
        val settings = DebugSettings()
        val monitor = DebugEventMonitor(EventBus())
        val bindings = DebugControlBindings()
        try {
            DebugSettingsWindowBuilder(ui, settings, SimulationController(), monitor,
                World(1, 1) { _, _ -> object : Tile {} }, bindings).build(ui.root)
            val expanders = descendants(ui.root).filterIsInstance<StrataExpander>().associateBy { it.title }
            fun texts(title: String) = descendants(expanders.getValue(title).content)
                .filterIsInstance<Label>().map { it.text.toString() }

            assertEquals(
                listOf("Performance overlay", "Performance history overlay", "World stats overlay", "Overlay refresh"),
                texts("Overlays").filter { it.isNotBlank() && it != "ON" && it != "OFF" && it != "-" && it != "+" &&
                    it.toFloatOrNull() == null }
            )
            val logging = texts("Terminal logging")
            assertTrue("Terminal logging enabled" in logging)
            assertTrue("Terminal interval" in logging)
            assertFalse(logging.any { it.contains("history", ignoreCase = true) })

            val refresh = descendants(expanders.getValue("Overlays").content)
                .filterIsInstance<StrataNumericStepper>().single()
            refresh.value = 0.5f
            assertEquals(0.5f, settings.operations.performance.overlayRefreshIntervalSeconds)
            val logToggle = descendants(expanders.getValue("Terminal logging").content)
                .filterIsInstance<StrataToggleButton>().single()
            logToggle.isChecked = true
            assertTrue(settings.operations.performance.terminalLoggingEnabled)
            settings.operations.performance.terminalLoggingEnabled = false
            bindings.sync()
            assertFalse(logToggle.isChecked)
        } finally {
            monitor.dispose()
            ui.dispose()
            skin.dispose()
        }
    }

    private fun descendants(actor: Actor): List<Actor> = listOf(actor) +
        if (actor is Group) actor.children.flatMap(::descendants) else emptyList()
}
