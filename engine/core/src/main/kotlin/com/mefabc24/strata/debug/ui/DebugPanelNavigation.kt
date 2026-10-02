package com.mefabc24.strata.debug.ui

internal enum class DebugPanelTab(val label: String) { TOOLS("Tools"), DEBUG("Debug") }

/** Tracks the selected debug-panel tab independently from the active world tool. */
internal class DebugPanelNavigation {
    var selectedTab: DebugPanelTab = DebugPanelTab.TOOLS
        private set

    fun select(tab: DebugPanelTab) {
        selectedTab = tab
    }
}
