package com.mefabc24.strata.debug

import com.mefabc24.strata.render.RenderStats
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugPerformanceHistoryGraphsTest {
    @Test
    fun `history starts with one frame time graph that cannot be removed`() {
        val graphs = DebugPerformanceHistoryGraphs()
        assertEquals(listOf(DebugPerformanceMetric.FRAME_TIME), graphs.metrics)
        assertTrue(graphs.canAdd)
        assertFalse(graphs.canRemove)
        assertFailsWith<IllegalStateException> { graphs.remove(graphs.graphs.single()) }
        assertEquals(1, graphs.graphs.size)
    }

    @Test
    fun `added graphs default to metrics that are not graphed yet`() {
        val graphs = DebugPerformanceHistoryGraphs()
        val second = graphs.add()
        assertEquals(DebugPerformanceMetric.FRAMES_PER_SECOND, second.metric)
        val third = graphs.add(DebugPerformanceMetric.ENTITIES_DRAWN)
        assertEquals(DebugPerformanceMetric.ENTITIES_DRAWN, third.metric)
        assertEquals(DebugPerformanceMetric.RENDER_TIME, graphs.add().metric)
        assertEquals(
            listOf(
                DebugPerformanceMetric.FRAME_TIME,
                DebugPerformanceMetric.FRAMES_PER_SECOND,
                DebugPerformanceMetric.ENTITIES_DRAWN,
                DebugPerformanceMetric.RENDER_TIME
            ),
            graphs.metrics
        )
        assertTrue(graphs.canRemove)
    }

    @Test
    fun `graph count is bounded`() {
        val graphs = DebugPerformanceHistoryGraphs()
        repeat(DebugPerformanceHistoryGraphs.MAXIMUM_GRAPHS - 1) { graphs.add() }
        assertFalse(graphs.canAdd)
        assertFailsWith<IllegalStateException> { graphs.add() }
        assertEquals(DebugPerformanceHistoryGraphs.MAXIMUM_GRAPHS, graphs.graphs.size)
        assertFailsWith<IllegalArgumentException> { graphs.metrics = emptyList() }
        assertFailsWith<IllegalArgumentException> {
            graphs.metrics = List(DebugPerformanceHistoryGraphs.MAXIMUM_GRAPHS + 1) { DebugPerformanceMetric.FRAME_TIME }
        }
    }

    @Test
    fun `removing a graph keeps the identity and metric of every other graph`() {
        val graphs = DebugPerformanceHistoryGraphs()
        val first = graphs.graphs.single()
        val middle = graphs.add(DebugPerformanceMetric.OBJECTS_DRAWN)
        val last = graphs.add(DebugPerformanceMetric.ENTITIES_TOTAL)
        val revision = graphs.revision

        graphs.remove(middle)

        assertEquals(listOf(first, last), graphs.graphs)
        assertEquals(
            listOf(DebugPerformanceMetric.FRAME_TIME, DebugPerformanceMetric.ENTITIES_TOTAL),
            graphs.metrics
        )
        assertNotEquals(revision, graphs.revision)
        assertFailsWith<IllegalArgumentException> { graphs.remove(middle) }
    }

    @Test
    fun `changing one graph metric leaves the other graphs unchanged`() {
        val graphs = DebugPerformanceHistoryGraphs()
        val second = graphs.add(DebugPerformanceMetric.DRAW_CALLS)
        graphs.graphs.first().metric = DebugPerformanceMetric.GROUND_DRAWN
        assertEquals(DebugPerformanceMetric.DRAW_CALLS, second.metric)
        assertEquals(
            listOf(DebugPerformanceMetric.GROUND_DRAWN, DebugPerformanceMetric.DRAW_CALLS),
            graphs.metrics
        )
    }

    @Test
    fun `assigning metrics reuses graphs in order and trims or extends the list`() {
        val graphs = DebugPerformanceHistoryGraphs()
        val first = graphs.graphs.single()
        graphs.metrics = listOf(DebugPerformanceMetric.RENDER_TIME, DebugPerformanceMetric.DRAW_CALLS)
        assertSame(first, graphs.graphs[0])
        assertEquals(DebugPerformanceMetric.RENDER_TIME, first.metric)
        graphs.metrics = listOf(DebugPerformanceMetric.OVERLAYS_DRAWN)
        assertEquals(listOf(first), graphs.graphs)
        assertEquals(listOf(DebugPerformanceMetric.OVERLAYS_DRAWN), graphs.metrics)
    }

    @Test
    fun `all graphs read one shared recording and graph changes keep captured samples`() {
        val settings = DebugPerformanceSettings()
        settings.historyGraphMetrics = listOf(
            DebugPerformanceMetric.FRAME_TIME,
            DebugPerformanceMetric.FRAMES_PER_SECOND,
            DebugPerformanceMetric.ENTITIES_DRAWN
        )
        settings.startHistoryRecording()
        settings.record(RenderStats().apply { entitiesDrawn = 5 }, 0.02f)
        settings.record(RenderStats().apply { entitiesDrawn = 8 }, 0.025f)

        settings.historyGraphs.remove(settings.historyGraphs.graphs[1])
        settings.historyGraphs.add(DebugPerformanceMetric.DRAW_CALLS)

        assertEquals(2, settings.history.size)
        assertContentEquals(doubleArrayOf(5.0, 8.0), settings.history.samples(DebugPerformanceMetric.ENTITIES_DRAWN))
        assertEquals(40.0, settings.history.samples(DebugPerformanceMetric.FRAMES_PER_SECOND)[1], 0.001)
    }
}
