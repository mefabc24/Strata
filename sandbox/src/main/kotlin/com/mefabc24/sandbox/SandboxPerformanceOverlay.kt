package com.mefabc24.sandbox

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi
import java.util.Locale

internal class SandboxPerformanceOverlay(
    ui: StrataUi,
    private val stats: () -> RenderStats,
    private val enabled: () -> Boolean,
    private val framesPerSecond: () -> Int = {
        Gdx.graphics.framesPerSecond
    }
) {

    private val state = SandboxPerformanceOverlayState()

    private val label = Label(
        "",
        ui.skin,
        ui.theme.labelStyle
    ).apply {
        setAlignment(Align.left)
        touchable = Touchable.disabled
    }

    private val panel = Table(ui.skin).apply {
        val panelStyleName = requireNotNull(ui.theme.panelStyle) {
            "The Sandbox performance overlay requires a panel style."
        }
        background = ui.skin.get(
            panelStyleName,
            StrataPanelStyle::class.java
        ).background
        pad(CONTENT_PADDING)
        touchable = Touchable.disabled
        add(label)
    }

    private val root = Table().apply {
        setFillParent(true)
        top().right()
        pad(ROOT_MARGIN)
        touchable = Touchable.disabled
        isVisible = false
        add(panel)
    }

    init {
        ui.stage.addActor(root)
    }

    fun update(delta: Float) {
        val averageFrameMs = state.update(
            enabled = enabled(),
            delta = delta
        )
        root.isVisible = state.visible

        if (averageFrameMs == null) {
            return
        }

        label.setText(
            SandboxPerformanceSnapshot.from(
                stats = stats(),
                framesPerSecond = framesPerSecond(),
                averageFrameMs = averageFrameMs
            ).format()
        )
    }

    private companion object {
        const val ROOT_MARGIN = 16f
        const val CONTENT_PADDING = 8f
    }
}

internal class SandboxPerformanceOverlayState(
    private val refreshInterval: Float = 0.25f
) {

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

    fun update(
        enabled: Boolean,
        delta: Float
    ): Double? {
        if (!enabled) {
            visible = false
            resetSamples()
            return null
        }

        require(delta.isFinite() && delta >= 0f) {
            "Frame delta must be finite and non-negative."
        }

        val becameVisible = !visible
        visible = true
        elapsed += delta
        accumulatedFrameMs += delta * MILLIS_PER_SECOND
        sampledFrames++

        if (!becameVisible && elapsed < refreshInterval) {
            return null
        }

        val averageFrameMs = accumulatedFrameMs / sampledFrames
        resetSamples()
        return averageFrameMs
    }

    private fun resetSamples() {
        elapsed = 0f
        accumulatedFrameMs = 0.0
        sampledFrames = 0
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000.0
    }
}

internal data class SandboxPerformanceSnapshot(
    val framesPerSecond: Int,
    val averageFrameMs: Double,
    val renderMs: Double,
    val dynamicPlanMs: Double,
    val drawCalls: Int,
    val terrainDrawn: Int,
    val terrainChecked: Int,
    val objectsDrawn: Int,
    val objectsChecked: Int,
    val entitiesDrawn: Int,
    val entitiesChecked: Int,
    val previewsDrawn: Int,
    val staticPlanMs: Double,
    val staticPlanUpdates: Int
) {

    fun format(): String = buildString {
        append("FPS: ")
        append(framesPerSecond)
        append("\nFrame: ")
        append(formatMilliseconds(averageFrameMs))
        append(" ms")
        append("\nRender: ")
        append(formatMilliseconds(renderMs))
        append(" ms")
        append("\nPlan: ")
        append(formatMilliseconds(dynamicPlanMs))
        append(" ms")

        if (staticPlanUpdates > 0) {
            append("\nStatic plan: ")
            append(formatMilliseconds(staticPlanMs))
            append(" ms")
            append("\nStatic updates: ")
            append(staticPlanUpdates)
        }

        append("\nDraw calls: ")
        append(drawCalls)
        append("\n\nTerrain: ")
        append(terrainDrawn)
        append('/')
        append(terrainChecked)
        append("\nObjects: ")
        append(objectsDrawn)
        append('/')
        append(objectsChecked)
        append("\nEntities: ")
        append(entitiesDrawn)
        append('/')
        append(entitiesChecked)
        append("\nPreviews: ")
        append(previewsDrawn)
    }

    companion object {

        fun from(
            stats: RenderStats,
            framesPerSecond: Int,
            averageFrameMs: Double
        ) = SandboxPerformanceSnapshot(
            framesPerSecond = framesPerSecond,
            averageFrameMs = averageFrameMs,
            renderMs = stats.cpuRenderMs,
            dynamicPlanMs = stats.dynamicPlanMs,
            drawCalls = stats.drawCalls,
            terrainDrawn = stats.terrainDrawn,
            terrainChecked = stats.terrainChecked,
            objectsDrawn = stats.objectsDrawn,
            objectsChecked = stats.objectsChecked,
            entitiesDrawn = stats.entitiesDrawn,
            entitiesChecked = stats.entitiesChecked,
            previewsDrawn = stats.previewsDrawn,
            staticPlanMs = stats.staticPlanMs,
            staticPlanUpdates = stats.staticPlanUpdates
        )

        private fun formatMilliseconds(value: Double): String {
            return String.format(Locale.ROOT, "%.2f", value)
        }
    }
}
