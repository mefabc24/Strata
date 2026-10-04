package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugPreset
import com.mefabc24.strata.debug.DebugSettings

internal enum class DebugPresetSelection(val label: String) {
    OFF("OFF"),
    DEFAULT("DEFAULT"),
    MINIMAL("Minimal"),
    PLACEMENT("Placement"),
    ENTITIES("Entities"),
    RENDERING("Rendering"),
    EVERYTHING("Everything");

    fun applyTo(settings: DebugSettings) {
        when (this) {
            DEFAULT -> settings.applyDefaultVisualConfiguration()
            else -> settings.applyPreset(DebugPreset.valueOf(name))
        }
    }
}
