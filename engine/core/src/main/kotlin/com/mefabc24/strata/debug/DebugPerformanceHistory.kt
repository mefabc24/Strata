package com.mefabc24.strata.debug

import com.mefabc24.strata.render.RenderStats

/** Aggregate of one metric's retained samples, in that metric's unit. */
data class DebugPerformanceSummary(
    val samples: Int,
    val average: Double,
    val minimum: Double,
    val maximum: Double
)

/**
 * Rolling live history for per-frame performance measurements.
 *
 * Every [DebugPerformanceMetric] is sampled once per frame. The same values also feed an optional
 * full [recordingData] session; [capacity] only limits the live history.
 */
class DebugPerformanceHistory internal constructor(initialCapacity: Int = 240) {
    private var columns = Array(METRICS.size) { DoubleArray(initialCapacity) }
    private var writeIndex = 0

    var size: Int = 0
        private set

    /** Current or most recently completed session; replaced by Start and removed by Clear. */
    var recordingData: DebugPerformanceRecording? = null
        private set

    /** Starts a fresh session when set to true; false finalizes it without pausing live history. */
    var recording: Boolean = false
        set(value) {
            if (value) recordingData = DebugPerformanceRecording()
            else recordingData?.finish()
            field = value
            structureRevision++
        }

    /** Changes that should update a visible graph immediately, outside its refresh timer. */
    internal var structureRevision: Long = 0
        private set

    init {
        require(initialCapacity in 1..16_384) {
            "Performance history length must be between 1 and 16384."
        }
    }

    var capacity: Int
        get() = columns[0].size
        set(value) {
            require(value in 1..16_384) {
                "Performance history length must be between 1 and 16384."
            }
            if (value == capacity) return
            resize(value)
        }

    internal fun record(stats: RenderStats, frameDelta: Float) {
        require(frameDelta.isFinite() && frameDelta >= 0f) {
            "Performance frame delta must be finite and non-negative."
        }
        for (metric in METRICS.indices) {
            columns[metric][writeIndex] = METRICS[metric].sample(stats, frameDelta)
        }
        if (recording) recordingData?.append(columns, writeIndex, frameDelta)
        writeIndex = (writeIndex + 1) % capacity
        if (size < capacity) size++
    }

    /** Clears both timelines, starting an empty session if recording is still active. */
    fun clear() {
        writeIndex = 0
        size = 0
        recordingData = if (recording) DebugPerformanceRecording() else null
        structureRevision++
    }

    /** Allocation-free chronological access for visualization; underlying history is retained. */
    internal fun sampleAt(metric: DebugPerformanceMetric, index: Int): Double {
        require(index in 0 until size)
        val first = Math.floorMod(writeIndex - size, capacity)
        return columns[metric.ordinal][(first + index) % capacity]
    }

    fun summary(metric: DebugPerformanceMetric): DebugPerformanceSummary? {
        if (size == 0) return null
        val values = columns[metric.ordinal]
        var sum = 0.0
        var minimum = Double.POSITIVE_INFINITY
        var maximum = Double.NEGATIVE_INFINITY
        forEachIndex { index ->
            val value = values[index]
            sum += value
            minimum = minOf(minimum, value)
            maximum = maxOf(maximum, value)
        }
        return DebugPerformanceSummary(size, sum / size, minimum, maximum)
    }

    /** Copies oldest-to-newest samples for infrequent UI refreshes. */
    fun samples(
        metric: DebugPerformanceMetric,
        maximumSamples: Int = size
    ): DoubleArray {
        require(maximumSamples >= 0) { "Maximum performance samples must be non-negative." }
        if (size == 0 || maximumSamples == 0) return DoubleArray(0)
        return newest(columns[metric.ordinal], minOf(size, maximumSamples))
    }

    private fun resize(newCapacity: Int) {
        val retained = minOf(size, newCapacity)
        columns = Array(columns.size) { metric ->
            newest(columns[metric], retained).copyInto(DoubleArray(newCapacity))
        }
        size = retained
        writeIndex = retained % newCapacity
        structureRevision++
    }

    private fun newest(source: DoubleArray, count: Int): DoubleArray {
        if (count == 0) return DoubleArray(0)
        val result = DoubleArray(count)
        val skip = size - count
        var output = 0
        var logical = 0
        forEachIndex { index ->
            if (logical++ >= skip) result[output++] = source[index]
        }
        return result
    }

    private inline fun forEachIndex(action: (Int) -> Unit) {
        val first = Math.floorMod(writeIndex - size, capacity)
        repeat(size) { offset -> action((first + offset) % capacity) }
    }

    private companion object {
        val METRICS = DebugPerformanceMetric.entries
    }
}
