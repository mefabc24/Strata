package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugPerformanceMetric
import com.mefabc24.strata.debug.DebugPerformanceSettings
import com.mefabc24.strata.debug.DebugPerformanceSummary
import com.mefabc24.strata.render.RenderStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugPerformanceRecordingOverviewTest {
    @Test
    fun `overview requires a completed nonempty recording`() {
        val settings = DebugPerformanceSettings().apply { historyOverlayEnabled = true }
        val state = DebugPerformanceHistoryOverlayState(settings)
        state.showRecording()
        assertFalse(state.viewingRecording)
        settings.startHistoryRecording()
        settings.stopHistoryRecording()
        assertFalse(state.canViewRecording)
        settings.startHistoryRecording()
        settings.record(RenderStats(), 0.02f)
        state.showRecording()
        assertFalse(state.viewingRecording)
        settings.stopHistoryRecording()
        assertTrue(state.canViewRecording)
        state.showRecording()
        assertTrue(state.viewingRecording)
    }

    @Test
    fun `overview spans the full thousand sample recording with complete statistics`() {
        val settings = recording()
        val state = DebugPerformanceHistoryOverlayState(settings)
        assertTrue(state.update(0f, 100))
        val liveCard = state.graphs.single()
        assertEquals(240, liveCard.data.sampleCount)
        assertEquals(DebugPerformanceSummary(240, 880.5, 761.0, 1000.0), liveCard.data.summary)

        state.showRecording()
        assertTrue(state.update(0f, 100))
        assertSame(liveCard, state.graphs.single(), "Both views reuse the graph")
        val data = state.graphs.single().data
        assertEquals(1_000, data.sampleCount)
        assertEquals(0, data.sampleIndex(0))
        assertEquals(999, data.sampleIndex(data.pointCount - 1))
        assertEquals(1.0, data.value(0))
        assertEquals(1000.0, data.value(data.pointCount - 1))
        assertEquals(DebugPerformanceSummary(1_000, 500.5, 1.0, 1000.0), data.summary)
        assertTrue(data.pointCount <= 102, "Rendering stays bounded by plot width")

        settings.record(RenderStats().apply { cpuRenderMs = 2000.0 }, 0.01f)
        assertFalse(state.update(1f, 100), "Finished recordings need no periodic reprocessing")
        assertEquals(1000.0, data.current)
        assertTrue(state.update(0f, 60), "Resizing refreshes even a finished recording")
        assertEquals(1000, state.graphs.single().data.sampleCount)
        state.showLive()
        assertTrue(state.update(0f, 60))
        assertEquals(240, liveCard.data.sampleCount)
        assertEquals(2000.0, liveCard.data.current)
    }

    @Test
    fun `metric selection and adding or removing graphs inspect the full recorded session`() {
        val settings = recording()
        val state = DebugPerformanceHistoryOverlayState(settings)
        state.showRecording()
        state.update(0f, 100)
        val first = state.graphs.single()
        first.graph.metric = DebugPerformanceMetric.ENTITIES_DRAWN
        assertTrue(state.update(0f, 100))
        assertEquals(DebugPerformanceSummary(1000, 500.5, 1.0, 1000.0), first.data.summary)

        val added = settings.historyGraphs.add(DebugPerformanceMetric.FRAMES_PER_SECOND)
        assertTrue(state.update(0f, 100))
        assertSame(first, state.graphs.first())
        assertSame(added, state.graphs.last().graph)
        assertEquals(1000, state.graphs.last().data.sampleCount)
        assertEquals(100.0, state.graphs.last().data.summary!!.average, 0.001)
        settings.historyGraphs.remove(first.graph)
        assertTrue(state.update(0f, 100))
        assertEquals(1000, state.graphs.single().data.sampleCount)
        assertEquals(1000, settings.history.recordingData!!.size)
    }

    @Test
    fun `starting or clearing while viewing a recording returns to live immediately`() {
        for (start in listOf(true, false)) {
            val settings = recording()
            val state = DebugPerformanceHistoryOverlayState(settings)
            state.showRecording()
            state.update(0f, 100)
            if (start) settings.startHistoryRecording() else settings.clearHistory()
            assertTrue(state.update(0f, 100))
            assertFalse(state.viewingRecording)
            assertFalse(state.canViewRecording)
            assertEquals(if (start) 240 else 0, state.graphs.single().data.sampleCount)
        }
    }

    @Test
    fun `live length changes and hiding do not alter the overview timeline`() {
        val settings = recording()
        val state = DebugPerformanceHistoryOverlayState(settings)
        state.showRecording()
        state.update(0f, 100)
        settings.historyLength = 1
        state.update(0f, 100)
        assertEquals(1000, state.graphs.single().data.sampleCount)
        settings.historyOverlayEnabled = false
        state.update(0f, 100)
        assertFalse(state.visible)
        settings.historyOverlayEnabled = true
        assertTrue(state.update(0f, 100))
        assertTrue(state.viewingRecording)
        assertEquals(1000, state.graphs.single().data.sampleCount)
    }

    private fun recording(): DebugPerformanceSettings = DebugPerformanceSettings().apply {
        historyOverlayEnabled = true
        historyGraphMetrics = listOf(DebugPerformanceMetric.RENDER_TIME)
        startHistoryRecording()
        val stats = RenderStats()
        for (sample in 1..1_000) {
            stats.cpuRenderMs = sample.toDouble()
            stats.entitiesDrawn = sample
            record(stats, 0.01f)
        }
        stopHistoryRecording()
    }
}
