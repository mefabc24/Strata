package com.mefabc24.strata.render.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.mefabc24.strata.debug.DebugRenderOrderSettings
import com.mefabc24.strata.debug.RenderPriorityFocusMode
import com.mefabc24.strata.debug.RenderOrderDebugMode
import com.mefabc24.strata.debug.TerrainHeatmapSteps
import com.mefabc24.strata.render.RenderDebugSnapshot
import com.mefabc24.strata.render.RenderItemDebugSnapshot
import com.mefabc24.strata.render.TerrainRenderDebugSnapshot
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.TilePosition
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

class TerrainRenderOrderDebugTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

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
        TerrainHeatmapSteps.entries.forEach { steps ->
            assertEquals(0f, terrainHeatmapProgress(rank = 0, terrainCount = 0, steps))
            assertEquals(0f, terrainHeatmapProgress(rank = 0, terrainCount = 1, steps))
        }
    }

    @Test
    fun `per tile heatmap retains unquantized interpolation`() {
        val progress = (0 until 9).map { rank ->
            terrainHeatmapProgress(rank, terrainCount = 9, TerrainHeatmapSteps.PER_TILE)
        }

        assertEquals((0 until 9).map { it / 8f }, progress)
    }

    @Test
    fun `fixed heatmap steps produce their configured number of color levels`() {
        val start = Color(0f, 0f, 0f, 0.25f)
        val end = Color(1f, 1f, 1f, 0.25f)

        listOf(
            TerrainHeatmapSteps.STEPS_32 to 32,
            TerrainHeatmapSteps.STEPS_16 to 16,
            TerrainHeatmapSteps.STEPS_8 to 8
        ).forEach { (steps, expectedLevels) ->
            val colors = (0 until 4097).map { rank ->
                terrainHeatmapColor(start, end, rank, 4097, steps).toFloatBits()
            }.toSet()

            assertEquals(expectedLevels, colors.size, steps.name)
        }
    }

    @Test
    fun `every heatmap step mode preserves both color endpoints`() {
        val start = Color(0.1f, 0.2f, 0.3f, 0.4f)
        val end = Color(0.9f, 0.8f, 0.7f, 0.6f)

        TerrainHeatmapSteps.entries.forEach { steps ->
            assertEquals(start, terrainHeatmapColor(start, end, 0, 257, steps))
            assertEquals(end, terrainHeatmapColor(start, end, 256, 257, steps))
        }
    }

    @Test
    fun `selected mode supplies both label indices and terrain heatmap ranks`() {
        val terrain = RenderItemDebugSnapshot(
            index = 7,
            actualIndex = 2,
            terrain = TerrainRenderDebugSnapshot(
                position = TilePosition(1, 2),
                rank = 5,
                actualRank = 1
            ),
            drawn = true
        )
        val objectItem = RenderItemDebugSnapshot(
            index = 8,
            actualIndex = 4,
            drawn = true
        )
        val culled = RenderItemDebugSnapshot(index = 9, drawn = false)
        val snapshot = RenderDebugSnapshot(
            visibleArea = com.badlogic.gdx.math.Rectangle(),
            items = listOf(terrain, objectItem, culled),
            terrainCount = 12,
            actualTerrainCount = 3
        )

        assertEquals(
            RenderOrderDebugMetadata(index = 7, terrainRank = 5, terrainCount = 12),
            renderOrderDebugMetadata(terrain, snapshot, RenderOrderDebugMode.CALCULATED)
        )
        assertEquals(
            RenderOrderDebugMetadata(index = 2, terrainRank = 1, terrainCount = 3),
            renderOrderDebugMetadata(terrain, snapshot, RenderOrderDebugMode.ACTUAL)
        )
        assertEquals(
            8,
            renderOrderDebugMetadata(
                objectItem,
                snapshot,
                RenderOrderDebugMode.CALCULATED
            )?.index
        )
        assertEquals(
            4,
            renderOrderDebugMetadata(
                objectItem,
                snapshot,
                RenderOrderDebugMode.ACTUAL
            )?.index
        )
        assertNull(renderOrderDebugMetadata(culled, snapshot, RenderOrderDebugMode.ACTUAL))

        terrainHeatmapProgress(5, 12, TerrainHeatmapSteps.STEPS_8)
        assertEquals(7, terrain.index)
        assertEquals(2, terrain.actualIndex)
        assertEquals(5, terrain.terrain?.rank)
        assertEquals(1, terrain.terrain?.actualRank)
    }

    @Test
    fun `terrain label scaling restores the font used by object labels`() {
        val font = BitmapFont()
        try {
            font.data.setScale(1.25f, 0.8f)

            font.withRelativeScale(0.68f) {
                assertEquals(0.85f, font.data.scaleX, absoluteTolerance = 0.0001f)
                assertEquals(0.544f, font.data.scaleY, absoluteTolerance = 0.0001f)
            }

            assertEquals(1.25f, font.data.scaleX)
            assertEquals(0.8f, font.data.scaleY)
        } finally {
            font.dispose()
        }
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
            assertEquals(
                settings.showTerrainHeatmap,
                layers.terrainHeatmapGrid,
                "heatmap grid mask $mask"
            )
        }

        val disabled = renderOrderDebugLayers(DebugRenderOrderSettings().apply {
            showLabels = true
            showTerrainIndices = true
            showTerrainHeatmap = true
        })
        assertFalse(disabled.objectEntityLabels)
        assertFalse(disabled.terrainIndices)
        assertFalse(disabled.terrainHeatmap)
        assertFalse(disabled.terrainHeatmapGrid)

        val settings = DebugRenderOrderSettings().apply {
            mode = RenderOrderDebugMode.ACTUAL
            terrainHeatmapSteps = TerrainHeatmapSteps.STEPS_8
            showLabels = false
            showTerrainIndices = true
            showTerrainHeatmap = false
        }
        assertEquals(RenderOrderDebugMode.ACTUAL, settings.mode)
        assertEquals(TerrainHeatmapSteps.STEPS_8, settings.terrainHeatmapSteps)
        assertFalse(settings.showLabels)
        assertTrue(settings.showTerrainIndices)
        assertFalse(settings.showTerrainHeatmap)
    }

    @Test
    fun `priority and sort geometry layers remain independently configurable`() {
        val settings = DebugRenderOrderSettings().apply { enabled = true }
        val setters = listOf<(Boolean) -> Unit>(
            { settings.showPriorityLabels = it },
            { settings.colorByPriority = it },
            {
                settings.priorityFocusMode = if (it) {
                    RenderPriorityFocusMode.HIGHLIGHT
                } else {
                    RenderPriorityFocusMode.OFF
                }
            },
            { settings.showSortVolumes = it },
            { settings.showSortAnchors = it },
            { settings.showProjectedSortPositions = it }
        )

        setters.indices.forEach { selected ->
            setters.forEach { it(false) }
            setters[selected](true)
            val layers = renderOrderDebugLayers(settings)
            assertEquals(
                listOf(
                    layers.priorityLabels,
                    layers.priorityColors,
                    layers.priorityFocus,
                    layers.sortVolumes,
                    layers.sortAnchors,
                    layers.projectedSortPositions
                ),
                setters.indices.map { it == selected }
            )
        }
    }

    @Test
    fun `priority palette is stable for negative values and focus can isolate`() {
        val negative = renderPriorityColor(-1, 0.35f)
        val wrapped = renderPriorityColor(7, 0.35f)
        val next = renderPriorityColor(0, 0.35f)

        assertEquals(wrapped, negative)
        assertNotEquals(negative, next)
        assertEquals(0.35f, negative.a)

        val item = RenderItemDebugSnapshot(
            index = 0,
            drawn = true,
            sort = com.mefabc24.strata.render.RenderSortDebugSnapshot(
                minX = 0f,
                maxX = 1f,
                minY = 0f,
                maxY = 1f,
                projectedFrontX = 0f,
                projectedFrontY = -1f,
                renderPriority = 3
            )
        )
        val settings = DebugRenderOrderSettings().apply {
            priorityFocusMode = RenderPriorityFocusMode.ISOLATE
            selectedPriority = 3
        }

        assertTrue(renderPriorityFocusAllows(settings, item))
        settings.selectedPriority = 4
        assertFalse(renderPriorityFocusAllows(settings, item))
        settings.priorityFocusMode = RenderPriorityFocusMode.HIGHLIGHT
        assertTrue(renderPriorityFocusAllows(settings, item))
    }
}
