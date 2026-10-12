package com.mefabc24.strata.debug

import com.badlogic.gdx.graphics.Color

/** Visual diagnostic presets supplied by the engine. */
enum class DebugPreset { OFF, MINIMAL, PLACEMENT, ENTITIES, RENDERING, EVERYTHING }

internal object DebugPresets {
    fun apply(settings: DebugSettings, preset: DebugPreset) {
        disableVisuals(settings)
        when (preset) {
            DebugPreset.OFF -> Unit
            DebugPreset.MINIMAL -> {
                settings.operations.performance.overlayEnabled = true
                settings.visuals.grid.enabled = true
            }
            DebugPreset.PLACEMENT -> {
                settings.visuals.grid.enabled = true
                settings.visuals.objects.showOccupiedTiles = true
                settings.visuals.objects.showOriginTile = true
                settings.visuals.objects.showOccupiedTileFill = true
                settings.visuals.objects.occupiedTileFillColor = Color(0.2f, 0.65f, 1f, 0.18f)
            }
            DebugPreset.ENTITIES -> {
                settings.visuals.entities.showCurrentTile = true
                settings.visuals.entities.showPosition = true
                settings.visuals.entities.showPath = true
                settings.visuals.entities.showDirection = true
                settings.visuals.entities.showSpriteBounds = true
                settings.visuals.entities.showCurrentTileFill = true
                settings.visuals.entities.currentTileFillColor = Color(0.3f, 1f, 0.3f, 0.16f)
            }
            DebugPreset.RENDERING -> {
                settings.operations.performance.overlayEnabled = true
                settings.visuals.objects.showSpriteBounds = true
                settings.visuals.entities.showSpriteBounds = true
                settings.visuals.renderOrder.showLabels = true
                settings.visuals.culling.showVisibleArea = true
                settings.visuals.culling.showObjectBounds = true
                settings.visuals.culling.showEntityBounds = true
                settings.visuals.camera.showVisibleArea = true
                settings.visuals.camera.showWorldBounds = true
                settings.visuals.camera.showClampBounds = true
            }
            DebugPreset.EVERYTHING -> {
                settings.operations.performance.overlayEnabled = true
                settings.operations.performance.historyOverlayEnabled = true
                settings.operations.simulation.enabled = true
                settings.visuals.grid.enabled = true
                settings.visuals.grid.showBackground = true
                settings.visuals.grid.showHoverBackground = true
                settings.visuals.grid.backgroundColor = Color(1f, 1f, 1f, 0.2f)
                settings.visuals.grid.hoverBackgroundColor = Color(1f, 0f, 0f, 0.5f)
                settings.visuals.worldInfo.showTileCoordinates = true
                settings.visuals.worldInfo.showTerrainIds = true
                settings.visuals.worldInfo.showOverlayInfo = true
                settings.visuals.worldInfo.showOccupancy = true
                settings.visuals.worldInfo.showMissingTerrainVisuals = true
                settings.visuals.objects.showOccupiedTiles = true
                settings.visuals.objects.showOriginTile = true
                settings.visuals.objects.showSpriteBounds = true
                settings.visuals.objects.showOccupiedTileFill = true
                settings.visuals.objects.occupiedTileFillColor = Color(0.2f, 0.65f, 1f, 0.18f)
                settings.visuals.entities.showCurrentTile = true
                settings.visuals.entities.showPosition = true
                settings.visuals.entities.showPath = true
                settings.visuals.entities.showDirection = true
                settings.visuals.entities.showSpriteBounds = true
                settings.visuals.entities.currentTileFillColor = Color(0.3f, 1f, 0.3f, 0.16f)
                settings.visuals.entities.showCurrentTileFill = true
                settings.visuals.picking.enabled = true
                settings.visuals.picking.showSpriteBounds = true
                settings.visuals.picking.showCursorHit = true
                settings.visuals.renderOrder.showLabels = true
                settings.visuals.culling.showVisibleArea = true
                settings.visuals.culling.showObjectBounds = true
                settings.visuals.culling.showEntityBounds = true
                settings.visuals.camera.showVisibleArea = true
                settings.visuals.camera.showWorldBounds = true
                settings.visuals.camera.showClampBounds = true
                settings.operations.worldStats.enabled = true
            }
        }
    }

    private fun disableVisuals(settings: DebugSettings) {
        settings.operations.performance.overlayEnabled = false
        settings.operations.performance.historyOverlayEnabled = false
        settings.operations.simulation.enabled = false
        settings.visuals.grid.enabled = false
        settings.visuals.grid.showBackground = false
        settings.visuals.grid.showHoverBackground = false
        settings.visuals.worldInfo.showTileCoordinates = false
        settings.visuals.worldInfo.showTerrainIds = false
        settings.visuals.worldInfo.showOverlayInfo = false
        settings.visuals.worldInfo.showOccupancy = false
        settings.visuals.worldInfo.showMissingTerrainVisuals = false
        settings.visuals.worldInfo.showOrigin = false
        settings.visuals.worldVisibility.showAll()
        settings.visuals.objects.showOccupiedTiles = false
        settings.visuals.objects.showOriginTile = false
        settings.visuals.objects.showSpriteBounds = false
        settings.visuals.objects.showOccupiedTileFill = false
        settings.visuals.entities.showCurrentTile = false
        settings.visuals.entities.showPosition = false
        settings.visuals.entities.showPath = false
        settings.visuals.entities.showDirection = false
        settings.visuals.entities.showSpriteBounds = false
        settings.visuals.entities.showMovementTrail = false
        settings.visuals.entities.showMovementVector = false
        settings.visuals.entities.showNextWaypoint = false
        settings.visuals.entities.showMovementSpeed = false
        settings.visuals.entities.showPositionTileOffset = false
        settings.visuals.entities.showCurrentTileFill = false
        settings.visuals.picking.enabled = false
        settings.visuals.picking.showSpriteBounds = false
        settings.visuals.picking.showCursorHit = false
        settings.visuals.renderOrder.showLabels = false
        settings.visuals.renderOrder.showPriorityLabels = false
        settings.visuals.renderOrder.colorByPriority = false
        settings.visuals.renderOrder.priorityFocusMode = RenderPriorityFocusMode.OFF
        settings.visuals.renderOrder.showSortVolumes = false
        settings.visuals.renderOrder.showSortAnchors = false
        settings.visuals.renderOrder.showProjectedSortPositions = false
        settings.visuals.renderOrder.showTerrainIndices = false
        settings.visuals.renderOrder.showTerrainHeatmap = false
        settings.visuals.culling.showVisibleArea = false
        settings.visuals.culling.showObjectBounds = false
        settings.visuals.culling.showEntityBounds = false
        settings.visuals.camera.showVisibleArea = false
        settings.visuals.camera.showWorldBounds = false
        settings.visuals.camera.showClampBounds = false
        settings.operations.worldStats.enabled = false
    }
}
