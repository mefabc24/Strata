package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.render.RenderStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugPerformanceOverlayTest {
    @Test
    fun `visibility and sampling state follow enabled setting`() {
        val state = DebugPerformanceOverlayState()
        assertNull(state.update(false, 0.1f))
        assertFalse(state.visible)
        assertEquals(16.0, state.update(true, 0.016f)!!, 0.001)
        assertTrue(state.visible)
        state.update(true, 0.1f)
        assertNull(state.update(false, 0.1f))
        assertEquals(20.0, state.update(true, 0.02f)!!, 0.001)
    }

    @Test
    fun `configured interval controls subsequent overlay refreshes`() {
        val state = DebugPerformanceOverlayState()

        assertEquals(16.0, state.update(true, 0.016f, 0.25f)!!, 0.001)
        assertNull(state.update(true, 0.1f, 0.25f))
        assertNull(state.update(true, 0.14f, 0.25f))
        assertEquals(83.333, state.update(true, 0.01f, 0.25f)!!, 0.001)
    }

    @Test
    fun `changing interval affects the active refresh window`() {
        val state = DebugPerformanceOverlayState()

        state.update(true, 0.016f, 1f)
        assertNull(state.update(true, 0.1f, 1f))
        assertEquals(100.0, state.update(true, 0.1f, 0.15f)!!, 0.001)
    }

    @Test
    fun `snapshot format includes renderer metrics`() {
        val text = DebugPerformanceSnapshot.from(RenderStats(), 60, 16.666).format()
        assertTrue(text.contains("FPS: 60"))
        assertTrue(text.contains("Frame: 16.67 ms"))
        assertTrue(text.contains("Ground: 0/0"))
        assertTrue(text.contains("Entities: 0/0"))
    }

    @Test
    fun `diagnostic sections retain their vertical stack order without gaps`() {
        val state = DebugStatsOverlayState()
        state.sync(false, false, false)
        assertFalse(state.visible)
        assertEquals(emptyList(), state.visibleSections)
        state.sync(true, false, false)
        assertTrue(state.visible)
        assertTrue(state.performanceVisible)
        assertFalse(state.worldVisible)
        assertEquals(listOf(DebugStatsSection.PERFORMANCE), state.visibleSections)
        state.sync(false, true, true)
        assertFalse(state.performanceVisible)
        assertTrue(state.worldVisible)
        assertTrue(state.worldBecameVisible)
        assertTrue(state.eventMonitorVisible)
        assertEquals(
            listOf(DebugStatsSection.WORLD, DebugStatsSection.EVENT_BUS_MONITOR),
            state.visibleSections
        )
        state.sync(true, true, true)
        assertEquals(
            listOf(
                DebugStatsSection.PERFORMANCE,
                DebugStatsSection.WORLD,
                DebugStatsSection.EVENT_BUS_MONITOR
            ),
            state.visibleSections
        )
        state.sync(true, false, true)
        assertEquals(
            listOf(DebugStatsSection.PERFORMANCE, DebugStatsSection.EVENT_BUS_MONITOR),
            state.visibleSections
        )
        state.sync(false, false, true)
        assertEquals(listOf(DebugStatsSection.EVENT_BUS_MONITOR), state.visibleSections)
        state.sync(false, false, false)
        assertFalse(state.visible)
        assertFalse(state.worldVisible)
        assertFalse(state.eventMonitorVisible)
    }
}
