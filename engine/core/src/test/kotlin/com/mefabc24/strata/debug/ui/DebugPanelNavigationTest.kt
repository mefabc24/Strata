package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugToolMode
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugPanelNavigationTest {
    @Test
    fun `switching from tools to debug resets tool mode once`() {
        val navigation = DebugPanelNavigation()
        var resets = 0

        navigation.select(DebugPanelTab.DEBUG) { resets++ }
        navigation.select(DebugPanelTab.DEBUG) { resets++ }

        assertEquals(DebugPanelTab.DEBUG, navigation.selectedTab)
        assertEquals(1, resets)
    }

    @Test
    fun `returning to tools does not reactivate a tool`() {
        val navigation = DebugPanelNavigation()
        var toolMode = DebugToolMode.BUILD

        navigation.select(DebugPanelTab.DEBUG) { toolMode = DebugToolMode.NONE }
        navigation.select(DebugPanelTab.TOOLS) { toolMode = DebugToolMode.INSPECT }

        assertEquals(DebugToolMode.NONE, toolMode)
    }
}
