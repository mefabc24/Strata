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
    fun `visible debug window reserves overlay space`() {
        assertEquals(380f, DebugWindowLayout.overlayRightInset(true, 372f))
        assertEquals(0f, DebugWindowLayout.overlayRightInset(false, 372f))
    }
}
