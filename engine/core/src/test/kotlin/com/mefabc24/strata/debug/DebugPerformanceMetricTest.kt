package com.mefabc24.strata.debug

import com.mefabc24.strata.render.RenderStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugPerformanceMetricTest {
    @Test
    fun `metrics describe their unit in names labels and formatted values`() {
        assertEquals("Frame time (ms)", DebugPerformanceMetric.FRAME_TIME.label)
        assertEquals("16.67 ms", DebugPerformanceMetric.FRAME_TIME.format(16.666))
        assertEquals("Frame rate (FPS)", DebugPerformanceMetric.FRAMES_PER_SECOND.label)
        assertEquals("59.9 FPS", DebugPerformanceMetric.FRAMES_PER_SECOND.format(59.94))
        assertEquals("Entities drawn (count)", DebugPerformanceMetric.ENTITIES_DRAWN.label)
        assertEquals("42", DebugPerformanceMetric.ENTITIES_DRAWN.format(42.0))
        assertEquals("12.5", DebugPerformanceMetric.ENTITIES_DRAWN.format(12.5))
        assertEquals("65536", DebugPerformanceMetric.GROUND_TOTAL.format(65_536.0))
    }

    @Test
    fun `count metrics never present time units`() {
        val counts = DebugPerformanceMetric.entries.filter { it.unit == DebugPerformanceUnit.COUNT }
        assertTrue(counts.isNotEmpty())
        for (metric in counts) {
            assertFalse(metric.format(3.0).contains("ms"), metric.name)
            assertFalse(metric.label.contains("ms"), metric.name)
        }
        val timings = DebugPerformanceMetric.entries.filter { it.unit == DebugPerformanceUnit.MILLISECONDS }
        assertEquals(
            setOf(
                DebugPerformanceMetric.FRAME_TIME,
                DebugPerformanceMetric.RENDER_TIME,
                DebugPerformanceMetric.STATIC_PLAN_TIME,
                DebugPerformanceMetric.DYNAMIC_PLAN_TIME
            ),
            timings.toSet()
        )
    }

    @Test
    fun `display names are unique and reasonably short for the metric selector`() {
        val names = DebugPerformanceMetric.entries.map { it.displayName }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { it.isNotBlank() && it.length <= 20 }, names.toString())
    }

    @Test
    fun `every metric reads its existing renderer statistic`() {
        val stats = RenderStats().apply {
            cpuRenderMs = 3.5
            staticPlanMs = 1.25
            dynamicPlanMs = 0.75
            drawCalls = 7
            groundTerrainDrawn = 100
            groundTerrainTotal = 400
            overlayTerrainDrawn = 20
            overlayTerrainTotal = 80
            terrainChecked = 150
            objectsDrawn = 12
            objectsChecked = 30
            objectsTotal = 90
            entitiesDrawn = 4
            entitiesChecked = 9
            entitiesTotal = 16
            previewsDrawn = 2
            staticPlanUpdates = 3
            staticPlanRelationChecks = 55
        }
        val expected = mapOf(
            DebugPerformanceMetric.FRAME_TIME to 20.0,
            DebugPerformanceMetric.FRAMES_PER_SECOND to 50.0,
            DebugPerformanceMetric.RENDER_TIME to 3.5,
            DebugPerformanceMetric.STATIC_PLAN_TIME to 1.25,
            DebugPerformanceMetric.DYNAMIC_PLAN_TIME to 0.75,
            DebugPerformanceMetric.DRAW_CALLS to 7.0,
            DebugPerformanceMetric.GROUND_DRAWN to 100.0,
            DebugPerformanceMetric.GROUND_TOTAL to 400.0,
            DebugPerformanceMetric.OVERLAYS_DRAWN to 20.0,
            DebugPerformanceMetric.OVERLAYS_TOTAL to 80.0,
            DebugPerformanceMetric.TERRAIN_CHECKED to 150.0,
            DebugPerformanceMetric.OBJECTS_DRAWN to 12.0,
            DebugPerformanceMetric.OBJECTS_CHECKED to 30.0,
            DebugPerformanceMetric.OBJECTS_TOTAL to 90.0,
            DebugPerformanceMetric.ENTITIES_DRAWN to 4.0,
            DebugPerformanceMetric.ENTITIES_CHECKED to 9.0,
            DebugPerformanceMetric.ENTITIES_TOTAL to 16.0,
            DebugPerformanceMetric.PREVIEWS_DRAWN to 2.0,
            DebugPerformanceMetric.STATIC_PLAN_UPDATES to 3.0,
            DebugPerformanceMetric.STATIC_PLAN_CHECKS to 55.0
        )
        assertEquals(DebugPerformanceMetric.entries.toSet(), expected.keys)
        for ((metric, value) in expected) {
            assertEquals(value, metric.sample(stats, 0.02f), 0.0001, metric.name)
        }
    }

    @Test
    fun `frame rate is the instantaneous inverse of frame time`() {
        val stats = RenderStats()
        assertEquals(60.0, DebugPerformanceMetric.FRAMES_PER_SECOND.sample(stats, 1f / 60f), 0.001)
        assertEquals(0.0, DebugPerformanceMetric.FRAMES_PER_SECOND.sample(stats, 0f))
    }

    @Test
    fun `legacy metric names remain stable for saved configurations`() {
        for (name in listOf("FRAME_TIME", "RENDER_TIME", "STATIC_PLAN_TIME", "DYNAMIC_PLAN_TIME")) {
            assertEquals(name, DebugPerformanceMetric.valueOf(name).name)
        }
    }
}
