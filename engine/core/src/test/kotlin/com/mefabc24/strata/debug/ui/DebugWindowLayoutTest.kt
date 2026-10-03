package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugWindowLayoutTest {
    @Test
    fun `default order places tools before debug`() {
        val settings = visibleWindows()

        assertEquals(
            listOf(DebugWindowKind.TOOLS, DebugWindowKind.DEBUG),
            orderedVisibleDebugWindows(settings)
        )
    }

    @Test
    fun `configured order can place debug before tools`() {
        val settings = visibleWindows().apply {
            toolsWindow.order = 10
            debugWindow.order = -2
        }

        assertEquals(
            listOf(DebugWindowKind.DEBUG, DebugWindowKind.TOOLS),
            orderedVisibleDebugWindows(settings)
        )
    }

    @Test
    fun `equal values have deterministic tools first layout before validation`() {
        val settings = visibleWindows().apply {
            toolsWindow.order = 3
            debugWindow.order = 3
        }

        assertEquals(
            listOf(DebugWindowKind.TOOLS, DebugWindowKind.DEBUG),
            orderedVisibleDebugWindows(settings)
        )
    }

    @Test
    fun `hidden and disabled windows reserve no layout position`() {
        val settings = visibleWindows()

        settings.toolsWindow.visible = false
        assertEquals(
            listOf(DebugWindowKind.DEBUG),
            orderedVisibleDebugWindows(settings)
        )

        settings.toolsWindow.visible = true
        settings.debugWindow.enabled = false
        assertEquals(
            listOf(DebugWindowKind.TOOLS),
            orderedVisibleDebugWindows(settings)
        )
    }

    private fun visibleWindows() = DebugSettings().apply {
        toolsWindow.enabled = true
        toolsWindow.visible = true
        debugWindow.enabled = true
        debugWindow.visible = true
    }
}
