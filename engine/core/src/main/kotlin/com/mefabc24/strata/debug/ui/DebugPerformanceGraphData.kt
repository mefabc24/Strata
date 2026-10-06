package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugPerformanceSummary
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Reusable, resolution-bounded view of a chronological numeric series. */
internal class DebugPerformanceGraphData {
    private var indices = IntArray(0)
    private var values = DoubleArray(0)
    var pointCount = 0
        private set
    var sampleCount = 0
        private set
    val minimumY = 0.0
    var maximumY = 1.0
        private set
    var summary: DebugPerformanceSummary? = null
        private set
    var current: Double? = null
        private set

    fun sampleIndex(point: Int): Int = indices[point]
    fun value(point: Int): Double = values[point]

    /**
     * Reads the complete series once; only bucket extrema become plotted points.
     *
     * [minimumRange] is the smallest upper bound, in the series' unit, used for idle or zero series.
     */
    fun update(count: Int, plotWidth: Int, minimumRange: Double = 0.1, sampleAt: (Int) -> Double) {
        require(count >= 0)
        require(minimumRange.isFinite() && minimumRange > 0.0)
        sampleCount = count
        pointCount = 0
        if (count == 0) {
            summary = null
            current = null
            maximumY = maxOf(1.0, minimumRange)
            return
        }
        val buckets = maxOf(1, plotWidth / 2)
        val limit = minOf(count, buckets * 2 + 2)
        if (indices.size < limit) {
            indices = IntArray(limit)
            values = DoubleArray(limit)
        }
        var sum = 0.0
        var minimum = Double.POSITIVE_INFINITY
        var maximum = 0.0
        var bucket = -1
        var minIndex = 0
        var maxIndex = 0
        var minValue = 0.0
        var maxValue = 0.0

        fun emit(index: Int, value: Double) {
            if (pointCount > 0 && indices[pointCount - 1] == index) return
            indices[pointCount] = index
            values[pointCount++] = value
        }
        fun emitBucket() {
            if (minIndex <= maxIndex) {
                emit(minIndex, minValue)
                emit(maxIndex, maxValue)
            } else {
                emit(maxIndex, maxValue)
                emit(minIndex, minValue)
            }
        }

        for (index in 0 until count) {
            val value = sampleAt(index)
            require(value.isFinite() && value >= 0.0) { "Graph samples must be finite and non-negative." }
            sum += value
            minimum = minOf(minimum, value)
            maximum = maxOf(maximum, value)
            if (count <= limit) {
                emit(index, value)
            } else {
                val nextBucket = (index.toLong() * buckets / count).toInt()
                if (nextBucket != bucket) {
                    if (bucket >= 0) emitBucket()
                    bucket = nextBucket
                    minIndex = index
                    maxIndex = index
                    minValue = value
                    maxValue = value
                }
                if (value < minValue) { minValue = value; minIndex = index }
                if (value > maxValue) { maxValue = value; maxIndex = index }
                if (index == 0) emit(index, value)
                if (index == count - 1) {
                    emitBucket()
                    emit(index, value)
                }
            }
            current = value
        }
        summary = DebugPerformanceSummary(count, sum / count, minimum, maximum)
        val desired = niceUpperBound(maximum, minimumRange)
        // Below 30%, even a 2-to-5 scale step cannot oscillate near its headroom boundary.
        if (desired > maximumY || maximum < maximumY * 0.30) maximumY = desired
    }

    private fun niceUpperBound(maximum: Double, minimumRange: Double): Double {
        val target = maxOf(minimumRange, maximum * 1.1)
        val magnitude = 10.0.pow(floor(log10(target)))
        val fraction = target / magnitude
        val step = when {
            fraction <= 1.0 -> 1.0
            fraction <= 2.0 -> 2.0
            fraction <= 5.0 -> 5.0
            else -> 10.0
        }
        return ceil(step) * magnitude
    }
}
