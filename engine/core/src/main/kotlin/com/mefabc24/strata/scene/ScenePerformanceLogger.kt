package com.mefabc24.strata.scene

import com.badlogic.gdx.Gdx
import com.mefabc24.strata.render.RenderStats

/**
 * Logs rendering performance at a configurable interval.
 */
class ScenePerformanceLogger {

    var enabled: Boolean = false
        set(value) {
            if (field != value) {
                reset()
            }

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
    private var renderMsSum = 0.0

    /**
     * Records the statistics of a completed render.
     */
    internal fun record(
        stats: RenderStats,
        delta: Float
    ) {
        if (!enabled) return

        elapsed += delta
        frames++
        renderMsSum += stats.cpuRenderMs

        if (elapsed < intervalSeconds) return

        val averageRenderMs = renderMsSum / frames

        Gdx.app.log(
            "StrataPerf",
            "FPS: ${Gdx.graphics.framesPerSecond} | " +
                    "CPU render: ${"%.2f".format(averageRenderMs)} ms | " +
                    "Tiles: ${stats.terrainDrawn}/${stats.terrainChecked} | " +
                    "Objects: ${stats.objectsDrawn}/${stats.objectsChecked} | " +
                    "Entities: ${stats.entitiesDrawn}/${stats.entitiesChecked} | " +
                    "Preview: ${stats.previewsDrawn} | " +
                    "Draw calls: ${stats.drawCalls}"
        )

        reset()
    }

    private fun reset() {
        elapsed = 0f
        frames = 0
        renderMsSum = 0.0
    }
}
