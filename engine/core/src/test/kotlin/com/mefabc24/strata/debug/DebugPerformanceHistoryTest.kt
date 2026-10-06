package com.mefabc24.strata.debug

import com.mefabc24.strata.render.RenderStats
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DebugPerformanceHistoryTest {
    @Test
    fun `bounded history retains newest values and aggregates every metric`() {
        val history = DebugPerformanceHistory(3).apply { recording = true }
        record(history, frameSeconds = 0.010f, render = 1.0, static = 0.1, dynamic = 0.5)
        record(history, frameSeconds = 0.020f, render = 2.0, static = 0.2, dynamic = 1.0)
        record(history, frameSeconds = 0.030f, render = 3.0, static = 0.3, dynamic = 1.5)
        record(history, frameSeconds = 0.040f, render = 4.0, static = 0.4, dynamic = 2.0)

        assertEquals(3, history.size)
        val frames = history.samples(DebugPerformanceMetric.FRAME_TIME)
        assertEquals(20.0, frames[0], 0.001)
        assertEquals(30.0, frames[1], 0.001)
        assertEquals(40.0, frames[2], 0.001)
        assertEquals(
            DebugPerformanceSummary(3, 3.0, 2.0, 4.0),
            history.summary(DebugPerformanceMetric.RENDER_TIME)
        )
        assertEquals(0.2, history.summary(DebugPerformanceMetric.STATIC_PLAN_TIME)!!.minimum)
        assertEquals(2.0, history.summary(DebugPerformanceMetric.DYNAMIC_PLAN_TIME)!!.maximum)
    }

    @Test
    fun `every frame captures count and rate metrics on the shared bounded timeline`() {
        val history = DebugPerformanceHistory(2).apply { recording = true }
        listOf(10 to 0.010f, 20 to 0.020f, 40 to 0.025f).forEach { (entities, frameSeconds) ->
            history.record(
                RenderStats().apply {
                    entitiesDrawn = entities
                    entitiesTotal = 50
                    drawCalls = entities / 10
                },
                frameSeconds
            )
        }

        assertEquals(2, history.size)
        assertContentEquals(doubleArrayOf(20.0, 40.0), history.samples(DebugPerformanceMetric.ENTITIES_DRAWN))
        assertContentEquals(doubleArrayOf(50.0, 50.0), history.samples(DebugPerformanceMetric.ENTITIES_TOTAL))
        assertContentEquals(doubleArrayOf(2.0, 4.0), history.samples(DebugPerformanceMetric.DRAW_CALLS))
        val rates = history.samples(DebugPerformanceMetric.FRAMES_PER_SECOND)
        assertEquals(50.0, rates[0], 0.001)
        assertEquals(40.0, rates[1], 0.001)
        assertEquals(
            DebugPerformanceSummary(2, 30.0, 20.0, 40.0),
            history.summary(DebugPerformanceMetric.ENTITIES_DRAWN)
        )
        DebugPerformanceMetric.entries.forEach { metric ->
            assertEquals(2, history.samples(metric).size, metric.name)
        }
        history.capacity = 1
        assertContentEquals(doubleArrayOf(40.0), history.samples(DebugPerformanceMetric.ENTITIES_DRAWN))
        assertEquals(40.0, history.samples(DebugPerformanceMetric.FRAMES_PER_SECOND)[0], 0.001)
    }

    @Test
    fun `resizing preserves the newest samples in chronological order`() {
        val history = DebugPerformanceHistory(4).apply { recording = true }
        (1..4).forEach { value -> record(history, render = value.toDouble()) }

        history.capacity = 2
        assertContentEquals(
            doubleArrayOf(3.0, 4.0),
            history.samples(DebugPerformanceMetric.RENDER_TIME)
        )

        history.capacity = 5
        record(history, render = 5.0)
        assertContentEquals(
            doubleArrayOf(3.0, 4.0, 5.0),
            history.samples(DebugPerformanceMetric.RENDER_TIME)
        )
    }

    @Test
    fun `stopping capture retains samples and clear resets aggregates`() {
        val history = DebugPerformanceHistory(4).apply { recording = true }
        record(history, render = 2.0)
        history.recording = false
        record(history, render = 9.0)

        assertEquals(1, history.size)
        assertEquals(2.0, history.summary(DebugPerformanceMetric.RENDER_TIME)?.average)

        history.clear()
        assertEquals(0, history.size)
        assertNull(history.summary(DebugPerformanceMetric.RENDER_TIME))
    }

    @Test
    fun `history limits reject invalid capacities and sample counts`() {
        assertFailsWith<IllegalArgumentException> { DebugPerformanceHistory(0) }
        val history = DebugPerformanceHistory()
        assertFailsWith<IllegalArgumentException> { history.capacity = 0 }
        assertFailsWith<IllegalArgumentException> { history.capacity = 16_385 }
        assertFailsWith<IllegalArgumentException> {
            history.samples(DebugPerformanceMetric.FRAME_TIME, -1)
        }
    }

    private fun record(
        history: DebugPerformanceHistory,
        frameSeconds: Float = 0.016f,
        render: Double = 0.0,
        static: Double = 0.0,
        dynamic: Double = 0.0
    ) {
        history.record(
            RenderStats().apply {
                cpuRenderMs = render
                staticPlanMs = static
                dynamicPlanMs = dynamic
            },
            frameSeconds
        )
    }
}
