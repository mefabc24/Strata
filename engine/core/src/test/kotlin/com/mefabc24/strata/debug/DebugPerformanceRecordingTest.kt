package com.mefabc24.strata.debug

import com.mefabc24.strata.render.RenderStats
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugPerformanceRecordingTest {
    @Test
    fun `live history rolls before during and after a recording`() {
        val settings = DebugPerformanceSettings().apply { historyLength = 2 }
        record(settings, 1..3)
        assertContentEquals(doubleArrayOf(2.0, 3.0), settings.history.samples(METRIC))
        assertNull(settings.history.recordingData)

        settings.startHistoryRecording()
        assertContentEquals(doubleArrayOf(2.0, 3.0), settings.history.samples(METRIC))
        record(settings, 4..6)
        assertContentEquals(doubleArrayOf(5.0, 6.0), settings.history.samples(METRIC))
        settings.stopHistoryRecording()
        record(settings, 7..8)

        assertContentEquals(doubleArrayOf(7.0, 8.0), settings.history.samples(METRIC))
        assertContentEquals(doubleArrayOf(4.0, 5.0, 6.0), settings.history.recordingData!!.samples(METRIC))
    }

    @Test
    fun `one thousand samples survive live capacity changes and stop`() {
        val settings = DebugPerformanceSettings().apply { historyLength = 240 }
        settings.startHistoryRecording()
        record(settings, 1..500)
        val session = settings.history.recordingData!!
        settings.historyLength = 10
        assertEquals(500, session.size)
        settings.historyLength = 240
        record(settings, 501..1_000)

        assertEquals(240, settings.history.size)
        assertEquals(761.0, settings.history.samples(METRIC).first())
        assertEquals(1_000, session.size)
        assertFalse(session.isComplete)
        settings.stopHistoryRecording()
        record(settings, 1_001..1_010)

        assertSame(session, settings.history.recordingData)
        assertTrue(session.isComplete)
        assertEquals(10.0, session.durationSeconds, 0.00001)
        assertContentEquals(DoubleArray(1_000) { it + 1.0 }, session.samples(METRIC))
        assertEquals(DebugPerformanceSummary(1_000, 500.5, 1.0, 1_000.0), session.summary(METRIC))
        assertEquals(890.5, settings.history.summary(METRIC)!!.average)
        settings.historyLength = 1
        assertEquals(1_000, session.size)
    }

    @Test
    fun `recording grows past the largest live history capacity`() {
        val settings = DebugPerformanceSettings().apply { historyLength = 1 }
        settings.startHistoryRecording()
        record(settings, 1..20_000)
        settings.stopHistoryRecording()
        val session = settings.history.recordingData!!
        assertEquals(1, settings.history.size)
        assertContentEquals(DoubleArray(20_000) { it + 1.0 }, session.samples(METRIC))
    }

    @Test
    fun `all metrics share identical sampled values independent of graph selection`() {
        val settings = DebugPerformanceSettings().apply { historyLength = 1 }
        val stats = RenderStats().apply {
            cpuRenderMs = 3.0
            entitiesDrawn = 19
            drawCalls = 7
            staticPlanMs = 0.25
        }
        settings.startHistoryRecording()
        settings.record(stats, 0.02f)
        settings.historyGraphs.add(DebugPerformanceMetric.ENTITIES_DRAWN)
        settings.historyGraphs.remove(settings.historyGraphs.graphs.first())
        settings.record(stats, 0.02f)
        settings.stopHistoryRecording()

        for (metric in DebugPerformanceMetric.entries) {
            val expected = metric.sample(stats, 0.02f)
            assertContentEquals(doubleArrayOf(expected), settings.history.samples(metric), metric.name)
            assertContentEquals(doubleArrayOf(expected, expected), settings.history.recordingData!!.samples(metric), metric.name)
        }
    }

    @Test
    fun `start replaces completed and active sessions without resetting live samples`() {
        val settings = DebugPerformanceSettings()
        settings.startHistoryRecording()
        record(settings, 1..3)
        settings.stopHistoryRecording()
        val completed = settings.history.recordingData!!

        settings.historyRecording = true
        val second = settings.history.recordingData!!
        assertNotSame(completed, second)
        assertEquals(0, second.size)
        assertEquals(0.0, second.durationSeconds)
        assertNull(second.summary(METRIC))
        record(settings, 4..4)
        settings.startHistoryRecording()
        val third = settings.history.recordingData!!
        assertNotSame(second, third)
        assertEquals(0, third.size)
        assertEquals(4, settings.history.size)
        record(settings, 5..5)
        settings.stopHistoryRecording()
        assertContentEquals(doubleArrayOf(5.0), third.samples(METRIC))
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0), completed.samples(METRIC))
    }

    @Test
    fun `clear while stopped removes recording and live history`() {
        val settings = DebugPerformanceSettings()
        settings.startHistoryRecording()
        record(settings, 1..3)
        settings.stopHistoryRecording()
        settings.clearHistory()

        assertNull(settings.history.recordingData)
        assertEquals(0, settings.history.size)
        assertFalse(settings.historyRecording)
        record(settings, 4..4)
        assertEquals(1, settings.history.size)
        assertNull(settings.history.recordingData)
    }

    @Test
    fun `clear while active resets both timelines and recording duration but keeps capturing`() {
        val settings = DebugPerformanceSettings()
        settings.startHistoryRecording()
        record(settings, 1..3)
        settings.clearHistory()

        assertTrue(settings.historyRecording)
        assertEquals(0, settings.history.size)
        val cleared = settings.history.recordingData!!
        assertEquals(0, cleared.size)
        assertEquals(0.0, cleared.durationSeconds)
        assertFalse(cleared.isComplete)
        record(settings, 4..4)
        settings.stopHistoryRecording()
        assertContentEquals(doubleArrayOf(4.0), cleared.samples(METRIC))
        assertEquals(0.01, cleared.durationSeconds, 0.000001)
    }

    @Test
    fun `empty and repeated stop retain coherent session state`() {
        val settings = DebugPerformanceSettings()
        settings.stopHistoryRecording()
        assertNull(settings.history.recordingData)
        settings.startHistoryRecording()
        settings.stopHistoryRecording()
        settings.stopHistoryRecording()
        val session = settings.history.recordingData!!
        assertTrue(session.isComplete)
        assertEquals(0, session.size)
        assertEquals(0.0, session.durationSeconds)
        assertNull(session.summary(METRIC))
        assertContentEquals(doubleArrayOf(), session.samples(METRIC))
    }

    @Test
    fun `invalid frame deltas do not modify either history`() {
        val settings = DebugPerformanceSettings()
        settings.startHistoryRecording()
        for (delta in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { settings.record(RenderStats(), delta) }
        }
        assertEquals(0, settings.history.size)
        assertEquals(0, settings.history.recordingData!!.size)
        settings.record(RenderStats(), 0f)
        assertEquals(1, settings.history.recordingData!!.size)
        assertEquals(0.0, settings.history.recordingData!!.durationSeconds)
    }

    @Test
    fun `sample copies cannot change the retained recording`() {
        val settings = DebugPerformanceSettings()
        settings.startHistoryRecording()
        record(settings, 1..2)
        settings.stopHistoryRecording()
        val recording = settings.history.recordingData!!
        recording.samples(METRIC)[0] = 9.0
        assertContentEquals(doubleArrayOf(1.0, 2.0), recording.samples(METRIC))
    }

    private fun record(settings: DebugPerformanceSettings, values: IntRange) {
        val stats = RenderStats()
        for (value in values) {
            stats.cpuRenderMs = value.toDouble()
            settings.record(stats, 0.01f)
        }
    }

    private companion object {
        val METRIC = DebugPerformanceMetric.RENDER_TIME
    }
}
