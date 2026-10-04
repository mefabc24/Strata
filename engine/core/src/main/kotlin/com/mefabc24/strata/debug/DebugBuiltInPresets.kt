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
                settings.performance.overlayEnabled = true
                settings.grid.enabled = true
            }
            DebugPreset.PLACEMENT -> {
                settings.grid.enabled = true
                settings.objects.showOccupiedTiles = true
                settings.objects.showOriginTile = true
                settings.objects.occupiedTileFillColor = Color(0.2f, 0.65f, 1f, 0.18f)
            }
            DebugPreset.ENTITIES -> {
                settings.entities.showCurrentTile = true
                settings.entities.showPosition = true
                settings.entities.showPath = true
                settings.entities.showDirection = true
                settings.entities.showSpriteBounds = true
                settings.entities.currentTileFillColor = Color(0.3f, 1f, 0.3f, 0.16f)
            }
            DebugPreset.RENDERING -> {
                settings.performance.overlayEnabled = true
                settings.objects.showSpriteBounds = true
                settings.entities.showSpriteBounds = true
                settings.renderOrder.showLabels = true
                settings.culling.showVisibleArea = true
                settings.culling.showObjectBounds = true
                settings.culling.showEntityBounds = true
                settings.camera.showVisibleArea = true
                settings.camera.showWorldBounds = true
                settings.camera.showClampBounds = true
            }
            DebugPreset.EVERYTHING -> {
                settings.performance.overlayEnabled = true
                settings.simulation.enabled = true
                settings.grid.enabled = true
                settings.grid.backgroundColor = Color(1f, 1f, 1f, 0.2f)
                settings.grid.hoverBackgroundColor = Color(1f, 0f, 0f, 0.5f)
                settings.worldInfo.showTileCoordinates = true
                settings.worldInfo.showTerrainIds = true
                settings.worldInfo.showOverlayInfo = true
                settings.worldInfo.showOccupancy = true
                settings.worldInfo.showMissingTerrainVisuals = true
                settings.objects.showOccupiedTiles = true
                settings.objects.showOriginTile = true
                settings.objects.showSpriteBounds = true
                settings.objects.occupiedTileFillColor = Color(0.2f, 0.65f, 1f, 0.18f)
                settings.entities.showCurrentTile = true
                settings.entities.showPosition = true
                settings.entities.showPath = true
                settings.entities.showDirection = true
                settings.entities.showSpriteBounds = true
                settings.entities.currentTileFillColor = Color(0.3f, 1f, 0.3f, 0.16f)
                settings.picking.showSpriteBounds = true
                settings.picking.showCursorHit = true
                settings.renderOrder.showLabels = true
                settings.culling.showVisibleArea = true
                settings.culling.showObjectBounds = true
                settings.culling.showEntityBounds = true
                settings.camera.showVisibleArea = true
                settings.camera.showWorldBounds = true
                settings.camera.showClampBounds = true
                settings.worldStats.enabled = true
            }
        }
    }

    private fun disableVisuals(settings: DebugSettings) {
        settings.performance.overlayEnabled = false
        settings.simulation.enabled = false
        settings.grid.enabled = false
        settings.worldInfo.showTileCoordinates = false
        settings.worldInfo.showTerrainIds = false
        settings.worldInfo.showOverlayInfo = false
        settings.worldInfo.showOccupancy = false
        settings.worldInfo.showMissingTerrainVisuals = false
        settings.worldInfo.showOrigin = false
        settings.worldVisibility.showAll()
        settings.objects.showOccupiedTiles = false
        settings.objects.showOriginTile = false
        settings.objects.showSpriteBounds = false
        settings.objects.occupiedTileFillColor = null
        settings.entities.showCurrentTile = false
        settings.entities.showPosition = false
        settings.entities.showPath = false
        settings.entities.showDirection = false
        settings.entities.showSpriteBounds = false
        settings.entities.showMovementTrail = false
        settings.entities.showMovementVector = false
        settings.entities.showNextWaypoint = false
        settings.entities.showMovementSpeed = false
        settings.entities.showPositionTileOffset = false
        settings.entities.currentTileFillColor = null
        settings.picking.showSpriteBounds = false
        settings.picking.showCursorHit = false
        settings.renderOrder.showLabels = false
        settings.renderOrder.showPriorityLabels = false
        settings.renderOrder.colorByPriority = false
        settings.renderOrder.priorityFocusMode = RenderPriorityFocusMode.OFF
        settings.renderOrder.showSortVolumes = false
        settings.renderOrder.showSortAnchors = false
        settings.renderOrder.showProjectedSortPositions = false
        settings.renderOrder.showTerrainIndices = false
        settings.renderOrder.showTerrainHeatmap = false
        settings.culling.showVisibleArea = false
        settings.culling.showObjectBounds = false
        settings.culling.showEntityBounds = false
        settings.camera.showVisibleArea = false
        settings.camera.showWorldBounds = false
        settings.camera.showClampBounds = false
        settings.worldStats.enabled = false
    }
}
