package com.mefabc24.strata.debug

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.pathfinding.PathMovementMode
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugSettingsTest {
    @Test
    fun `optional overlays are disabled while tool context defaults are ready`() {
        val settings = DebugSettings()
        assertFalse(settings.toolsWindow.enabled)
        assertFalse(settings.toolsWindow.visible)
        assertEquals(Input.Keys.F2, settings.toolsWindow.toggleKey)
        assertFalse(settings.debugWindow.enabled)
        assertFalse(settings.debugWindow.visible)
        assertEquals(Input.Keys.F3, settings.debugWindow.toggleKey)
        assertFalse(settings.performance.overlayEnabled)
        assertFalse(settings.performance.terminalLoggingEnabled)
        assertFalse(settings.performance.historyRecording)
        assertEquals(240, settings.performance.historyLength)
        assertEquals(DebugPerformanceMetric.FRAME_TIME, settings.performance.historyMetric)
        assertFalse(settings.simulation.enabled)
        assertFalse(settings.simulation.freezeVisualAnimations)
        assertFalse(settings.grid.enabled)
        assertFalse(settings.worldInfo.enabled)
        assertFalse(settings.worldInfo.showTileCoordinates)
        assertFalse(settings.worldInfo.showTerrainIds)
        assertFalse(settings.worldInfo.showOverlayInfo)
        assertFalse(settings.worldInfo.showOccupancy)
        assertFalse(settings.worldInfo.showMissingTerrainVisuals)
        assertFalse(settings.worldInfo.showOrigin)
        assertEquals(1.5f, settings.worldInfo.maximumLabelZoom)
        assertEquals(256, settings.worldInfo.maximumVisibleLabels)
        assertTrue(settings.worldVisibility.groundTerrainVisible)
        assertTrue(settings.worldVisibility.terrainOverlaysVisible)
        assertTrue(settings.worldVisibility.placedObjectsVisible)
        assertTrue(settings.worldVisibility.entitiesVisible)
        assertFalse(settings.objects.enabled)
        assertFalse(settings.entities.enabled)
        assertFalse(settings.entities.showMovementTrail)
        assertFalse(settings.entities.showMovementVector)
        assertFalse(settings.entities.showNextWaypoint)
        assertFalse(settings.entities.showMovementSpeed)
        assertFalse(settings.entities.showPositionTileOffset)
        assertEquals(120, settings.entities.trailMaxPositions)
        assertEquals(5f, settings.entities.trailHistoryDurationSeconds)
        assertEquals(0.02f, settings.entities.trailMinimumDistance)
        assertEquals(0.65f, settings.entities.trailOpacity)
        assertFalse(settings.picking.enabled)
        assertFalse(settings.renderOrder.enabled)
        assertEquals(RenderOrderDebugMode.CALCULATED, settings.renderOrder.mode)
        assertEquals(DebugVisualizationFilter.ALL, settings.visualizationFilter)
        assertFalse(settings.renderOrder.showPriorityLabels)
        assertFalse(settings.renderOrder.colorByPriority)
        assertEquals(RenderPriorityFocusMode.OFF, settings.renderOrder.priorityFocusMode)
        assertFalse(settings.renderOrder.showSortVolumes)
        assertFalse(settings.renderOrder.showSortAnchors)
        assertFalse(settings.renderOrder.showProjectedSortPositions)
        assertFalse(settings.renderOrder.showTerrainIndices)
        assertFalse(settings.renderOrder.showTerrainHeatmap)
        assertEquals(
            TerrainHeatmapSteps.PER_TILE,
            settings.renderOrder.terrainHeatmapSteps
        )
        assertFalse(settings.culling.enabled)
        assertFalse(settings.camera.enabled)
        assertFalse(settings.worldStats.enabled)
        assertFalse(settings.eventBus.enabled)
        assertFalse(settings.camera.disableRestrictions)
        assertTrue(settings.inspect.showTile)
        assertTrue(settings.inspect.showObjectFootprint)
        assertTrue(settings.inspect.showObjectOrigin)
        assertTrue(settings.inspect.showObjectSpriteBounds)
        assertTrue(settings.inspect.showEntityTile)
        assertTrue(settings.inspect.showEntityPosition)
        assertTrue(settings.inspect.showEntityPath)
        assertTrue(settings.inspect.showEntityDirection)
        assertTrue(settings.inspect.showEntitySpriteBounds)
        assertTrue(settings.pathfinding.enabled)
        assertEquals(PathMovementMode.FOUR_WAY, settings.pathfinding.movementMode)
        assertEquals(1f, settings.pathfinding.entitySpeedMultiplier)
        assertFalse(settings.pathfinding.showOpenSet)
        assertFalse(settings.pathfinding.showClosedSet)
        assertFalse(settings.pathfinding.showGCost)
        assertFalse(settings.pathfinding.showHCost)
        assertFalse(settings.pathfinding.showFCost)
        assertFalse(settings.pathfinding.showParentDirections)
        assertFalse(settings.pathfinding.showExplorationOrder)
        assertFalse(settings.pathfinding.showRejectedTransitions)
        assertEquals(1.5f, settings.pathfinding.maximumLabelZoom)
        assertEquals(128, settings.pathfinding.maximumVisibleLabels)
        assertEquals(2048, settings.pathfinding.maximumRejectedTransitions)
        assertEquals(1, settings.pathfinding.automaticIterationsPerUpdate)
        assertTrue(settings.notifications.enabled)
        assertNull(settings.worldState.pickingSelection.lockedTarget)
        assertEquals(1, settings.paint.brushSize)
        assertTrue(settings.paint.showBrushPreview)
        assertEquals(1, settings.delete.brushSize)
        assertTrue(settings.delete.dragEnabled)
        assertTrue(settings.delete.showBrushPreview)
    }

    @Test
    fun `editing brush sizes require supported odd values`() {
        val settings = DebugSettings()
        for (size in listOf(1, 3, 5, 7, 9)) {
            settings.paint.brushSize = size
            settings.delete.brushSize = size
        }
        for (size in listOf(-1, 0, 2, 10, 11)) {
            assertFailsWith<IllegalArgumentException> { settings.paint.brushSize = size }
            assertFailsWith<IllegalArgumentException> { settings.delete.brushSize = size }
        }
    }

    @Test
    fun `render order heatmap colors are copied on assignment and access`() {
        val settings = DebugRenderOrderSettings()
        val start = Color(0.1f, 0.2f, 0.3f, 0.4f)
        val end = Color(0.6f, 0.7f, 0.8f, 0.5f)
        settings.terrainHeatmapStartColor = start
        settings.terrainHeatmapEndColor = end

        start.set(Color.RED)
        end.set(Color.BLUE)
        settings.terrainHeatmapStartColor.set(Color.GREEN)
        settings.terrainHeatmapEndColor.set(Color.YELLOW)

        assertEquals(Color(0.1f, 0.2f, 0.3f, 0.4f), settings.terrainHeatmapStartColor)
        assertEquals(Color(0.6f, 0.7f, 0.8f, 0.5f), settings.terrainHeatmapEndColor)
    }

    @Test
    fun `render order heatmap defaults use increased opacity`() {
        val settings = DebugRenderOrderSettings()

        assertEquals(0.55f, settings.terrainHeatmapStartColor.a)
        assertEquals(0.55f, settings.terrainHeatmapEndColor.a)
    }

    @Test
    fun `render diagnostic numeric settings reject invalid values`() {
        val settings = DebugRenderOrderSettings()

        listOf(-0.1f, 1.1f, Float.NaN).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.priorityColorAlpha = invalid
            }
        }
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.sortGeometryLineWidth = invalid
            }
        }
    }

    @Test
    fun `world information settings validate limits and copy colors`() {
        val settings = DebugWorldInfoSettings()
        val color = Color(0.1f, 0.2f, 0.3f, 0.4f)
        settings.occupancyColor = color
        color.set(Color.RED)
        settings.occupancyColor.set(Color.BLUE)

        assertEquals(Color(0.1f, 0.2f, 0.3f, 0.4f), settings.occupancyColor)
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.maximumLabelZoom = invalid
            }
        }
        listOf(0, 4097).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.maximumVisibleLabels = invalid
            }
        }
    }

    @Test
    fun `off preset restores normal world visibility`() {
        val settings = DebugSettings().apply {
            worldVisibility.groundTerrainVisible = false
            worldVisibility.terrainOverlaysVisible = false
            worldVisibility.placedObjectsVisible = false
            worldVisibility.entitiesVisible = false
            worldVisibility.setOverlayLayerVisible("roads", false)
            worldInfo.enabled = true
        }

        settings.applyPreset(DebugPreset.OFF)

        assertFalse(settings.worldInfo.enabled)
        assertTrue(settings.worldVisibility.groundTerrainVisible)
        assertTrue(settings.worldVisibility.terrainOverlaysVisible)
        assertTrue(settings.worldVisibility.placedObjectsVisible)
        assertTrue(settings.worldVisibility.entitiesVisible)
        assertTrue(settings.worldVisibility.isOverlayLayerVisible("roads"))
    }

    @Test
    fun `off preset resets camera restriction override`() {
        val settings = DebugSettings().apply {
            camera.disableRestrictions = true
        }

        settings.applyPreset(DebugPreset.OFF)

        assertFalse(settings.camera.disableRestrictions)
    }

    @Test
    fun `window visibility is independent from feature settings`() {
        val settings = DebugSettings().apply {
            toolsWindow { enabled = true; visible = false }
            debugWindow { enabled = true; visible = true }
            grid { enabled = true }
        }
        assertTrue(settings.toolsWindow.enabled)
        assertFalse(settings.toolsWindow.visible)
        assertTrue(settings.debugWindow.enabled)
        assertTrue(settings.debugWindow.visible)
        assertTrue(settings.grid.enabled)
    }

    @Test
    fun `presets only change diagnostic settings`() {
        val settings = DebugSettings().apply {
            pathfinding.enabled = false
            pathfinding.showExploredNodes = false
            pathfinding.showFinalPath = true
            inspect.showEntityPath = false
            inspect.showObjectOrigin = false
        }
        settings.applyPreset(DebugPreset.PLACEMENT)
        assertTrue(settings.grid.enabled)
        assertTrue(settings.objects.showOccupiedTiles)
        assertFalse(settings.entities.hasActiveVisuals)

        settings.applyPreset(DebugPreset.EVERYTHING)
        assertTrue(settings.performance.overlayEnabled)
        assertFalse(settings.performance.terminalLoggingEnabled)
        assertTrue(settings.simulation.enabled)
        assertTrue(settings.grid.enabled)
        assertTrue(settings.worldInfo.hasActiveVisuals)
        assertTrue(settings.worldInfo.showTileCoordinates)
        assertTrue(settings.worldInfo.showTerrainIds)
        assertTrue(settings.worldInfo.showOverlayInfo)
        assertTrue(settings.worldInfo.showOccupancy)
        assertTrue(settings.worldInfo.showMissingTerrainVisuals)
        assertTrue(settings.objects.hasActiveVisuals)
        assertTrue(settings.entities.hasActiveVisuals)
        assertTrue(settings.picking.hasActiveVisuals)
        assertTrue(settings.renderOrder.hasActiveVisuals)
        assertTrue(settings.culling.hasActiveVisuals)
        assertTrue(settings.camera.hasActiveVisuals)
        assertTrue(settings.worldStats.enabled)
        assertTrue(settings.eventBus.enabled)
        assertTrue(settings.notifications.enabled)
        assertTrue(settings.camera.disableRestrictions)
        assertNotNull(settings.grid.backgroundColor)
        assertNotNull(settings.grid.hoverBackgroundColor)
        assertTrue(settings.objects.showOccupiedTiles)
        assertTrue(settings.objects.showOriginTile)
        assertTrue(settings.objects.showSpriteBounds)
        assertNotNull(settings.objects.occupiedTileFillColor)
        assertTrue(settings.entities.showCurrentTile)
        assertTrue(settings.entities.showPosition)
        assertTrue(settings.entities.showPath)
        assertTrue(settings.entities.showDirection)
        assertTrue(settings.entities.showSpriteBounds)
        assertNotNull(settings.entities.currentTileFillColor)
        assertTrue(settings.picking.showSpriteBounds)
        assertTrue(settings.picking.showCursorHit)
        assertTrue(settings.renderOrder.showLabels)
        assertTrue(settings.culling.showVisibleArea)
        assertTrue(settings.culling.showObjectBounds)
        assertTrue(settings.culling.showEntityBounds)
        assertTrue(settings.camera.showVisibleArea)
        assertTrue(settings.camera.showWorldBounds)
        assertTrue(settings.camera.showClampBounds)
        assertFalse(settings.pathfinding.enabled)
        assertFalse(settings.pathfinding.showExploredNodes)
        assertTrue(settings.pathfinding.showFinalPath)
        assertFalse(settings.inspect.showEntityPath)
        assertFalse(settings.inspect.showObjectOrigin)
    }

    @Test
    fun `off preset disables every optional diagnostic overlay`() {
        val settings = DebugSettings().apply {
            DebugPreset.EVERYTHING.let(::applyPreset)
            simulation.enabled = true
            toolsWindow.enabled = true
            toolsWindow.visible = true
            debugWindow.enabled = true
            debugWindow.visible = true
        }

        settings.applyPreset(DebugPreset.OFF)

        assertFalse(settings.performance.overlayEnabled)
        assertFalse(settings.simulation.enabled)
        assertFalse(settings.grid.enabled)
        assertFalse(settings.worldInfo.enabled)
        assertFalse(settings.objects.enabled)
        assertFalse(settings.entities.enabled)
        assertFalse(settings.picking.enabled)
        assertFalse(settings.renderOrder.enabled)
        assertFalse(settings.culling.enabled)
        assertFalse(settings.camera.enabled)
        assertFalse(settings.worldStats.enabled)
        assertFalse(settings.eventBus.enabled)
        assertFalse(settings.notifications.enabled)
        assertFalse(settings.camera.disableRestrictions)
        assertTrue(settings.pathfinding.enabled)
        assertTrue(settings.pathfinding.showExploredNodes)
        assertTrue(settings.pathfinding.showFinalPath)
        assertTrue(settings.toolsWindow.enabled)
        assertTrue(settings.toolsWindow.visible)
        assertTrue(settings.debugWindow.enabled)
        assertTrue(settings.debugWindow.visible)
    }

    @Test
    fun `every preset preserves tool visualization settings`() {
        DebugPreset.entries.forEach { preset ->
            val settings = DebugSettings().apply {
                pathfinding.enabled = false
                pathfinding.showExploredNodes = false
                pathfinding.showFinalPath = true
                inspect.showTile = false
                inspect.showEntityDirection = false
            }

            settings.applyPreset(preset)

            assertFalse(settings.pathfinding.enabled, preset.name)
            assertFalse(settings.pathfinding.showExploredNodes, preset.name)
            assertTrue(settings.pathfinding.showFinalPath, preset.name)
            assertFalse(settings.inspect.showTile, preset.name)
            assertFalse(settings.inspect.showEntityDirection, preset.name)
        }
    }

    @Test
    fun `every preset starts from a disabled diagnostic state`() {
        val settings = DebugSettings()
        DebugPreset.entries.forEach { preset ->
            settings.applyPreset(DebugPreset.EVERYTHING)
            settings.simulation.enabled = true
            settings.applyPreset(preset)
            assertEquals(
                preset == DebugPreset.EVERYTHING,
                settings.simulation.enabled,
                preset.name
            )
        }
        settings.applyPreset(DebugPreset.MINIMAL)
        assertTrue(settings.performance.overlayEnabled)
        assertTrue(settings.grid.enabled)
        assertFalse(settings.worldStats.enabled)
        settings.applyPreset(DebugPreset.RENDERING)
        assertTrue(settings.renderOrder.hasActiveVisuals)
        assertTrue(settings.culling.hasActiveVisuals)
        assertTrue(settings.camera.hasActiveVisuals)
    }

    @Test
    fun `only everything disables camera restrictions`() {
        DebugPreset.entries.forEach { preset ->
            val settings = DebugSettings()

            settings.applyPreset(preset)

            assertEquals(
                preset == DebugPreset.EVERYTHING,
                settings.camera.disableRestrictions,
                preset.name
            )
        }
    }

    @Test
    fun `path cost callback receives world and directed edge`() {
        val settings = DebugSettings()
        val world = World(2, 1) { _, _ -> TestTile }
        val from = TilePosition(0, 0)
        val to = TilePosition(1, 0)
        settings.pathCost { suppliedWorld, suppliedFrom, suppliedTo ->
            assertTrue(suppliedWorld === world)
            assertEquals(from, suppliedFrom)
            assertEquals(to, suppliedTo)
            2.5f
        }

        assertEquals(2.5f, settings.pathCost?.invoke(world, from, to))
    }

    @Test
    fun `debug entity speed multiplier must be finite and positive`() {
        val settings = DebugSettings()

        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.pathfinding.entitySpeedMultiplier = invalid
            }
        }
    }

    @Test
    fun `pathfinding diagnostic limits reject invalid values`() {
        val settings = DebugPathfindingSettings()
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { settings.maximumLabelZoom = invalid }
        }
        listOf(0, 4097).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { settings.maximumVisibleLabels = invalid }
        }
        listOf(-1, 16385).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.maximumRejectedTransitions = invalid
            }
        }
        listOf(0, 1025).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.automaticIterationsPerUpdate = invalid
            }
        }
    }

    @Test
    fun `entity trail bounds and movement vector scale reject invalid values`() {
        val settings = DebugEntitySettings()

        listOf(1, 4097).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.trailMaxPositions = invalid
            }
        }
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.trailHistoryDurationSeconds = invalid
            }
            assertFailsWith<IllegalArgumentException> {
                settings.movementVectorScaleSeconds = invalid
            }
        }
        listOf(-0.1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.trailMinimumDistance = invalid
            }
        }
        listOf(-0.1f, 1.1f, Float.NaN).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.trailOpacity = invalid
            }
        }
    }

    @Test
    fun `debug window startup visibility is independent from runtime visibility`() {
        val settings = DebugSettings()

        assertFalse(settings.debugWindow.visibleOnStartup)
        assertFalse(settings.debugWindow.visible)

        settings.debugWindow {
            enabled = true
            visibleOnStartup = true
        }

        assertTrue(settings.debugWindow.visibleOnStartup)

        // Runtime visibility is not changed by configuration alone.
        assertFalse(settings.debugWindow.visible)
    }

    @Test
    fun `enabled windows require independent shortcuts`() {
        val settings = DebugSettings().apply {
            toolsWindow { enabled = true; toggleKey = Input.Keys.F4 }
            debugWindow { enabled = true; toggleKey = Input.Keys.F4 }
        }

        assertFailsWith<IllegalArgumentException> {
            settings.validateWindowConfiguration()
        }
    }

    private data object TestTile : Tile
}
