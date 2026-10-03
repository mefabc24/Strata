package com.mefabc24.strata.debug.ui

internal enum class DebugPanelSection(val label: String) {
    VISUALS("Visuals"),
    DIAGNOSTICS("Diagnostics"),
    RUNTIME("Runtime"),
    PRESETS("Presets")
}

/** Tracks the Debug window subsection independently from Tools window state. */
internal class DebugPanelNavigation {
    var selectedDebugSection: DebugPanelSection = DebugPanelSection.VISUALS
        private set

    fun select(section: DebugPanelSection) {
        selectedDebugSection = section
    }
}
