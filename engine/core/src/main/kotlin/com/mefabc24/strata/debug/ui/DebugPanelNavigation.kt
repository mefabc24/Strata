package com.mefabc24.strata.debug.ui

internal enum class DebugPanelTab(val label: String) { TOOLS("Tools"), DEBUG("Debug") }

internal enum class DebugPanelSection(val label: String) {
    VISUALS("Visuals"),
    DIAGNOSTICS("Diagnostics"),
    RUNTIME("Runtime"),
    PRESETS("Presets")
}

/** Tracks the selected debug-panel tab independently from the active world tool. */
internal class DebugPanelNavigation {
    var selectedTab: DebugPanelTab = DebugPanelTab.TOOLS
        private set

    var selectedDebugSection: DebugPanelSection = DebugPanelSection.VISUALS
        private set

    fun select(tab: DebugPanelTab) {
        selectedTab = tab
    }

    fun select(section: DebugPanelSection) {
        selectedDebugSection = section
    }
}
