package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.mefabc24.strata.debug.DebugEventMonitor
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.event.EventBus
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.ui.StrataToggleButton
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugPerformanceControlsTest {
    @Test
    fun `settings expose independent graph visibility and recording with runtime bindings`() {
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
            val recording = toggle("History recording")
            assertFalse(history.isChecked)
            history.isChecked = true
            assertTrue(settings.operations.performance.historyOverlayEnabled)
            assertFalse(settings.operations.performance.historyRecording)
            recording.isChecked = true
            history.isChecked = false
            assertTrue(settings.operations.performance.historyRecording)
            assertFalse(settings.operations.performance.overlayEnabled)
            settings.operations.performance.historyOverlayEnabled = true
            settings.operations.performance.historyRecording = false
            bindings.sync()
            assertTrue(history.isChecked)
            assertFalse(recording.isChecked)
            assertTrue(labels.any { it.text.toString() == "Clear performance history" })
        } finally {
            monitor.dispose()
            ui.dispose()
            skin.dispose()
        }
    }

    private fun descendants(actor: Actor): List<Actor> = listOf(actor) +
        if (actor is Group) actor.children.flatMap(::descendants) else emptyList()
}
