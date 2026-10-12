package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.mefabc24.strata.debug.DebugPerformanceHistoryGraphs
import com.mefabc24.strata.debug.DebugPerformanceMetric
import com.mefabc24.strata.debug.DebugPerformanceSettings
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.ui.StrataButton
import com.mefabc24.strata.ui.StrataDropdown
import com.mefabc24.strata.ui.StrataNumericStepper
import com.mefabc24.strata.ui.StrataPanel
import com.mefabc24.strata.ui.StrataScrollPane
import com.mefabc24.strata.ui.StrataUi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugPerformanceHistoryPanelTest {
    private lateinit var ui: StrataUi
    private lateinit var settings: DebugPerformanceSettings
    private lateinit var overlay: DebugPerformanceHistoryOverlay
    private val skin by lazy { DebugPanelSkin.create() }

    @BeforeTest
    fun setUp() {
        TestGdxEnvironment.install()
        ui = StrataUi(skin, DebugPanelSkin.theme())
        settings = DebugPerformanceSettings().apply { historyOverlayEnabled = true }
        overlay = DebugPerformanceHistoryOverlay(ui, settings)
        refresh()
    }

    @AfterTest
    fun tearDown() {
        ui.dispose()
        skin.dispose()
    }

    @Test
    fun `panel owns start stop and clear for the shared recording`() {
        assertTrue(panel().isVisible)
        assertFalse(settings.historyRecording)
        assertEquals("Stopped", recordingText())
        assertFalse(button("Start").isDisabled)
        assertTrue(button("Stop").isDisabled)
        assertTrue(button("Clear").isDisabled)

        button("Start").onClick()
        settings.record(RenderStats(), 0.016f)
        settings.record(RenderStats(), 0.016f)
        refresh()

        assertTrue(settings.historyRecording)
        assertEquals("Recording", recordingText())
        assertTrue(button("Start").isDisabled)
        assertFalse(button("Stop").isDisabled)
        assertFalse(button("Clear").isDisabled)
        assertTrue(labels().contains("Samples 2 / 240"))

        button("Stop").onClick()
        settings.record(RenderStats(), 0.016f)
        refresh()
        assertFalse(settings.historyRecording)
        assertEquals("Stopped", recordingText())
        assertEquals(2, settings.history.size, "Stopping retains captured samples")

        button("Clear").onClick()
        refresh()
        assertEquals(0, settings.history.size)
        assertFalse(settings.historyRecording, "Clearing does not start recording")
        assertTrue(labels().contains("Samples 0 / 240"))
        assertTrue(button("Clear").isDisabled)
    }

    @Test
    fun `each graph has its own metric selector and only additional graphs are removable`() {
        assertEquals(1, dropdowns().size)
        assertTrue(buttons("Remove").single().isDisabled)

        button("Add graph").onClick()
        refresh()

        val selectors = dropdowns()
        assertEquals(2, selectors.size)
        assertEquals(
            listOf(DebugPerformanceMetric.FRAME_TIME, DebugPerformanceMetric.FRAMES_PER_SECOND),
            selectors.map { it.value }
        )
        assertTrue(buttons("Remove").none { it.isDisabled })
        assertTrue(labels().contains("Frame rate (FPS)"))

        selectors[1].value = DebugPerformanceMetric.ENTITIES_TOTAL
        refresh()
        assertEquals(
            listOf(DebugPerformanceMetric.FRAME_TIME, DebugPerformanceMetric.ENTITIES_TOTAL),
            settings.historyGraphMetrics
        )
        assertTrue(labels().contains("Entities total (count)"))

        buttons("Remove").first().onClick()
        refresh()
        assertEquals(listOf(DebugPerformanceMetric.ENTITIES_TOTAL), settings.historyGraphMetrics)
        assertEquals(DebugPerformanceMetric.ENTITIES_TOTAL, dropdowns().single().value)
        assertTrue(buttons("Remove").single().isDisabled)
    }

    @Test
    fun `graph count stays bounded and graphs share one recording`() {
        settings.startHistoryRecording()
        settings.record(RenderStats().apply { drawCalls = 9 }, 0.02f)
        repeat(DebugPerformanceHistoryGraphs.MAXIMUM_GRAPHS - 1) {
            button("Add graph").onClick()
        }
        refresh()

        assertEquals(DebugPerformanceHistoryGraphs.MAXIMUM_GRAPHS, dropdowns().size)
        assertTrue(button("Add graph").isDisabled)
        button("Add graph").onClick()
        assertEquals(DebugPerformanceHistoryGraphs.MAXIMUM_GRAPHS, settings.historyGraphMetrics.size)
        val values = labels()
        assertTrue(values.contains("20.00 ms"), values.toString())
        assertTrue(values.contains("50.0 FPS"), values.toString())
        assertFalse(values.any { it.endsWith(" ms") && it.startsWith("9") }, "Counts never use time units")
    }

    @Test
    fun `history length control keeps sample storage bounded`() {
        val stepper = descendants(panel()).filterIsInstance<StrataNumericStepper>().single()
        assertEquals(240f, stepper.value)
        stepper.value = 2f
        assertEquals(2, settings.historyLength)
        settings.startHistoryRecording()
        repeat(5) { settings.record(RenderStats(), 0.016f) }
        refresh()
        assertEquals(2, settings.history.size)
        assertTrue(labels().contains("Samples 2 / 2"))

        settings.historyLength = 1_000
        refresh()
        assertEquals(1_000f, stepper.value)
    }

    @Test
    fun `tall graph lists scroll inside an input blocking panel`() {
        repeat(DebugPerformanceHistoryGraphs.MAXIMUM_GRAPHS - 1) { button("Add graph").onClick() }
        overlay.update(0f)
        overlay.position(800f, 400f, left = 16f, rightEdge = 760f, bottom = 16f)

        val panel = panel()
        assertTrue(panel.blocksInput)
        assertEquals(Touchable.enabled, panel.touchable)
        assertEquals(400f - 16f - 16f, panel.height)
        val scroll = descendants(panel).filterIsInstance<StrataScrollPane>().single()
        assertTrue(scroll.height < scroll.content.prefHeight, "Graphs beyond the panel height scroll")
        assertTrue(scroll.y >= 0f && scroll.y + scroll.height <= panel.height)
    }

    @Test
    fun `hiding the panel keeps recording and graph configuration`() {
        button("Start").onClick()
        button("Add graph").onClick()
        settings.historyOverlayEnabled = false
        refresh()
        assertFalse(panel().isVisible)
        assertTrue(settings.historyRecording)
        assertEquals(2, settings.historyGraphMetrics.size)
        settings.historyOverlayEnabled = true
        refresh()
        assertTrue(panel().isVisible)
        assertEquals(2, dropdowns().size)
    }

    private fun refresh() {
        overlay.update(0f)
        overlay.position(1280f, 900f, left = 16f, rightEdge = 900f, bottom = 16f)
    }

    private fun panel(): StrataPanel = ui.stage.root.findActor("debug-performance-history")

    private fun recordingText(): String = labels().single { it == "Recording" || it == "Stopped" }

    private fun labels(): List<String> = descendants(panel()).filterIsInstance<Label>().map { it.text.toString() }

    @Suppress("UNCHECKED_CAST")
    private fun dropdowns(): List<StrataDropdown<DebugPerformanceMetric>> =
        descendants(panel()).filterIsInstance<StrataDropdown<*>>() as List<StrataDropdown<DebugPerformanceMetric>>

    private fun buttons(text: String): List<StrataButton> =
        descendants(panel()).filterIsInstance<StrataButton>().filter { it.text.toString() == text }

    private fun button(text: String): StrataButton = buttons(text).single()

    private fun descendants(actor: Actor): List<Actor> = listOf(actor) +
        if (actor is Group) actor.children.flatMap(::descendants) else emptyList()
}
