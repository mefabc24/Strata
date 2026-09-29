package com.mefabc24.strata.debug.ui

internal enum class DebugPanelTab(val label: String) { TOOLS("Tools"), DEBUG("Debug") }

/** Tracks tab transitions that require active world tools to be released. */
internal class DebugPanelNavigation {
    var selectedTab: DebugPanelTab = DebugPanelTab.TOOLS
        private set

    fun select(tab: DebugPanelTab, leaveTools: () -> Unit) {
        if (selectedTab == DebugPanelTab.TOOLS && tab == DebugPanelTab.DEBUG) {
            leaveTools()
        }
        selectedTab = tab
    }
}
