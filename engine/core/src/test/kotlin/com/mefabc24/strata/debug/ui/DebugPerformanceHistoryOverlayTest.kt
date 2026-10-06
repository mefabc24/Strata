package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.mefabc24.strata.debug.DebugPerformanceMetric
import com.mefabc24.strata.debug.DebugPerformanceSettings
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.ui.StrataUi
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugPerformanceHistoryOverlayTest {
    @Test
    fun `visibility defaults off and does not start or stop recording`() {
        val settings = DebugPerformanceSettings()
        val state = DebugPerformanceHistoryOverlayState(settings)
        assertFalse(settings.historyOverlayEnabled)
        assertFalse(state.update(0f, 100))
        settings.historyOverlayEnabled = true
        assertTrue(state.update(0f, 100))
        assertTrue(state.visible)
        assertFalse(settings.historyRecording)
        settings.startHistoryRecording()
        settings.historyOverlayEnabled = false
        assertFalse(state.update(0f, 100))
        assertFalse(state.visible)
        assertTrue(settings.historyRecording)
    }

    @Test
    fun `hidden recording accumulates and stopped recording retains history`() {
        val settings = DebugPerformanceSettings().apply { historyRecording = true }
        val state = DebugPerformanceHistoryOverlayState(settings)
        settings.record(stats(1.0), 0.016f)
        state.update(1f, 100)
        settings.record(stats(9.0), 0.017f)
        settings.historyMetric = DebugPerformanceMetric.RENDER_TIME
        settings.historyOverlayEnabled = true
        assertTrue(state.update(0f, 100))
        assertEquals(2, state.graph.sampleCount)
        assertEquals(9.0, state.graph.currentMs)
        settings.historyOverlayEnabled = false
        state.update(1f, 100)
        settings.record(stats(4.0), 0.018f)
        settings.stopHistoryRecording()
        settings.record(stats(100.0), 0.1f)
        settings.historyOverlayEnabled = true
        assertTrue(state.update(0f, 100))
        assertEquals(3, state.graph.sampleCount)
        assertEquals(4.0, state.graph.currentMs)
        assertFalse(state.recording)
    }

    @Test
    fun `clear metric resize and recording changes refresh immediately`() {
        val settings = DebugPerformanceSettings().apply {
            historyOverlayEnabled = true
            historyRecording = true
            overlayRefreshIntervalSeconds = 5f
        }
        val state = DebugPerformanceHistoryOverlayState(settings)
        settings.record(stats(3.0), 0.02f)
        assertTrue(state.update(0f, 100))
        assertEquals(20.0, state.graph.currentMs!!, 0.001)
        for ((metric, value) in listOf(
            DebugPerformanceMetric.RENDER_TIME to 3.0,
            DebugPerformanceMetric.STATIC_PLAN_TIME to 1.5,
            DebugPerformanceMetric.DYNAMIC_PLAN_TIME to 0.75
        )) {
            settings.historyMetric = metric
            assertTrue(state.update(0f, 100))
            assertEquals(value, state.graph.currentMs)
        }
        settings.historyMetric = DebugPerformanceMetric.RENDER_TIME
        settings.record(stats(6.0), 0.02f)
        settings.record(stats(9.0), 0.02f)
        settings.historyLength = 2
        assertTrue(state.update(0f, 100))
        assertEquals(6.0, state.graph.value(0))
        assertEquals(9.0, state.graph.value(1))
        settings.historyLength = 1000
        assertTrue(state.update(0f, 100))
        assertEquals(2, state.graph.sampleCount)
        settings.stopHistoryRecording()
        assertTrue(state.update(0f, 100))
        assertFalse(state.recording)
        settings.clearHistory()
        assertTrue(state.update(0f, 100))
        assertEquals(0, state.graph.pointCount)
        assertNull(state.graph.summary)
    }

    @Test
    fun `capture runs every frame while graph refresh follows the shared interval`() {
        val settings = DebugPerformanceSettings().apply {
            historyOverlayEnabled = true
            historyRecording = true
            overlayRefreshIntervalSeconds = 1f
        }
        val state = DebugPerformanceHistoryOverlayState(settings)
        assertTrue(state.update(0f, 100))
        repeat(5) {
            settings.record(stats(it.toDouble()), 0.1f)
            assertFalse(state.update(0.1f, 100))
        }
        assertEquals(5, settings.history.size)
        assertEquals(0, state.graph.sampleCount)
        settings.overlayRefreshIntervalSeconds = 0.25f
        assertTrue(state.update(0f, 100))
        assertEquals(5, state.graph.sampleCount)
        assertTrue(state.update(0f, 80))
    }

    @Test
    fun `panel text renders at native font scale on whole pixel bounds`() {
        TestGdxEnvironment.install()
        val skin = DebugPanelSkin.create()
        val ui = StrataUi(skin, DebugPanelSkin.theme())
        val settings = DebugPerformanceSettings().apply {
            historyOverlayEnabled = true
            historyRecording = true
        }
        try {
            val overlay = DebugPerformanceHistoryOverlay(ui, settings)
            settings.record(stats(2.0), 0.016f)
            overlay.position(1280f, 720f, left = 86.4f, rightEdge = 903.7f, bottom = 70.5f)
            overlay.update(0f)
            val panel = ui.stage.root.findActor<Table>("debug-performance-history")
            for (value in listOf(panel.x, panel.y, panel.width, panel.height)) {
                assertEquals(floor(value), value, "Panel bounds must stay on whole pixels")
            }
            assertTrue(panel.x >= 86.4f && panel.y >= 70.5f)
            assertTrue(panel.x + panel.width <= 903.7f - DebugWindowLayout.OVERLAY_GAP)
            val labels = descendants(panel).filterIsInstance<Label>()
            assertTrue(labels.any { it.text.startsWith("Avg") })
            labels.forEach { label ->
                assertEquals(1f, label.fontScaleX, "${label.text} must not resample the bitmap font")
                assertEquals(1f, label.fontScaleY, "${label.text} must not resample the bitmap font")
            }
        } finally {
            ui.dispose()
            skin.dispose()
        }
    }

    private fun descendants(actor: Actor): List<Actor> = listOf(actor) +
        if (actor is Group) actor.children.flatMap(::descendants) else emptyList()

    private fun stats(value: Double) = RenderStats().apply {
        cpuRenderMs = value
        staticPlanMs = value / 2
        dynamicPlanMs = value / 4
    }
}
