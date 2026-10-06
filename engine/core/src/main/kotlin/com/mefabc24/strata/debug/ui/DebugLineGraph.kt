package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.mefabc24.strata.debug.DebugPerformanceMetric
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot

/** Lines are textured quads in the stage's existing batch; no renderer or batch restart. */
internal class DebugLineGraph(
    private val pixel: TextureRegion,
    private val lineColor: Color,
    private val labelStyle: Label.LabelStyle
) : Widget() {
    private var data: DebugPerformanceGraphData? = null
    private var metric = DebugPerformanceMetric.FRAME_TIME
    // x, y, length, angle per segment; regenerated only on refresh or resize.
    private var segments = FloatArray(0)
    private var segmentCount = 0
    private val savedFontColor = Color()
    private val emptyText = GlyphLayout(labelStyle.font, "No captured samples")
    private var upperLabel = "1"
    private var middleLabel = "0.5"
    val plotWidth: Float get() = maxOf(1f, width - LEFT - RIGHT)
    private val plotHeight: Float get() = maxOf(1f, height - BOTTOM - TOP)

    init { touchable = Touchable.disabled }

    fun show(data: DebugPerformanceGraphData, metric: DebugPerformanceMetric) {
        this.data = data
        this.metric = metric
        upperLabel = format(data.maximumY)
        middleLabel = format(data.maximumY / 2)
        invalidate()
    }

    override fun getPrefWidth() = 440f
    override fun getPrefHeight() = 180f

    override fun layout() {
        val graph = data ?: return
        segmentCount = maxOf(0, graph.pointCount - 1)
        if (segments.size < segmentCount * 4) segments = FloatArray(segmentCount * 4)
        for (point in 0 until segmentCount) {
            val x1 = pointX(graph, point)
            val y1 = pointY(graph, point)
            val dx = pointX(graph, point + 1) - x1
            val dy = pointY(graph, point + 1) - y1
            val offset = point * 4
            segments[offset] = x1
            segments[offset + 1] = y1
            segments[offset + 2] = hypot(dx, dy)
            segments[offset + 3] = atan2(dy, dx) * MathUtils.radiansToDegrees
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        validate()
        if (width <= LEFT + RIGHT || height <= TOP + BOTTOM) return
        val savedBatchColor = batch.packedColor
        val font = labelStyle.font
        savedFontColor.set(font.color)
        val alpha = parentAlpha * color.a
        try {
            batch.setColor(labelStyle.fontColor.r, labelStyle.fontColor.g, labelStyle.fontColor.b, alpha * 0.4f)
            rect(batch, LEFT, BOTTOM, plotWidth, 1f)
            rect(batch, LEFT, BOTTOM + plotHeight - 1f, plotWidth, 1f)
            rect(batch, LEFT, BOTTOM, 1f, plotHeight)
            rect(batch, LEFT + plotWidth - 1f, BOTTOM, 1f, plotHeight)
            batch.setColor(labelStyle.fontColor.r, labelStyle.fontColor.g, labelStyle.fontColor.b, alpha * 0.14f)
            for (division in 1..3) rect(batch, LEFT, BOTTOM + plotHeight * division / 4, plotWidth, 1f)
            font.setColor(labelStyle.fontColor.r, labelStyle.fontColor.g, labelStyle.fontColor.b, alpha)
            font.draw(batch, upperLabel, x + 2, y + BOTTOM + plotHeight)
            font.draw(batch, middleLabel, x + 2, y + BOTTOM + plotHeight / 2 + font.capHeight / 2)
            font.draw(batch, "0 ms", x + 2, y + BOTTOM + font.capHeight)

            // Flush on either side of the scissor, preserving the Scene2D batch lifecycle.
            batch.flush()
            if (clipBegin(x + LEFT, y + BOTTOM, plotWidth, plotHeight)) {
                try {
                    val graph = data
                    if (graph == null || graph.pointCount == 0) {
                        font.draw(batch, emptyText, x + LEFT + (plotWidth - emptyText.width) / 2,
                            y + BOTTOM + (plotHeight + emptyText.height) / 2)
                    } else {
                        if (metric == DebugPerformanceMetric.FRAME_TIME) {
                            reference(batch, graph, 1000.0 / 60, "60 FPS", alpha)
                            reference(batch, graph, 1000.0 / 30, "30 FPS", alpha)
                        }
                        batch.setColor(lineColor.r, lineColor.g, lineColor.b, alpha)
                        if (graph.pointCount == 1) {
                            rect(batch, pointX(graph, 0) - 2, pointY(graph, 0) - 2, 4f, 4f)
                        } else {
                            for (segment in 0 until segmentCount) {
                                val offset = segment * 4
                                batch.draw(pixel, x + segments[offset], y + segments[offset + 1] - 0.75f,
                                    0f, 0.75f, segments[offset + 2], 1.5f, 1f, 1f, segments[offset + 3])
                            }
                        }
                    }
                } finally {
                    batch.flush()
                    clipEnd()
                }
            }
        } finally {
            batch.packedColor = savedBatchColor
            font.color = savedFontColor
        }
    }

    private fun reference(batch: Batch, graph: DebugPerformanceGraphData, value: Double, text: String, alpha: Float) {
        if (value >= graph.maximumY) return
        val lineY = BOTTOM + (value / graph.maximumY).toFloat() * plotHeight
        batch.setColor(lineColor.r, lineColor.g, lineColor.b, alpha * 0.25f)
        rect(batch, LEFT, lineY, plotWidth, 1f)
        labelStyle.font.setColor(lineColor.r, lineColor.g, lineColor.b, alpha * 0.65f)
        labelStyle.font.draw(batch, text, x + LEFT + plotWidth - 52f, y + lineY + labelStyle.font.capHeight + 3f)
    }

    private fun pointX(graph: DebugPerformanceGraphData, point: Int): Float = LEFT +
        if (graph.sampleCount <= 1) plotWidth / 2 else
            graph.sampleIndex(point).toFloat() / (graph.sampleCount - 1) * plotWidth

    private fun pointY(graph: DebugPerformanceGraphData, point: Int): Float =
        BOTTOM + (graph.value(point) / graph.maximumY).toFloat().coerceIn(0f, 1f) * plotHeight

    private fun rect(batch: Batch, left: Float, bottom: Float, w: Float, h: Float) =
        batch.draw(pixel, x + left, y + bottom, w, h)

    private fun format(value: Double): String = String.format(Locale.ROOT, if (value >= 10) "%.0f" else "%.1f", value)

    private companion object {
        const val LEFT = 52f
        const val RIGHT = 8f
        const val BOTTOM = 8f
        const val TOP = 8f
    }
}
