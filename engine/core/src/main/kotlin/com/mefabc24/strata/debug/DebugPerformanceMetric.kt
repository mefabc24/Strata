package com.mefabc24.strata.debug

import com.mefabc24.strata.render.RenderStats
import java.util.Locale
import kotlin.math.floor

/** How performance history values are measured, labelled, and formatted. */
enum class DebugPerformanceUnit(
    /** Suffix shown after values; empty for plain counts. */
    val symbol: String,
    /** Unit name shown in graph titles. */
    val displayName: String,
    /** Smallest graph upper bound, so idle or zero series keep a readable axis. */
    internal val minimumGraphRange: Double
) {
    MILLISECONDS("ms", "ms", 0.1),
    FRAMES_PER_SECOND("FPS", "FPS", 1.0),
    COUNT("", "count", 2.0);

    /** Formats one value of this unit, including its symbol when it has one. */
    fun format(value: Double): String {
        val number = when (this) {
            MILLISECONDS -> String.format(Locale.ROOT, "%.2f", value)
            FRAMES_PER_SECOND -> String.format(Locale.ROOT, "%.1f", value)
            // Counts are whole per frame; only averages need a fraction.
            COUNT -> String.format(Locale.ROOT, if (value == floor(value)) "%.0f" else "%.1f", value)
        }
        return if (symbol.isEmpty()) number else "$number $symbol"
    }
}

/** Scalar frame and renderer measurements that performance history records every captured frame. */
enum class DebugPerformanceMetric(
    /** Metric name without its unit. */
    val displayName: String,
    /** Unit of every recorded value. */
    val unit: DebugPerformanceUnit
) {
    FRAME_TIME("Frame time", DebugPerformanceUnit.MILLISECONDS),

    /** Instantaneous rate derived from each frame's time; zero for a zero-length frame. */
    FRAMES_PER_SECOND("Frame rate", DebugPerformanceUnit.FRAMES_PER_SECOND),
    RENDER_TIME("Render time", DebugPerformanceUnit.MILLISECONDS),
    STATIC_PLAN_TIME("Static plan time", DebugPerformanceUnit.MILLISECONDS),
    DYNAMIC_PLAN_TIME("Dynamic plan time", DebugPerformanceUnit.MILLISECONDS),
    DRAW_CALLS("Draw calls", DebugPerformanceUnit.COUNT),
    GROUND_DRAWN("Ground drawn", DebugPerformanceUnit.COUNT),
    GROUND_TOTAL("Ground total", DebugPerformanceUnit.COUNT),
    OVERLAYS_DRAWN("Overlays drawn", DebugPerformanceUnit.COUNT),
    OVERLAYS_TOTAL("Overlays total", DebugPerformanceUnit.COUNT),

    /** Ground and overlay terrain candidates visited by culling. */
    TERRAIN_CHECKED("Terrain checked", DebugPerformanceUnit.COUNT),
    OBJECTS_DRAWN("Objects drawn", DebugPerformanceUnit.COUNT),
    OBJECTS_CHECKED("Objects checked", DebugPerformanceUnit.COUNT),
    OBJECTS_TOTAL("Objects total", DebugPerformanceUnit.COUNT),
    ENTITIES_DRAWN("Entities drawn", DebugPerformanceUnit.COUNT),
    ENTITIES_CHECKED("Entities checked", DebugPerformanceUnit.COUNT),
    ENTITIES_TOTAL("Entities total", DebugPerformanceUnit.COUNT),
    PREVIEWS_DRAWN("Previews drawn", DebugPerformanceUnit.COUNT),
    STATIC_PLAN_UPDATES("Static plan updates", DebugPerformanceUnit.COUNT),
    STATIC_PLAN_CHECKS("Static plan checks", DebugPerformanceUnit.COUNT);

    /** Graph title including the unit, such as "Frame time (ms)". */
    val label: String
        get() = "$displayName (${unit.displayName})"

    /** Formats a value of this metric with its unit. */
    fun format(value: Double): String = unit.format(value)

    /** Reads this metric from the last completed world render and its frame time. */
    internal fun sample(stats: RenderStats, frameDeltaSeconds: Float): Double = when (this) {
        FRAME_TIME -> frameDeltaSeconds * 1_000.0
        FRAMES_PER_SECOND -> if (frameDeltaSeconds > 0f) 1.0 / frameDeltaSeconds else 0.0
        RENDER_TIME -> stats.cpuRenderMs
        STATIC_PLAN_TIME -> stats.staticPlanMs
        DYNAMIC_PLAN_TIME -> stats.dynamicPlanMs
        DRAW_CALLS -> stats.drawCalls.toDouble()
        GROUND_DRAWN -> stats.groundTerrainDrawn.toDouble()
        GROUND_TOTAL -> stats.groundTerrainTotal.toDouble()
        OVERLAYS_DRAWN -> stats.overlayTerrainDrawn.toDouble()
        OVERLAYS_TOTAL -> stats.overlayTerrainTotal.toDouble()
        TERRAIN_CHECKED -> stats.terrainChecked.toDouble()
        OBJECTS_DRAWN -> stats.objectsDrawn.toDouble()
        OBJECTS_CHECKED -> stats.objectsChecked.toDouble()
        OBJECTS_TOTAL -> stats.objectsTotal.toDouble()
        ENTITIES_DRAWN -> stats.entitiesDrawn.toDouble()
        ENTITIES_CHECKED -> stats.entitiesChecked.toDouble()
        ENTITIES_TOTAL -> stats.entitiesTotal.toDouble()
        PREVIEWS_DRAWN -> stats.previewsDrawn.toDouble()
        STATIC_PLAN_UPDATES -> stats.staticPlanUpdates.toDouble()
        STATIC_PLAN_CHECKS -> stats.staticPlanRelationChecks.toDouble()
    }
}
