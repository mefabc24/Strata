package com.mefabc24.strata.debug

/**
 * All frame samples from one explicit performance recording, independent of live history length.
 *
 * Every metric shares the same chronological timeline, including metrics with no visible graph.
 * Storage grows as needed during capture and stays unchanged after Stop. Start replaces the session.
 */
class DebugPerformanceRecording internal constructor() {
    private var columns = Array(DebugPerformanceMetric.entries.size) { DoubleArray(256) }

    /** Total captured frames. */
    var size: Int = 0
        private set

    /** Sum of captured frame deltas in seconds, independent of simulation speed. */
    var durationSeconds: Double = 0.0
        private set

    /** Whether Stop has finalized this session. */
    var isComplete: Boolean = false
        private set

    internal fun append(sampledColumns: Array<DoubleArray>, index: Int, frameDelta: Float) {
        check(!isComplete)
        if (size == columns[0].size) {
            columns = Array(columns.size) { columns[it].copyOf(size * 2) }
        }
        for (metric in columns.indices) columns[metric][size] = sampledColumns[metric][index]
        size++
        durationSeconds += frameDelta.toDouble()
    }

    internal fun finish() {
        isComplete = true
    }

    internal fun sampleAt(metric: DebugPerformanceMetric, index: Int): Double {
        require(index in 0 until size)
        return columns[metric.ordinal][index]
    }

    /** Copies the complete oldest-to-newest series for one metric. */
    fun samples(metric: DebugPerformanceMetric): DoubleArray = columns[metric.ordinal].copyOf(size)

    /** Calculates statistics over the entire recording, including samples evicted from live history. */
    fun summary(metric: DebugPerformanceMetric): DebugPerformanceSummary? {
        if (size == 0) return null
        val values = columns[metric.ordinal]
        var sum = 0.0
        var minimum = Double.POSITIVE_INFINITY
        var maximum = Double.NEGATIVE_INFINITY
        for (index in 0 until size) {
            val value = values[index]
            sum += value
            minimum = minOf(minimum, value)
            maximum = maxOf(maximum, value)
        }
        return DebugPerformanceSummary(size, sum / size, minimum, maximum)
    }
}
