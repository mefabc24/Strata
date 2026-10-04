package com.mefabc24.strata.debug.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DebugPanelHierarchyTest {
    @Test
    fun `all debug groups share one continuous hierarchy`() {
        val labels = DebugHierarchyGroup.entries.map(DebugHierarchyGroup::label)

        assertEquals(
            listOf(
                "General",
                "World",
                "Scene elements",
                "Camera",
                "Performance",
                "Simulation",
                "Diagnostics",
                "Presets"
            ),
            labels
        )
        assertFalse("Visuals" in labels)
        assertFalse("Runtime" in labels)
    }
}
