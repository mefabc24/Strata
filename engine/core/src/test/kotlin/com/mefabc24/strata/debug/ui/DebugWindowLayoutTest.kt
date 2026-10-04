package com.mefabc24.strata.debug.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class DebugWindowLayoutTest {
    @Test
    fun `debug window keeps its practical width on large screens`() {
        assertEquals(372f, DebugWindowLayout.debugWidth(1920f))
    }

    @Test
    fun `debug window shrinks to the available width`() {
        assertEquals(320f, DebugWindowLayout.debugWidth(320f))
        assertEquals(0f, DebugWindowLayout.debugWidth(0f))
    }

    @Test
    fun `overlay stack clears the rail and visible flyout`() {
        assertEquals(10f, DebugWindowLayout.TOOL_RAIL_MARGIN)
        assertEquals(
            16f,
            DebugWindowLayout.overlayLeftPadding(false, false)
        )
        assertEquals(
            86f,
            DebugWindowLayout.overlayLeftPadding(true, false)
        )
        assertEquals(
            426f,
            DebugWindowLayout.overlayLeftPadding(true, true)
        )
    }
}
