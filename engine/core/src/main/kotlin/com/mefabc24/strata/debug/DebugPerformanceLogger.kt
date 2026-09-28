package com.mefabc24.strata.debug

import com.badlogic.gdx.Gdx
import com.mefabc24.strata.render.RenderStats

/** Logs renderer performance at a configurable interval when enabled. */
class DebugPerformanceLogger {
    var enabled: Boolean = false
        set(value) {
            if (field != value) reset()
            field = value
        }
    var intervalSeconds: Float = 2f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Log interval must be finite and positive."
            }
            field = value
        }

    private var elapsed = 0f
    private var frames = 0
    private var frameMsSum = 0.0
    private var frameMsMax = 0.0
    private val frameSamples = mutableListOf<Double>()
    private var renderMsSum = 0.0
    private var renderMsMax = 0.0
    private var staticPlanMsMax = 0.0
    private var staticPlanUpdates = 0
    private var staticPlanRelationChecks = 0
    private var dynamicPlanMsSum = 0.0
    private var dynamicPlanMsMax = 0.0

    internal fun record(stats: RenderStats, delta: Float) {
        if (!enabled) return
        val frameMs = delta * 1_000.0
        elapsed += delta
        frames++
        frameMsSum += frameMs
        frameMsMax = maxOf(frameMsMax, frameMs)
        frameSamples += frameMs
        renderMsSum += stats.cpuRenderMs
        renderMsMax = maxOf(renderMsMax, stats.cpuRenderMs)
        staticPlanMsMax = maxOf(staticPlanMsMax, stats.staticPlanMs)
        staticPlanUpdates += stats.staticPlanUpdates
        staticPlanRelationChecks += stats.staticPlanRelationChecks
        dynamicPlanMsSum += stats.dynamicPlanMs
        dynamicPlanMsMax = maxOf(dynamicPlanMsMax, stats.dynamicPlanMs)
        if (elapsed < intervalSeconds) return

        val sortedFrames = frameSamples.sorted()
        val p95 = sortedFrames[((sortedFrames.size - 1) * 0.95).toInt()]
        Gdx.app.log(
            "StrataPerf",
            "FPS: ${Gdx.graphics.framesPerSecond} | " +
                "Frame: ${format(frameMsSum / frames)} avg / ${format(p95)} p95 / " +
                "${format(frameMsMax)} max ms | World: ${format(renderMsSum / frames)} avg / " +
                "${format(renderMsMax)} max ms | Static plan: ${format(staticPlanMsMax)} max ms " +
                "($staticPlanUpdates updates, $staticPlanRelationChecks checks) | " +
                "Dynamic plan: ${format(dynamicPlanMsSum / frames)} avg / " +
                "${format(dynamicPlanMsMax)} max ms | Tiles: ${stats.terrainDrawn}/${stats.terrainChecked} | " +
                "Objects: ${stats.objectsDrawn}/${stats.objectsChecked} | " +
                "Entities: ${stats.entitiesDrawn}/${stats.entitiesChecked} | " +
                "Preview: ${stats.previewsDrawn} | Draw calls: ${stats.drawCalls}"
        )
        reset()
    }

    private fun format(value: Double) = "%.2f".format(value)

    private fun reset() {
        elapsed = 0f
        frames = 0
        frameMsSum = 0.0
        frameMsMax = 0.0
        frameSamples.clear()
        renderMsSum = 0.0
        renderMsMax = 0.0
        staticPlanMsMax = 0.0
        staticPlanUpdates = 0
        staticPlanRelationChecks = 0
        dynamicPlanMsSum = 0.0
        dynamicPlanMsMax = 0.0
    }
}
