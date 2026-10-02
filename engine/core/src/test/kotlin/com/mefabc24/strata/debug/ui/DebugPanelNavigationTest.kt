package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.DebugPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugPanelNavigationTest {
    @Test
    fun `switching from tools to debug changes only the selected tab`() {
        val navigation = DebugPanelNavigation()

        navigation.select(DebugPanelTab.DEBUG)
        navigation.select(DebugPanelTab.DEBUG)

        assertEquals(DebugPanelTab.DEBUG, navigation.selectedTab)
    }

    @Test
    fun `tab navigation preserves independently managed tool selection`() {
        val navigation = DebugPanelNavigation()
        val toolMode = DebugToolMode.BUILD

        navigation.select(DebugPanelTab.DEBUG)
        navigation.select(DebugPanelTab.TOOLS)

        assertEquals(DebugToolMode.BUILD, toolMode)
        assertEquals(DebugPanelTab.TOOLS, navigation.selectedTab)
    }

    @Test
    fun `secondary section survives primary tab navigation`() {
        val navigation = DebugPanelNavigation()

        navigation.select(DebugPanelTab.DEBUG)
        navigation.select(DebugPanelSection.RUNTIME)
        navigation.select(DebugPanelTab.TOOLS)
        navigation.select(DebugPanelTab.DEBUG)

        assertEquals(DebugPanelTab.DEBUG, navigation.selectedTab)
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
}
