package com.mefabc24.strata.debug

/** One Performance History graph; it keeps its identity when its metric or other graphs change. */
internal class DebugPerformanceHistoryGraph(metric: DebugPerformanceMetric) {
    var metric: DebugPerformanceMetric = metric
}

/** Ordered graphs of the Performance History panel; every graph reads the same shared recording. */
internal class DebugPerformanceHistoryGraphs {
    private val entries = mutableListOf(DebugPerformanceHistoryGraph(DebugPerformanceMetric.FRAME_TIME))

    val graphs: List<DebugPerformanceHistoryGraph>
        get() = entries

    /** Incremented whenever graphs are added, removed, or replaced together. */
    var revision: Long = 0
        private set

    val canAdd: Boolean
        get() = entries.size < MAXIMUM_GRAPHS

    /** The last graph stays, so the panel always shows at least one series. */
    val canRemove: Boolean
        get() = entries.size > 1

    /** Graph metrics from top to bottom; assigning reuses existing graphs in order. */
    var metrics: List<DebugPerformanceMetric>
        get() = entries.map { it.metric }
        set(value) {
            require(value.size in 1..MAXIMUM_GRAPHS) {
                "Performance history must show between 1 and $MAXIMUM_GRAPHS graphs."
            }
            value.forEachIndexed { index, metric ->
                if (index < entries.size) entries[index].metric = metric
                else entries += DebugPerformanceHistoryGraph(metric)
            }
            while (entries.size > value.size) entries.removeAt(entries.lastIndex)
            revision++
        }

    /** Adds a graph below the others, defaulting to the first metric that is not graphed yet. */
    fun add(metric: DebugPerformanceMetric = nextMetric()): DebugPerformanceHistoryGraph {
        check(canAdd) { "Performance history already shows $MAXIMUM_GRAPHS graphs." }
        return DebugPerformanceHistoryGraph(metric).also {
            entries += it
            revision++
        }
    }

    fun remove(graph: DebugPerformanceHistoryGraph) {
        check(canRemove) { "Performance history must keep at least one graph." }
        require(entries.remove(graph)) { "The graph is not part of this performance history." }
        revision++
    }

    private fun nextMetric(): DebugPerformanceMetric = DebugPerformanceMetric.entries
        .firstOrNull { metric -> entries.none { it.metric == metric } }
        ?: DebugPerformanceMetric.FRAME_TIME

    companion object {
        const val MAXIMUM_GRAPHS = 6
    }
}
