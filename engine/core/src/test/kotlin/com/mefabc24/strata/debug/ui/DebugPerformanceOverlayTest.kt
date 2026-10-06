package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.render.RenderStats
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.mefabc24.strata.debug.DebugEventMonitor
import com.mefabc24.strata.debug.DebugEventMonitorSettings
import com.mefabc24.strata.debug.DebugPerformanceMetric
import com.mefabc24.strata.debug.DebugPerformanceSettings
import com.mefabc24.strata.event.EventBus
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugPerformanceOverlayTest {
    @Test
    fun `live overlay keeps only live rows through history state changes`() {
        TestGdxEnvironment.install()
        val skin = DebugPanelSkin.create()
        val ui = StrataUi(skin, DebugPanelSkin.theme())
        val settings = DebugPerformanceSettings().apply { overlayEnabled = true }
        val monitor = DebugEventMonitor(EventBus())
        try {
            val overlay = DebugStatsOverlay(
                ui, { RenderStats() }, settings, { settings.overlayEnabled }, { false },
                World(1, 1) { _, _ -> object : Tile {} }, null,
                monitor, DebugEventMonitorSettings(), { 60 }
            )
            overlay.update(0.016f)
            val initial = labels(ui.stage)
            assertTrue("FPS" in initial)
            assertFalse(initial.any { it.contains("History") || it.contains("Capture") || it.contains("Samples") })
            settings.startHistoryRecording()
            settings.historyOverlayEnabled = true
            settings.record(RenderStats(), 0.016f)
            overlay.update(0.25f)
            assertEquals(initial, labels(ui.stage))
            settings.historyMetric = DebugPerformanceMetric.STATIC_PLAN_TIME
            settings.stopHistoryRecording()
            overlay.update(0.25f)
            assertEquals(initial, labels(ui.stage))
            settings.clearHistory()
            overlay.update(0.25f)
            assertEquals(initial, labels(ui.stage))
        } finally {
            monitor.dispose()
            ui.dispose()
            skin.dispose()
        }
    }

    private fun labels(actor: Actor): List<String> = when (actor) {
        is Label -> listOf(actor.text.toString()).filter { it.isNotEmpty() && !it.endsWith(" ms") }
        is Group -> actor.children.flatMap(::labels)
        else -> emptyList()
    }

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
