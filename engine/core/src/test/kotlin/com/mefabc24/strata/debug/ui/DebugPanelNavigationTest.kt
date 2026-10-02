package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugToolMode
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
}
