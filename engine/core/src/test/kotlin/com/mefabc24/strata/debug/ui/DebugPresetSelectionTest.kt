package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugPreset
import com.mefabc24.strata.debug.DebugSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugPresetSelectionTest {
    @Test
    fun `preset selector includes off default and every built in preset`() {
        assertEquals(
            listOf("OFF", "DEFAULT", "MINIMAL", "PLACEMENT", "ENTITIES", "RENDERING", "EVERYTHING"),
            DebugPresetSelection.entries.map(DebugPresetSelection::name)
        )
        assertEquals(
            listOf("OFF", "MINIMAL", "PLACEMENT", "ENTITIES", "RENDERING", "EVERYTHING"),
            DebugPreset.entries.map(DebugPreset::name)
        )
    }

    @Test
    fun `default preset selection restores the persisted default configuration`() {
        val settings = DebugSettings().apply {
            visuals.grid.enabled = true
            saveDefaultVisualConfiguration()
            visuals.grid.enabled = false
        }

        DebugPresetSelection.DEFAULT.applyTo(settings)

        assertEquals(true, settings.visuals.grid.enabled)
    }
}
