package com.mefabc24.strata.debug.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugPerformanceGraphDataTest {
    @Test
    fun `empty history has a usable zero based range`() {
        val graph = graph(doubleArrayOf())
        assertEquals(0, graph.pointCount)
        assertEquals(0.0, graph.minimumY)
        assertTrue(graph.maximumY > 0.0)
        assertNull(graph.summary)
        assertNull(graph.current)
    }

    @Test
    fun `single and constant samples have headroom`() {
        for (values in listOf(doubleArrayOf(16.0), DoubleArray(20) { 16.0 }, DoubleArray(20))) {
            val graph = graph(values)
            assertEquals(values.size, graph.pointCount)
            assertEquals(values.last(), graph.current)
            assertTrue(graph.maximumY > values.max())
            assertEquals(values.first(), graph.summary!!.average)
        }
    }

    @Test
    fun `varying values retain order and complete summary`() {
        val values = doubleArrayOf(3.0, 1.0, 8.0, 4.0)
        val graph = graph(values)
        assertEquals(values.toList(), (0 until graph.pointCount).map(graph::value))
        assertEquals(values.indices.toList(), (0 until graph.pointCount).map(graph::sampleIndex))
        assertEquals(4.0, graph.summary!!.average)
        assertEquals(1.0, graph.summary!!.minimum)
        assertEquals(8.0, graph.summary!!.maximum)
        assertTrue(graph.maximumY > 8.0)
    }

    @Test
    fun `large histories preserve spikes dips endpoints and chronological order`() {
        val values = DoubleArray(16_384) { 4.0 }.apply {
            this[753] = 500.0
            this[756] = 0.0
        }
        val graph = graph(values, 240)
        val indices = (0 until graph.pointCount).map(graph::sampleIndex)
        assertTrue(graph.pointCount <= 242)
        assertEquals(0, indices.first())
        assertEquals(values.lastIndex, indices.last())
        assertTrue(indices.zipWithNext().all { (a, b) -> a < b })
        assertTrue(753 in indices)
        assertTrue(756 in indices)
        assertEquals(500.0, graph.summary!!.maximum)
        assertEquals(values.size, graph.summary!!.samples)
        assertTrue(graph.maximumY > 500.0)
    }

    @Test
    fun `bucket extrema retain their original order in either direction`() {
        for (values in listOf(
            doubleArrayOf(5.0, 10.0, 1.0, 5.0),
            doubleArrayOf(5.0, 1.0, 10.0, 5.0)
        )) {
            val graph = graph(values, 2)
            assertEquals(values.toList(), (0 until graph.pointCount).map(graph::value))
        }
    }

    @Test
    fun `tiny plot widths remain bounded`() {
        val graph = graph(DoubleArray(1000) { it.toDouble() }, 0)
        assertTrue(graph.pointCount <= 4)
        assertEquals(0, graph.sampleIndex(0))
        assertEquals(999, graph.sampleIndex(graph.pointCount - 1))
    }

    @Test
    fun `range does not oscillate across a headroom boundary`() {
        val graph = graph(doubleArrayOf(17.0))
        graph.update(1, 100) { 18.3 }
        val expanded = graph.maximumY
        repeat(4) {
            graph.update(1, 100) { 17.0 }
            assertEquals(expanded, graph.maximumY)
            graph.update(1, 100) { 18.3 }
            assertEquals(expanded, graph.maximumY)
        }
    }

    @Test
    fun `range is stable for small changes expands for spikes and recovers`() {
        val graph = graph(doubleArrayOf(16.0, 17.0))
        val upper = graph.maximumY
        graph.update(2, 100) { 17.1 }
        assertEquals(upper, graph.maximumY)
        graph.update(2, 100) { 150.0 }
        assertTrue(graph.maximumY > 150.0)
        graph.update(2, 100) { 16.0 }
        assertEquals(upper, graph.maximumY)
        graph.update(0, 100) { error("No empty samples should be read") }
        assertEquals(1.0, graph.maximumY)
    }

    @Test
    fun `minimum range keeps idle series readable in their unit`() {
        val counts = DebugPerformanceGraphData()
        counts.update(3, 100, minimumRange = 2.0) { 0.0 }
        assertEquals(2.0, counts.maximumY)
        assertEquals(0.0, counts.current)
        counts.update(0, 100, minimumRange = 2.0) { error("No empty samples should be read") }
        assertEquals(2.0, counts.maximumY)
        val timings = graph(DoubleArray(3))
        assertEquals(0.1, timings.maximumY, 0.000001)
        val large = DebugPerformanceGraphData().apply { update(1, 100, minimumRange = 2.0) { 65_536.0 } }
        assertEquals(100_000.0, large.maximumY)
    }

    private fun graph(values: DoubleArray, width: Int = 100) =
        DebugPerformanceGraphData().apply { update(values.size, width) { values[it] } }
}
