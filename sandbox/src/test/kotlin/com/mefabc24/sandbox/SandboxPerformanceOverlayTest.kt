package com.mefabc24.sandbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxPerformanceOverlayTest {

    @Test
    fun `visibility follows enabled state and disabled state resets samples`() {
        val state = SandboxPerformanceOverlayState()

        assertNull(state.update(enabled = false, delta = 0.1f))
        assertFalse(state.visible)

        assertEquals(
            16.0,
            state.update(enabled = true, delta = 0.016f)!!,
            absoluteTolerance = 0.001
        )
        assertTrue(state.visible)

        assertNull(state.update(enabled = true, delta = 0.1f))
        assertNull(state.update(enabled = false, delta = 0.1f))
        assertFalse(state.visible)

        assertEquals(
            20.0,
            state.update(enabled = true, delta = 0.02f)!!,
            absoluteTolerance = 0.001
        )
    }

    @Test
    fun `display refresh averages frames across the interval`() {
        val state = SandboxPerformanceOverlayState()

        state.update(enabled = true, delta = 0.01f)

        assertNull(state.update(enabled = true, delta = 0.1f))
        assertNull(state.update(enabled = true, delta = 0.1f))
        assertEquals(
            83.333,
            state.update(enabled = true, delta = 0.05f)!!,
            absoluteTolerance = 0.001
        )
    }

    @Test
    fun `format includes high-level renderer metrics`() {
        val text = SandboxPerformanceSnapshot(
            framesPerSecond = 60,
            averageFrameMs = 16.666,
            renderMs = 0.416,
            dynamicPlanMs = 0.054,
            drawCalls = 12,
            terrainDrawn = 320,
            terrainChecked = 2500,
            objectsDrawn = 18,
            objectsChecked = 24,
            entitiesDrawn = 7,
            entitiesChecked = 7,
            previewsDrawn = 0,
            staticPlanMs = 0.344,
            staticPlanUpdates = 1
        ).format()

        assertEquals(
            """
            FPS: 60
            Frame: 16.67 ms
            Render: 0.42 ms
            Plan: 0.05 ms
            Static plan: 0.34 ms
            Static updates: 1
            Draw calls: 12

            Terrain: 320/2500
            Objects: 18/24
            Entities: 7/7
            Previews: 0
            """.trimIndent(),
            text
        )
    }

    @Test
    fun `format omits inactive static plan metrics`() {
        val text = SandboxPerformanceSnapshot(
            framesPerSecond = 60,
            averageFrameMs = 16.0,
            renderMs = 0.4,
            dynamicPlanMs = 0.05,
            drawCalls = 12,
            terrainDrawn = 320,
            terrainChecked = 2500,
            objectsDrawn = 18,
            objectsChecked = 24,
            entitiesDrawn = 7,
            entitiesChecked = 7,
            previewsDrawn = 0,
            staticPlanMs = 0.0,
            staticPlanUpdates = 0
        ).format()

        assertFalse(text.contains("Static plan"))
        assertFalse(text.contains("Static updates"))
    }
}
