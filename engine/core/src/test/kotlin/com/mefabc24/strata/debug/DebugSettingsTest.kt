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
        assertFalse(settings.ui.toolRail.enabled)
        assertFalse(settings.ui.toolRail.isVisible)
        assertEquals(Input.Keys.F2, settings.ui.toolRail.toggleKey)
        assertFalse(settings.ui.settingsWindow.enabled)
        assertFalse(settings.ui.settingsWindow.isVisible)
        assertEquals(Input.Keys.F3, settings.ui.settingsWindow.toggleKey)
        assertFalse(settings.operations.performance.overlayEnabled)
        assertFalse(settings.operations.performance.terminalLoggingEnabled)
        assertFalse(settings.operations.performance.historyRecording)
        assertEquals(240, settings.operations.performance.historyLength)
        assertEquals(DebugPerformanceMetric.FRAME_TIME, settings.operations.performance.historyMetric)
        assertFalse(settings.operations.simulation.enabled)
        assertFalse(settings.operations.simulation.freezeVisualAnimations)
        assertFalse(settings.visuals.grid.enabled)
        assertFalse(settings.visuals.worldInfo.hasActiveVisuals)
        assertFalse(settings.visuals.worldInfo.showTileCoordinates)
        assertFalse(settings.visuals.worldInfo.showTerrainIds)
        assertFalse(settings.visuals.worldInfo.showOverlayInfo)
        assertFalse(settings.visuals.worldInfo.showOccupancy)
        assertFalse(settings.visuals.worldInfo.showMissingTerrainVisuals)
        assertFalse(settings.visuals.worldInfo.showOrigin)
        assertEquals(1.5f, settings.visuals.worldInfo.maximumLabelZoom)
        assertEquals(256, settings.visuals.worldInfo.maximumVisibleLabels)
        assertTrue(settings.visuals.worldVisibility.groundTerrainVisible)
        assertTrue(settings.visuals.worldVisibility.terrainOverlaysVisible)
        assertTrue(settings.visuals.worldVisibility.placedObjectsVisible)
        assertTrue(settings.visuals.worldVisibility.entitiesVisible)
        assertFalse(settings.visuals.objects.hasActiveVisuals)
        assertFalse(settings.visuals.entities.hasActiveVisuals)
        assertFalse(settings.visuals.entities.showMovementTrail)
        assertFalse(settings.visuals.entities.showMovementVector)
        assertFalse(settings.visuals.entities.showNextWaypoint)
        assertFalse(settings.visuals.entities.showMovementSpeed)
        assertFalse(settings.visuals.entities.showPositionTileOffset)
        assertEquals(120, settings.visuals.entities.trailMaxPositions)
        assertEquals(5f, settings.visuals.entities.trailHistoryDurationSeconds)
        assertEquals(0.02f, settings.visuals.entities.trailMinimumDistance)
        assertEquals(0.65f, settings.visuals.entities.trailOpacity)
        assertFalse(settings.visuals.picking.enabled)
        assertFalse(settings.visuals.picking.hasActiveVisuals)
        assertFalse(settings.visuals.renderOrder.hasActiveVisuals)
        assertEquals(RenderOrderDebugMode.CALCULATED, settings.visuals.renderOrder.mode)
        assertEquals(DebugVisualizationFilter.ALL, settings.visuals.filter)
        assertFalse(settings.visuals.renderOrder.showPriorityLabels)
        assertFalse(settings.visuals.renderOrder.colorByPriority)
        assertEquals(RenderPriorityFocusMode.OFF, settings.visuals.renderOrder.priorityFocusMode)
        assertFalse(settings.visuals.renderOrder.showSortVolumes)
        assertFalse(settings.visuals.renderOrder.showSortAnchors)
        assertFalse(settings.visuals.renderOrder.showProjectedSortPositions)
        assertFalse(settings.visuals.renderOrder.showTerrainIndices)
        assertFalse(settings.visuals.renderOrder.showTerrainHeatmap)
        assertEquals(
            TerrainHeatmapSteps.PER_TILE,
            settings.visuals.renderOrder.terrainHeatmapSteps
        )
        assertFalse(settings.visuals.culling.hasActiveVisuals)
        assertFalse(settings.visuals.camera.hasActiveVisuals)
        assertFalse(settings.operations.worldStats.enabled)
        assertFalse(settings.operations.eventBus.visible)
        assertFalse(settings.operations.eventBus.captureEnabled)
        assertFalse(settings.operations.disableCameraRestrictions)
        assertTrue(settings.tools.inspect.showTile)
        assertTrue(settings.tools.inspect.showObjectFootprint)
        assertTrue(settings.tools.inspect.showObjectOrigin)
        assertTrue(settings.tools.inspect.showObjectSpriteBounds)
        assertTrue(settings.tools.inspect.showEntityTile)
        assertTrue(settings.tools.inspect.showEntityPosition)
        assertTrue(settings.tools.inspect.showEntityPath)
        assertTrue(settings.tools.inspect.showEntityDirection)
        assertTrue(settings.tools.inspect.showEntitySpriteBounds)
        assertTrue(settings.tools.pathfinding.enabled)
        assertEquals(PathMovementMode.FOUR_WAY, settings.tools.pathfinding.movementMode)
        assertEquals(1f, settings.tools.pathfinding.entitySpeedMultiplier)
        assertFalse(settings.tools.pathfinding.showOpenSet)
        assertFalse(settings.tools.pathfinding.showClosedSet)
        assertFalse(settings.tools.pathfinding.showGCost)
        assertFalse(settings.tools.pathfinding.showHCost)
        assertFalse(settings.tools.pathfinding.showFCost)
        assertFalse(settings.tools.pathfinding.showParentDirections)
        assertFalse(settings.tools.pathfinding.showExplorationOrder)
        assertFalse(settings.tools.pathfinding.showRejectedTransitions)
        assertEquals(1.5f, settings.tools.pathfinding.maximumLabelZoom)
        assertEquals(128, settings.tools.pathfinding.maximumVisibleLabels)
        assertEquals(2048, settings.tools.pathfinding.maximumRejectedTransitions)
        assertEquals(1, settings.tools.pathfinding.automaticIterationsPerUpdate)
        assertTrue(settings.operations.notifications.enabled)
        assertNull(settings.worldState.pickingSelection.lockedTarget)
        assertEquals(1, settings.tools.paint.brushSize)
        assertTrue(settings.tools.paint.showBrushPreview)
        assertFalse(settings.tools.paint.showTileBorders)
        assertEquals(1, settings.tools.delete.brushSize)
        assertTrue(settings.tools.delete.dragEnabled)
        assertTrue(settings.tools.delete.showBrushPreview)
        assertFalse(settings.tools.delete.showTileBorders)
    }

    @Test
    fun `editing brush sizes require supported odd values`() {
        val settings = DebugSettings()
        for (size in listOf(1, 3, 5, 7, 9)) {
            settings.tools.paint.brushSize = size
            settings.tools.delete.brushSize = size
        }
        for (size in listOf(-1, 0, 2, 10, 11)) {
            assertFailsWith<IllegalArgumentException> { settings.tools.paint.brushSize = size }
            assertFailsWith<IllegalArgumentException> { settings.tools.delete.brushSize = size }
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
            visuals.worldVisibility.groundTerrainVisible = false
            visuals.worldVisibility.terrainOverlaysVisible = false
            visuals.worldVisibility.placedObjectsVisible = false
            visuals.worldVisibility.entitiesVisible = false
            visuals.worldVisibility.setOverlayLayerVisible("roads", false)
            visuals.worldInfo.showOrigin = true
        }

        settings.applyPreset(DebugPreset.OFF)

        assertFalse(settings.visuals.worldInfo.hasActiveVisuals)
        assertTrue(settings.visuals.worldVisibility.groundTerrainVisible)
        assertTrue(settings.visuals.worldVisibility.terrainOverlaysVisible)
        assertTrue(settings.visuals.worldVisibility.placedObjectsVisible)
        assertTrue(settings.visuals.worldVisibility.entitiesVisible)
        assertTrue(settings.visuals.worldVisibility.isOverlayLayerVisible("roads"))
    }

    @Test
    fun `off preset preserves camera restriction override`() {
        val settings = DebugSettings().apply {
            operations.disableCameraRestrictions = true
        }

        settings.applyPreset(DebugPreset.OFF)

        assertTrue(settings.operations.disableCameraRestrictions)
    }

    @Test
    fun `window visibility is independent from feature settings`() {
        val settings = DebugSettings().apply {
            ui.toolRail.apply { enabled = true; isVisible = false }
            ui.settingsWindow.apply { enabled = true; isVisible = true }
            visuals.grid.apply { enabled = true }
        }
        assertTrue(settings.ui.toolRail.enabled)
        assertFalse(settings.ui.toolRail.isVisible)
        assertTrue(settings.ui.settingsWindow.enabled)
        assertTrue(settings.ui.settingsWindow.isVisible)
        assertTrue(settings.visuals.grid.enabled)
    }

    @Test
    fun `individual visualization controls activate their category independently`() {
        val settings = DebugSettings().apply {
            visuals.worldInfo.showOrigin = true
            visuals.objects.showSpriteBounds = true
            visuals.entities.showDirection = true
            visuals.picking.enabled = true
            visuals.picking.showCursorHit = true
            visuals.renderOrder.showSortAnchors = true
            visuals.culling.showObjectBounds = true
            visuals.camera.showWorldBounds = true
        }

        assertTrue(settings.visuals.worldInfo.hasActiveVisuals)
        assertTrue(settings.visuals.objects.hasActiveVisuals)
        assertTrue(settings.visuals.entities.hasActiveVisuals)
        assertTrue(settings.visuals.picking.hasActiveVisuals)
        assertTrue(settings.visuals.renderOrder.hasActiveVisuals)
        assertTrue(settings.visuals.culling.hasActiveVisuals)
        assertTrue(settings.visuals.camera.hasActiveVisuals)
    }

    @Test
    fun `picking activation preserves configured child visuals`() {
        val picking = DebugPickingSettings().apply {
            showSpriteBounds = true
        }

        assertTrue(picking.hasConfiguredVisuals)
        assertFalse(picking.hasActiveVisuals)

        picking.enabled = true
        assertTrue(picking.hasActiveVisuals)

        picking.enabled = false
        assertTrue(picking.showSpriteBounds)
        assertFalse(picking.hasActiveVisuals)

        picking.enabled = true
        assertTrue(picking.hasActiveVisuals)
    }

    @Test
    fun `presets only change diagnostic settings`() {
        val settings = DebugSettings().apply {
            tools.pathfinding.enabled = false
            tools.pathfinding.showExploredNodes = false
            tools.pathfinding.showFinalPath = true
            tools.inspect.showEntityPath = false
            tools.inspect.showObjectOrigin = false
        }
        settings.applyPreset(DebugPreset.PLACEMENT)
        assertTrue(settings.visuals.grid.enabled)
        assertTrue(settings.visuals.objects.showOccupiedTiles)
        assertFalse(settings.visuals.entities.hasActiveVisuals)

        settings.applyPreset(DebugPreset.EVERYTHING)
        assertTrue(settings.operations.performance.overlayEnabled)
        assertFalse(settings.operations.performance.terminalLoggingEnabled)
        assertTrue(settings.operations.simulation.enabled)
        assertTrue(settings.visuals.grid.enabled)
        assertTrue(settings.visuals.worldInfo.hasActiveVisuals)
        assertTrue(settings.visuals.worldInfo.showTileCoordinates)
        assertTrue(settings.visuals.worldInfo.showTerrainIds)
        assertTrue(settings.visuals.worldInfo.showOverlayInfo)
        assertTrue(settings.visuals.worldInfo.showOccupancy)
        assertTrue(settings.visuals.worldInfo.showMissingTerrainVisuals)
        assertTrue(settings.visuals.objects.hasActiveVisuals)
        assertTrue(settings.visuals.entities.hasActiveVisuals)
        assertTrue(settings.visuals.picking.enabled)
        assertTrue(settings.visuals.picking.hasActiveVisuals)
        assertTrue(settings.visuals.renderOrder.hasActiveVisuals)
        assertTrue(settings.visuals.culling.hasActiveVisuals)
        assertTrue(settings.visuals.camera.hasActiveVisuals)
        assertTrue(settings.operations.worldStats.enabled)
        assertFalse(settings.operations.eventBus.visible)
        assertTrue(settings.operations.notifications.enabled)
        assertFalse(settings.operations.disableCameraRestrictions)
        assertTrue(settings.visuals.grid.showBackground)
        assertTrue(settings.visuals.grid.showHoverBackground)
        assertTrue(settings.visuals.objects.showOccupiedTiles)
        assertTrue(settings.visuals.objects.showOriginTile)
        assertTrue(settings.visuals.objects.showSpriteBounds)
        assertTrue(settings.visuals.objects.showOccupiedTileFill)
        assertTrue(settings.visuals.entities.showCurrentTile)
        assertTrue(settings.visuals.entities.showPosition)
        assertTrue(settings.visuals.entities.showPath)
        assertTrue(settings.visuals.entities.showDirection)
        assertTrue(settings.visuals.entities.showSpriteBounds)
        assertTrue(settings.visuals.entities.showCurrentTileFill)
        assertTrue(settings.visuals.picking.showSpriteBounds)
        assertTrue(settings.visuals.picking.showCursorHit)
        assertTrue(settings.visuals.renderOrder.showLabels)
        assertTrue(settings.visuals.culling.showVisibleArea)
        assertTrue(settings.visuals.culling.showObjectBounds)
        assertTrue(settings.visuals.culling.showEntityBounds)
        assertTrue(settings.visuals.camera.showVisibleArea)
        assertTrue(settings.visuals.camera.showWorldBounds)
        assertTrue(settings.visuals.camera.showClampBounds)
        assertFalse(settings.tools.pathfinding.enabled)
        assertFalse(settings.tools.pathfinding.showExploredNodes)
        assertTrue(settings.tools.pathfinding.showFinalPath)
        assertFalse(settings.tools.inspect.showEntityPath)
        assertFalse(settings.tools.inspect.showObjectOrigin)
    }

    @Test
    fun `off preset disables every optional diagnostic overlay`() {
        val settings = DebugSettings().apply {
            DebugPreset.EVERYTHING.let(::applyPreset)
            operations.simulation.enabled = true
            ui.toolRail.enabled = true
            ui.toolRail.isVisible = true
            ui.settingsWindow.enabled = true
            ui.settingsWindow.isVisible = true
        }

        settings.applyPreset(DebugPreset.OFF)

        assertFalse(settings.operations.performance.overlayEnabled)
        assertFalse(settings.operations.simulation.enabled)
        assertFalse(settings.visuals.grid.enabled)
        assertFalse(settings.visuals.grid.showBackground)
        assertFalse(settings.visuals.grid.showHoverBackground)
        assertFalse(settings.visuals.worldInfo.hasActiveVisuals)
        assertFalse(settings.visuals.objects.hasActiveVisuals)
        assertFalse(settings.visuals.objects.showOccupiedTileFill)
        assertFalse(settings.visuals.entities.hasActiveVisuals)
        assertFalse(settings.visuals.entities.showCurrentTileFill)
        assertFalse(settings.visuals.picking.enabled)
        assertFalse(settings.visuals.picking.hasActiveVisuals)
        assertFalse(settings.visuals.renderOrder.hasActiveVisuals)
        assertFalse(settings.visuals.culling.hasActiveVisuals)
        assertFalse(settings.visuals.camera.hasActiveVisuals)
        assertFalse(settings.operations.worldStats.enabled)
        assertFalse(settings.operations.eventBus.visible)
        assertTrue(settings.operations.notifications.enabled)
        assertFalse(settings.operations.disableCameraRestrictions)
        assertTrue(settings.tools.pathfinding.enabled)
        assertTrue(settings.tools.pathfinding.showExploredNodes)
        assertTrue(settings.tools.pathfinding.showFinalPath)
        assertTrue(settings.ui.toolRail.enabled)
        assertTrue(settings.ui.toolRail.isVisible)
        assertTrue(settings.ui.settingsWindow.enabled)
        assertTrue(settings.ui.settingsWindow.isVisible)
    }

    @Test
    fun `every preset preserves tool visualization settings`() {
        DebugPreset.entries.forEach { preset ->
            val settings = DebugSettings().apply {
                tools.pathfinding.enabled = false
                tools.pathfinding.showExploredNodes = false
                tools.pathfinding.showFinalPath = true
                tools.inspect.showTile = false
                tools.inspect.showEntityDirection = false
            }

            settings.applyPreset(preset)

            assertFalse(settings.tools.pathfinding.enabled, preset.name)
            assertFalse(settings.tools.pathfinding.showExploredNodes, preset.name)
            assertTrue(settings.tools.pathfinding.showFinalPath, preset.name)
            assertFalse(settings.tools.inspect.showTile, preset.name)
            assertFalse(settings.tools.inspect.showEntityDirection, preset.name)
        }
    }

    @Test
    fun `every preset preserves operational switches`() {
        DebugPreset.entries.forEach { preset ->
            val settings = DebugSettings().apply {
                operations.eventBus.visible = true
                operations.eventBus.captureEnabled = false
                operations.notifications.enabled = false
                operations.disableCameraRestrictions = true
            }

            settings.applyPreset(preset)

            assertTrue(settings.operations.eventBus.visible, preset.name)
            assertFalse(settings.operations.eventBus.captureEnabled, preset.name)
            assertFalse(settings.operations.notifications.enabled, preset.name)
            assertTrue(settings.operations.disableCameraRestrictions, preset.name)
        }
    }

    @Test
    fun `every preset starts from a disabled diagnostic state`() {
        val settings = DebugSettings()
        DebugPreset.entries.forEach { preset ->
            settings.applyPreset(DebugPreset.EVERYTHING)
            settings.operations.simulation.enabled = true
            settings.applyPreset(preset)
            assertEquals(
                preset == DebugPreset.EVERYTHING,
                settings.operations.simulation.enabled,
                preset.name
            )
        }
        settings.applyPreset(DebugPreset.MINIMAL)
        assertTrue(settings.operations.performance.overlayEnabled)
        assertTrue(settings.visuals.grid.enabled)
        assertFalse(settings.operations.worldStats.enabled)
        settings.applyPreset(DebugPreset.RENDERING)
        assertTrue(settings.visuals.renderOrder.hasActiveVisuals)
        assertTrue(settings.visuals.culling.hasActiveVisuals)
        assertTrue(settings.visuals.camera.hasActiveVisuals)
    }

    @Test
    fun `presets preserve camera restriction configuration`() {
        DebugPreset.entries.forEach { preset ->
            val settings = DebugSettings().apply {
                operations.disableCameraRestrictions = true
            }

            settings.applyPreset(preset)

            assertTrue(settings.operations.disableCameraRestrictions, preset.name)
        }
    }

    @Test
    fun `path cost callback receives world and directed edge`() {
        val settings = DebugSettings()
        val world = World(2, 1) { _, _ -> TestTile }
        val from = TilePosition(0, 0)
        val to = TilePosition(1, 0)
        settings.tools.pathCost { suppliedWorld, suppliedFrom, suppliedTo ->
            assertTrue(suppliedWorld === world)
            assertEquals(from, suppliedFrom)
            assertEquals(to, suppliedTo)
            2.5f
        }

        assertEquals(2.5f, settings.tools.pathCost?.invoke(world, from, to))
    }

    @Test
    fun `debug entity speed multiplier must be finite and positive`() {
        val settings = DebugSettings()

        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                settings.tools.pathfinding.entitySpeedMultiplier = invalid
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

        assertFalse(settings.ui.settingsWindow.visibleOnStartup)
        assertFalse(settings.ui.settingsWindow.isVisible)

        settings.ui.settingsWindow {
            enabled = true
            visibleOnStartup = true
        }

        assertTrue(settings.ui.settingsWindow.visibleOnStartup)

        // Runtime visibility is not changed by configuration alone.
        assertFalse(settings.ui.settingsWindow.isVisible)
    }

    @Test
    fun `enabled windows require independent shortcuts`() {
        val settings = DebugSettings().apply {
            ui.toolRail.apply { enabled = true; toggleKey = Input.Keys.F4 }
            ui.settingsWindow.apply { enabled = true; toggleKey = Input.Keys.F4 }
        }

        assertFailsWith<IllegalArgumentException> {
            settings.validateWindowConfiguration()
        }
    }

    private data object TestTile : Tile
}
