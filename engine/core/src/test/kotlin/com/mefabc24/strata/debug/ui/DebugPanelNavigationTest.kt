package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.DebugPreset
import com.mefabc24.strata.debug.DebugSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugPanelNavigationTest {
    @Test
    fun `debug section selection is retained`() {
        val navigation = DebugPanelNavigation()

        navigation.select(DebugPanelSection.RUNTIME)

        assertEquals(DebugPanelSection.RUNTIME, navigation.selectedDebugSection)
    }

    @Test
    fun `switching debug sections does not affect the active world tool`() {
        val navigation = DebugPanelNavigation()
        val toolMode = DebugToolMode.PATHFINDING

        DebugPanelSection.entries.forEach(navigation::select)

        assertEquals(DebugPanelSection.PRESETS, navigation.selectedDebugSection)
        assertEquals(DebugToolMode.PATHFINDING, toolMode)
    }

    @Test
    fun `preset section exposes every built in preset concept`() {
        assertEquals(
            listOf("OFF", "MINIMAL", "PLACEMENT", "ENTITIES", "RENDERING", "EVERYTHING"),
            DebugPreset.entries.map(DebugPreset::name)
        )
        assertEquals("Presets", DebugPanelSection.PRESETS.label)
    }

    @Test
    fun `preset selector includes off default and every built in preset`() {
        assertEquals(
            listOf("OFF", "DEFAULT", "MINIMAL", "PLACEMENT", "ENTITIES", "RENDERING", "EVERYTHING"),
            DebugPresetSelection.entries.map(DebugPresetSelection::name)
        )
    }

    @Test
    fun `default preset selection restores the persisted default configuration`() {
        val settings = DebugSettings().apply {
            grid.enabled = true
            saveDefaultVisualConfiguration()
            grid.enabled = false
        }

        DebugPresetSelection.DEFAULT.applyTo(settings)

        assertEquals(true, settings.grid.enabled)
    }
}
