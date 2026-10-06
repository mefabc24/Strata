package com.mefabc24.strata.debug

import com.mefabc24.strata.render.RenderStats

enum class DebugPerformanceMetric {
    FRAME_TIME,
    RENDER_TIME,
    STATIC_PLAN_TIME,
    DYNAMIC_PLAN_TIME
}

data class DebugPerformanceSummary(
    val samples: Int,
    val averageMs: Double,
    val minimumMs: Double,
    val maximumMs: Double
)

/** Bounded allocation-free recorder for per-frame performance measurements. */
class DebugPerformanceHistory internal constructor(initialCapacity: Int = 240) {
    private var frameMs = DoubleArray(initialCapacity)
    private var renderMs = DoubleArray(initialCapacity)
    private var staticPlanMs = DoubleArray(initialCapacity)
    private var dynamicPlanMs = DoubleArray(initialCapacity)
    private var writeIndex = 0

    var size: Int = 0
        private set

    var recording: Boolean = false

    /** Changes that should update a visible graph immediately, outside its refresh timer. */
    internal var structureRevision: Long = 0
        private set

    init {
        require(initialCapacity in 1..16_384) {
            "Performance history length must be between 1 and 16384."
        }
    }

    var capacity: Int
        get() = frameMs.size
        set(value) {
            require(value in 1..16_384) {
                "Performance history length must be between 1 and 16384."
            }
            if (value == capacity) return
            resize(value)
        }

    internal fun record(stats: RenderStats, frameDelta: Float) {
        if (!recording) return
        require(frameDelta.isFinite() && frameDelta >= 0f) {
            "Performance frame delta must be finite and non-negative."
        }
        frameMs[writeIndex] = frameDelta * 1_000.0
        renderMs[writeIndex] = stats.cpuRenderMs
        staticPlanMs[writeIndex] = stats.staticPlanMs
        dynamicPlanMs[writeIndex] = stats.dynamicPlanMs
        writeIndex = (writeIndex + 1) % capacity
        if (size < capacity) size++
    }

    fun clear() {
        writeIndex = 0
        size = 0
        structureRevision++
    }

    /** Allocation-free chronological access for visualization; underlying history is retained. */
    internal fun sampleAt(metric: DebugPerformanceMetric, index: Int): Double {
        require(index in 0 until size)
        val first = Math.floorMod(writeIndex - size, capacity)
        return values(metric)[(first + index) % capacity]
    }

    fun summary(metric: DebugPerformanceMetric): DebugPerformanceSummary? {
        if (size == 0) return null
        val values = values(metric)
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
        val count = minOf(size, maximumSamples)
        val result = DoubleArray(count)
        val skip = size - count
        var output = 0
        var logical = 0
        forEachIndex { index ->
            if (logical++ >= skip) result[output++] = values(metric)[index]
        }
        return result
    }

    private fun resize(newCapacity: Int) {
        val retained = minOf(size, newCapacity)
        val frames = newest(frameMs, retained)
        val renders = newest(renderMs, retained)
        val staticPlans = newest(staticPlanMs, retained)
        val dynamicPlans = newest(dynamicPlanMs, retained)
        frameMs = DoubleArray(newCapacity)
        renderMs = DoubleArray(newCapacity)
        staticPlanMs = DoubleArray(newCapacity)
        dynamicPlanMs = DoubleArray(newCapacity)
        frames.copyInto(frameMs)
        renders.copyInto(renderMs)
        staticPlans.copyInto(staticPlanMs)
        dynamicPlans.copyInto(dynamicPlanMs)
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

    private fun values(metric: DebugPerformanceMetric): DoubleArray = when (metric) {
        DebugPerformanceMetric.FRAME_TIME -> frameMs
        DebugPerformanceMetric.RENDER_TIME -> renderMs
        DebugPerformanceMetric.STATIC_PLAN_TIME -> staticPlanMs
        DebugPerformanceMetric.DYNAMIC_PLAN_TIME -> dynamicPlanMs
    }
}

