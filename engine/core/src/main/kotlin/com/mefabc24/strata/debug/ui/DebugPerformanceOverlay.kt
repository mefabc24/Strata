package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi
import java.util.Locale

internal class DebugPerformanceOverlay(
    ui: StrataUi,
    private val stats: () -> RenderStats,
    private val enabled: () -> Boolean,
    private val framesPerSecond: () -> Int = { Gdx.graphics.framesPerSecond }
) {
    private val state = DebugPerformanceOverlayState()
    private val label = Label("", ui.skin, ui.theme.labelStyle).apply {
        setAlignment(Align.left)
        touchable = Touchable.disabled
    }
    private val root = Table().apply {
        setFillParent(true)
        top().right()
        pad(16f)
        touchable = Touchable.disabled
        isVisible = false
        add(Table(ui.skin).apply {
            background = ui.skin.get(
                requireNotNull(ui.theme.panelStyle),
                StrataPanelStyle::class.java
            ).background
            pad(8f)
            touchable = Touchable.disabled
            add(label)
        })
    }

    init { ui.stage.addActor(root) }

    fun update(delta: Float) {
        val average = state.update(enabled(), delta)
        root.isVisible = state.visible
        if (average != null) {
            label.setText(
                DebugPerformanceSnapshot.from(stats(), framesPerSecond(), average).format()
            )
        }
    }
}

class DebugPerformanceOverlayState(private val refreshInterval: Float = 0.25f) {
    var visible: Boolean = false
        private set
    private var elapsed = 0f
    private var accumulatedFrameMs = 0.0
    private var sampledFrames = 0

    init {
        require(refreshInterval.isFinite() && refreshInterval > 0f) {
            "Refresh interval must be finite and greater than zero."
        }
    }

    fun update(enabled: Boolean, delta: Float): Double? {
        if (!enabled) {
            visible = false
            reset()
            return null
        }
        require(delta.isFinite() && delta >= 0f) {
            "Frame delta must be finite and non-negative."
        }
        val becameVisible = !visible
        visible = true
        elapsed += delta
        accumulatedFrameMs += delta * 1000.0
        sampledFrames++
        if (!becameVisible && elapsed < refreshInterval) return null
        return (accumulatedFrameMs / sampledFrames).also { reset() }
    }

    private fun reset() {
        elapsed = 0f
        accumulatedFrameMs = 0.0
        sampledFrames = 0
    }
}

data class DebugPerformanceSnapshot(
    val framesPerSecond: Int,
    val averageFrameMs: Double,
    val renderMs: Double,
    val dynamicPlanMs: Double,
    val drawCalls: Int,
    val groundTerrainDrawn: Int,
    val groundTerrainTotal: Int,
    val overlayTerrainDrawn: Int,
    val overlayTerrainTotal: Int,
    val terrainChecked: Int,
    val objectsDrawn: Int,
    val objectsTotal: Int,
    val entitiesDrawn: Int,
    val entitiesTotal: Int,
    val previewsDrawn: Int,
    val staticPlanMs: Double,
    val staticPlanUpdates: Int
) {
    fun format(): String = buildString {
        append("FPS: $framesPerSecond")
        append("\nFrame: ${ms(averageFrameMs)} ms")
        append("\nRender: ${ms(renderMs)} ms")
        append("\nPlan: ${ms(dynamicPlanMs)} ms")
        if (staticPlanUpdates > 0) {
            append("\nStatic plan: ${ms(staticPlanMs)} ms")
            append("\nStatic updates: $staticPlanUpdates")
        }
        append("\nDraw calls: $drawCalls")
        append("\n\nGround: $groundTerrainDrawn/$groundTerrainTotal")
        append("\nOverlays: $overlayTerrainDrawn/$overlayTerrainTotal")
        append("\nChecks: $terrainChecked")
        append("\nObjects: $objectsDrawn/$objectsTotal")
        append("\nEntities: $entitiesDrawn/$entitiesTotal")
        append("\nPreviews: $previewsDrawn")
    }

    companion object {
        fun from(stats: RenderStats, framesPerSecond: Int, averageFrameMs: Double) =
            DebugPerformanceSnapshot(
                framesPerSecond, averageFrameMs, stats.cpuRenderMs,
                stats.dynamicPlanMs, stats.drawCalls, stats.groundTerrainDrawn,
                stats.groundTerrainTotal, stats.overlayTerrainDrawn,
                stats.overlayTerrainTotal, stats.terrainChecked, stats.objectsDrawn,
                stats.objectsTotal, stats.entitiesDrawn, stats.entitiesTotal,
                stats.previewsDrawn, stats.staticPlanMs, stats.staticPlanUpdates
            )

        private fun ms(value: Double) = String.format(Locale.ROOT, "%.2f", value)
    }
}
