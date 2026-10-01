package com.mefabc24.strata.render.debug

import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.debug.DebugRenderOrderSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TerrainRenderOrderDebugTest {
    @Test
    fun `heatmap endpoints and midpoint follow terrain rank`() {
        val start = Color(0f, 0.2f, 0.4f, 0.25f)
        val end = Color(1f, 0.8f, 0.6f, 0.35f)

        assertEquals(start, terrainHeatmapColor(start, end, rank = 0, terrainCount = 3))
        assertEquals(end, terrainHeatmapColor(start, end, rank = 2, terrainCount = 3))
        assertEquals(
            Color(0.5f, 0.5f, 0.5f, 0.3f),
            terrainHeatmapColor(start, end, rank = 1, terrainCount = 3)
        )
    }

    @Test
    fun `single terrain cell uses the start endpoint without division by zero`() {
        val start = Color(0.1f, 0.2f, 0.3f, 0.4f)
        val end = Color(0.9f, 0.8f, 0.7f, 0.6f)

        assertEquals(0f, terrainHeatmapProgress(rank = 0, terrainCount = 1))
        assertEquals(start, terrainHeatmapColor(start, end, rank = 0, terrainCount = 1))
    }

    @Test
    fun `render order layers remain independently configurable`() {
        for (mask in 0..7) {
            val settings = DebugRenderOrderSettings().apply {
                enabled = true
                showLabels = mask and 1 != 0
                showTerrainIndices = mask and 2 != 0
                showTerrainHeatmap = mask and 4 != 0
            }

            val layers = renderOrderDebugLayers(settings)

            assertEquals(settings.showLabels, layers.objectEntityLabels, "labels mask $mask")
            assertEquals(settings.showTerrainIndices, layers.terrainIndices, "indices mask $mask")
            assertEquals(settings.showTerrainHeatmap, layers.terrainHeatmap, "heatmap mask $mask")
        }

        val disabled = renderOrderDebugLayers(DebugRenderOrderSettings().apply {
            showLabels = true
            showTerrainIndices = true
            showTerrainHeatmap = true
        })
        assertFalse(disabled.objectEntityLabels)
        assertFalse(disabled.terrainIndices)
        assertFalse(disabled.terrainHeatmap)
    }
}
