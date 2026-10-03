package com.mefabc24.strata.debug

import com.badlogic.gdx.graphics.Color

/** Stable visual sections used by category reset controls and custom presets. */
enum class DebugVisualCategory {
    GENERAL,
    PERFORMANCE,
    GRID,
    WORLD_INFORMATION,
    WORLD_VISIBILITY,
    OBJECTS,
    ENTITIES,
    PICKING,
    RENDER_ORDER,
    CULLING,
    CAMERA,
    WORLD_STATS,
    EVENT_MONITOR,
    NOTIFICATIONS
}

/** Versioned, engine-owned snapshot of visual debug settings. */
class DebugVisualConfiguration internal constructor(
    val version: Int,
    internal val values: Map<String, Any?>
)

internal object DebugVisualSettings {
    private const val VERSION = 2

    private interface Binding {
        val key: String
        val category: DebugVisualCategory
        fun read(settings: DebugSettings): Any?
        fun write(settings: DebugSettings, value: Any?)
    }

    private class TypedBinding<T>(
        override val key: String,
        override val category: DebugVisualCategory,
        private val readValue: (DebugSettings) -> T,
        private val writeValue: (DebugSettings, T) -> Unit,
        private val copyValue: (T) -> T = { it }
    ) : Binding {
        override fun read(settings: DebugSettings): Any? = copyValue(readValue(settings))

        @Suppress("UNCHECKED_CAST")
        override fun write(settings: DebugSettings, value: Any?) {
            writeValue(settings, copyValue(value as T))
        }
    }

    private fun <T> binding(
        key: String,
        category: DebugVisualCategory,
        read: (DebugSettings) -> T,
        write: (DebugSettings, T) -> Unit,
        copy: (T) -> T = { it }
    ): Binding = TypedBinding(key, category, read, write, copy)

    private val colorCopy: (Color) -> Color = { it.cpy() }
    private val nullableColorCopy: (Color?) -> Color? = { it?.cpy() }
    private val setCopy: (Set<String>) -> Set<String> = { it.toSet() }

    private val bindings = listOf(
        binding("filter", DebugVisualCategory.GENERAL, { it.visualizationFilter }, { s, v -> s.visualizationFilter = v }),
        binding("performance.overlay", DebugVisualCategory.GENERAL, { it.performance.overlayEnabled }, { s, v -> s.performance.overlayEnabled = v }),
        binding("grid.enabled", DebugVisualCategory.GRID, { it.grid.enabled }, { s, v -> s.grid.enabled = v }),
        binding("grid.layer", DebugVisualCategory.GRID, { it.grid.renderLayer }, { s, v -> s.grid.renderLayer = v }),
        binding("grid.extent", DebugVisualCategory.GRID, { it.grid.extent }, { s, v -> s.grid.extent = v }),
        binding("grid.color", DebugVisualCategory.GRID, { it.grid.color }, { s, v -> s.grid.color = v }, colorCopy),
        binding("grid.hoverColor", DebugVisualCategory.GRID, { it.grid.hoverColor }, { s, v -> s.grid.hoverColor = v }, colorCopy),
        binding("grid.lineWidth", DebugVisualCategory.GRID, { it.grid.lineWidth }, { s, v -> s.grid.lineWidth = v }),
        binding("grid.background", DebugVisualCategory.GRID, { it.grid.backgroundColor }, { s, v -> s.grid.backgroundColor = v }, nullableColorCopy),
        binding("grid.hoverBackground", DebugVisualCategory.GRID, { it.grid.hoverBackgroundColor }, { s, v -> s.grid.hoverBackgroundColor = v }, nullableColorCopy),
        binding("worldInfo.coordinates", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.showTileCoordinates }, { s, v -> s.worldInfo.showTileCoordinates = v }),
        binding("worldInfo.terrainIds", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.showTerrainIds }, { s, v -> s.worldInfo.showTerrainIds = v }),
        binding("worldInfo.overlays", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.showOverlayInfo }, { s, v -> s.worldInfo.showOverlayInfo = v }),
        binding("worldInfo.occupancy", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.showOccupancy }, { s, v -> s.worldInfo.showOccupancy = v }),
        binding("worldInfo.missing", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.showMissingTerrainVisuals }, { s, v -> s.worldInfo.showMissingTerrainVisuals = v }),
        binding("worldInfo.origin", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.showOrigin }, { s, v -> s.worldInfo.showOrigin = v }),
        binding("worldInfo.zoom", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.maximumLabelZoom }, { s, v -> s.worldInfo.maximumLabelZoom = v }),
        binding("worldInfo.labels", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.maximumVisibleLabels }, { s, v -> s.worldInfo.maximumVisibleLabels = v }),
        binding("worldInfo.labelColor", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.labelColor }, { s, v -> s.worldInfo.labelColor = v }, colorCopy),
        binding("worldInfo.occupancyColor", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.occupancyColor }, { s, v -> s.worldInfo.occupancyColor = v }, colorCopy),
        binding("worldInfo.missingColor", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.missingVisualColor }, { s, v -> s.worldInfo.missingVisualColor = v }, colorCopy),
        binding("worldInfo.originColor", DebugVisualCategory.WORLD_INFORMATION, { it.worldInfo.originColor }, { s, v -> s.worldInfo.originColor = v }, colorCopy),
        binding("visibility.ground", DebugVisualCategory.WORLD_VISIBILITY, { it.worldVisibility.groundTerrainVisible }, { s, v -> s.worldVisibility.groundTerrainVisible = v }),
        binding("visibility.overlays", DebugVisualCategory.WORLD_VISIBILITY, { it.worldVisibility.terrainOverlaysVisible }, { s, v -> s.worldVisibility.terrainOverlaysVisible = v }),
        binding("visibility.objects", DebugVisualCategory.WORLD_VISIBILITY, { it.worldVisibility.placedObjectsVisible }, { s, v -> s.worldVisibility.placedObjectsVisible = v }),
        binding("visibility.entities", DebugVisualCategory.WORLD_VISIBILITY, { it.worldVisibility.entitiesVisible }, { s, v -> s.worldVisibility.entitiesVisible = v }),
        binding("visibility.layers", DebugVisualCategory.WORLD_VISIBILITY, { it.worldVisibility.hiddenOverlayLayerIds() }, { s, v -> s.worldVisibility.restoreHiddenOverlayLayers(v) }, setCopy),
        binding("objects.tiles", DebugVisualCategory.OBJECTS, { it.objects.showOccupiedTiles }, { s, v -> s.objects.showOccupiedTiles = v }),
        binding("objects.origin", DebugVisualCategory.OBJECTS, { it.objects.showOriginTile }, { s, v -> s.objects.showOriginTile = v }),
        binding("objects.bounds", DebugVisualCategory.OBJECTS, { it.objects.showSpriteBounds }, { s, v -> s.objects.showSpriteBounds = v }),
        binding("objects.width", DebugVisualCategory.OBJECTS, { it.objects.lineWidth }, { s, v -> s.objects.lineWidth = v }),
        binding("objects.tileColor", DebugVisualCategory.OBJECTS, { it.objects.occupiedTileColor }, { s, v -> s.objects.occupiedTileColor = v }, colorCopy),
        binding("objects.fill", DebugVisualCategory.OBJECTS, { it.objects.occupiedTileFillColor }, { s, v -> s.objects.occupiedTileFillColor = v }, nullableColorCopy),
        binding("objects.originColor", DebugVisualCategory.OBJECTS, { it.objects.originTileColor }, { s, v -> s.objects.originTileColor = v }, colorCopy),
        binding("objects.boundsColor", DebugVisualCategory.OBJECTS, { it.objects.spriteBoundsColor }, { s, v -> s.objects.spriteBoundsColor = v }, colorCopy),
        binding("entities.tile", DebugVisualCategory.ENTITIES, { it.entities.showCurrentTile }, { s, v -> s.entities.showCurrentTile = v }),
        binding("entities.position", DebugVisualCategory.ENTITIES, { it.entities.showPosition }, { s, v -> s.entities.showPosition = v }),
        binding("entities.path", DebugVisualCategory.ENTITIES, { it.entities.showPath }, { s, v -> s.entities.showPath = v }),
        binding("entities.direction", DebugVisualCategory.ENTITIES, { it.entities.showDirection }, { s, v -> s.entities.showDirection = v }),
        binding("entities.bounds", DebugVisualCategory.ENTITIES, { it.entities.showSpriteBounds }, { s, v -> s.entities.showSpriteBounds = v }),
        binding("entities.trail", DebugVisualCategory.ENTITIES, { it.entities.showMovementTrail }, { s, v -> s.entities.showMovementTrail = v }),
        binding("entities.vector", DebugVisualCategory.ENTITIES, { it.entities.showMovementVector }, { s, v -> s.entities.showMovementVector = v }),
        binding("entities.waypoint", DebugVisualCategory.ENTITIES, { it.entities.showNextWaypoint }, { s, v -> s.entities.showNextWaypoint = v }),
        binding("entities.speed", DebugVisualCategory.ENTITIES, { it.entities.showMovementSpeed }, { s, v -> s.entities.showMovementSpeed = v }),
        binding("entities.offset", DebugVisualCategory.ENTITIES, { it.entities.showPositionTileOffset }, { s, v -> s.entities.showPositionTileOffset = v }),
        binding("entities.trailPositions", DebugVisualCategory.ENTITIES, { it.entities.trailMaxPositions }, { s, v -> s.entities.trailMaxPositions = v }),
        binding("entities.trailSeconds", DebugVisualCategory.ENTITIES, { it.entities.trailHistoryDurationSeconds }, { s, v -> s.entities.trailHistoryDurationSeconds = v }),
        binding("entities.trailDistance", DebugVisualCategory.ENTITIES, { it.entities.trailMinimumDistance }, { s, v -> s.entities.trailMinimumDistance = v }),
        binding("entities.trailOpacity", DebugVisualCategory.ENTITIES, { it.entities.trailOpacity }, { s, v -> s.entities.trailOpacity = v }),
        binding("entities.vectorScale", DebugVisualCategory.ENTITIES, { it.entities.movementVectorScaleSeconds }, { s, v -> s.entities.movementVectorScaleSeconds = v }),
        binding("entities.width", DebugVisualCategory.ENTITIES, { it.entities.lineWidth }, { s, v -> s.entities.lineWidth = v }),
        binding("entities.tileColor", DebugVisualCategory.ENTITIES, { it.entities.currentTileColor }, { s, v -> s.entities.currentTileColor = v }, colorCopy),
        binding("entities.fill", DebugVisualCategory.ENTITIES, { it.entities.currentTileFillColor }, { s, v -> s.entities.currentTileFillColor = v }, nullableColorCopy),
        binding("entities.positionColor", DebugVisualCategory.ENTITIES, { it.entities.positionColor }, { s, v -> s.entities.positionColor = v }, colorCopy),
        binding("entities.pathColor", DebugVisualCategory.ENTITIES, { it.entities.pathColor }, { s, v -> s.entities.pathColor = v }, colorCopy),
        binding("entities.directionColor", DebugVisualCategory.ENTITIES, { it.entities.directionColor }, { s, v -> s.entities.directionColor = v }, colorCopy),
        binding("entities.trailColor", DebugVisualCategory.ENTITIES, { it.entities.trailColor }, { s, v -> s.entities.trailColor = v }, colorCopy),
        binding("entities.vectorColor", DebugVisualCategory.ENTITIES, { it.entities.movementVectorColor }, { s, v -> s.entities.movementVectorColor = v }, colorCopy),
        binding("entities.waypointColor", DebugVisualCategory.ENTITIES, { it.entities.nextWaypointColor }, { s, v -> s.entities.nextWaypointColor = v }, colorCopy),
        binding("entities.offsetColor", DebugVisualCategory.ENTITIES, { it.entities.positionTileOffsetColor }, { s, v -> s.entities.positionTileOffsetColor = v }, colorCopy),
        binding("entities.boundsColor", DebugVisualCategory.ENTITIES, { it.entities.spriteBoundsColor }, { s, v -> s.entities.spriteBoundsColor = v }, colorCopy),
        binding("picking.bounds", DebugVisualCategory.PICKING, { it.picking.showSpriteBounds }, { s, v -> s.picking.showSpriteBounds = v }),
        binding("picking.cursor", DebugVisualCategory.PICKING, { it.picking.showCursorHit }, { s, v -> s.picking.showCursorHit = v }),
        binding("render.mode", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.mode }, { s, v -> s.renderOrder.mode = v }),
        binding("render.labels", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.showLabels }, { s, v -> s.renderOrder.showLabels = v }),
        binding("render.priorityLabels", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.showPriorityLabels }, { s, v -> s.renderOrder.showPriorityLabels = v }),
        binding("render.priorityColors", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.colorByPriority }, { s, v -> s.renderOrder.colorByPriority = v }),
        binding("render.focus", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.priorityFocusMode }, { s, v -> s.renderOrder.priorityFocusMode = v }),
        binding("render.priority", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.selectedPriority }, { s, v -> s.renderOrder.selectedPriority = v }),
        binding("render.volumes", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.showSortVolumes }, { s, v -> s.renderOrder.showSortVolumes = v }),
        binding("render.anchors", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.showSortAnchors }, { s, v -> s.renderOrder.showSortAnchors = v }),
        binding("render.projected", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.showProjectedSortPositions }, { s, v -> s.renderOrder.showProjectedSortPositions = v }),
        binding("render.terrainIndices", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.showTerrainIndices }, { s, v -> s.renderOrder.showTerrainIndices = v }),
        binding("render.heatmap", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.showTerrainHeatmap }, { s, v -> s.renderOrder.showTerrainHeatmap = v }),
        binding("render.heatmapSteps", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.terrainHeatmapSteps }, { s, v -> s.renderOrder.terrainHeatmapSteps = v }),
        binding("render.alpha", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.priorityColorAlpha }, { s, v -> s.renderOrder.priorityColorAlpha = v }),
        binding("render.width", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.sortGeometryLineWidth }, { s, v -> s.renderOrder.sortGeometryLineWidth = v }),
        binding("render.highlightColor", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.priorityHighlightColor }, { s, v -> s.renderOrder.priorityHighlightColor = v }, colorCopy),
        binding("render.volumeColor", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.sortVolumeColor }, { s, v -> s.renderOrder.sortVolumeColor = v }, colorCopy),
        binding("render.backColor", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.sortBackAnchorColor }, { s, v -> s.renderOrder.sortBackAnchorColor = v }, colorCopy),
        binding("render.frontColor", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.sortFrontAnchorColor }, { s, v -> s.renderOrder.sortFrontAnchorColor = v }, colorCopy),
        binding("render.projectedColor", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.projectedSortPositionColor }, { s, v -> s.renderOrder.projectedSortPositionColor = v }, colorCopy),
        binding("render.heatmapStart", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.terrainHeatmapStartColor }, { s, v -> s.renderOrder.terrainHeatmapStartColor = v }, colorCopy),
        binding("render.heatmapEnd", DebugVisualCategory.RENDER_ORDER, { it.renderOrder.terrainHeatmapEndColor }, { s, v -> s.renderOrder.terrainHeatmapEndColor = v }, colorCopy),
        binding("culling.area", DebugVisualCategory.CULLING, { it.culling.showVisibleArea }, { s, v -> s.culling.showVisibleArea = v }),
        binding("culling.objects", DebugVisualCategory.CULLING, { it.culling.showObjectBounds }, { s, v -> s.culling.showObjectBounds = v }),
        binding("culling.entities", DebugVisualCategory.CULLING, { it.culling.showEntityBounds }, { s, v -> s.culling.showEntityBounds = v }),
        binding("culling.areaColor", DebugVisualCategory.CULLING, { it.culling.visibleAreaColor }, { s, v -> s.culling.visibleAreaColor = v }, colorCopy),
        binding("culling.objectDrawnColor", DebugVisualCategory.CULLING, { it.culling.objectDrawnColor }, { s, v -> s.culling.objectDrawnColor = v }, colorCopy),
        binding("culling.objectCulledColor", DebugVisualCategory.CULLING, { it.culling.objectCulledColor }, { s, v -> s.culling.objectCulledColor = v }, colorCopy),
        binding("culling.entityDrawnColor", DebugVisualCategory.CULLING, { it.culling.entityDrawnColor }, { s, v -> s.culling.entityDrawnColor = v }, colorCopy),
        binding("culling.entityCulledColor", DebugVisualCategory.CULLING, { it.culling.entityCulledColor }, { s, v -> s.culling.entityCulledColor = v }, colorCopy),
        binding("camera.area", DebugVisualCategory.CAMERA, { it.camera.showVisibleArea }, { s, v -> s.camera.showVisibleArea = v }),
        binding("camera.world", DebugVisualCategory.CAMERA, { it.camera.showWorldBounds }, { s, v -> s.camera.showWorldBounds = v }),
        binding("camera.clamp", DebugVisualCategory.CAMERA, { it.camera.showClampBounds }, { s, v -> s.camera.showClampBounds = v }),
        binding("worldStats.enabled", DebugVisualCategory.GENERAL, { it.worldStats.enabled }, { s, v -> s.worldStats.enabled = v }),
        binding("events.enabled", DebugVisualCategory.EVENT_MONITOR, { it.eventBus.enabled }, { s, v -> s.eventBus.enabled = v }),
        binding("events.records", DebugVisualCategory.EVENT_MONITOR, { it.eventBus.maximumVisibleRecords }, { s, v -> s.eventBus.maximumVisibleRecords = v }),
        binding("events.order", DebugVisualCategory.EVENT_MONITOR, { it.eventBus.newestFirst }, { s, v -> s.eventBus.newestFirst = v }),
        binding("notifications.enabled", DebugVisualCategory.NOTIFICATIONS, { it.notifications.enabled }, { s, v -> s.notifications.enabled = v }),
        binding("notifications.position", DebugVisualCategory.NOTIFICATIONS, { it.notifications.position }, { s, v -> s.notifications.position = v })
    )

    fun capture(settings: DebugSettings): DebugVisualConfiguration = DebugVisualConfiguration(
        VERSION,
        bindings.associate { it.key to it.read(settings) }
    )

    fun apply(settings: DebugSettings, configuration: DebugVisualConfiguration) {
        require(configuration.version in 1..VERSION) {
            "Unsupported debug visual configuration version ${configuration.version}."
        }
        bindings.forEach { binding ->
            if (configuration.values.containsKey(binding.key)) {
                binding.write(settings, configuration.values[binding.key])
            }
        }
        if (configuration.version == 1) migrateLegacyFeatureGates(settings, configuration)
    }

    fun reset(settings: DebugSettings, category: DebugVisualCategory? = null) {
        val defaults = DebugSettings()
        bindings.asSequence()
            .filter { category == null || it.category == category }
            .forEach { it.write(settings, it.read(defaults)) }
    }

    private fun migrateLegacyFeatureGates(
        settings: DebugSettings,
        configuration: DebugVisualConfiguration
    ) {
        fun disabled(key: String): Boolean = configuration.values[key] == false

        if (disabled("worldInfo.enabled")) {
            settings.worldInfo.showTileCoordinates = false
            settings.worldInfo.showTerrainIds = false
            settings.worldInfo.showOverlayInfo = false
            settings.worldInfo.showOccupancy = false
            settings.worldInfo.showMissingTerrainVisuals = false
            settings.worldInfo.showOrigin = false
        }
        if (disabled("objects.enabled")) {
            settings.objects.showOccupiedTiles = false
            settings.objects.showOriginTile = false
            settings.objects.showSpriteBounds = false
            settings.objects.occupiedTileFillColor = null
        }
        if (disabled("entities.enabled")) {
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
        }
        if (disabled("picking.enabled")) {
            settings.picking.showSpriteBounds = false
            settings.picking.showCursorHit = false
        }
        if (disabled("render.enabled")) {
            settings.renderOrder.showLabels = false
            settings.renderOrder.showPriorityLabels = false
            settings.renderOrder.colorByPriority = false
            settings.renderOrder.priorityFocusMode = RenderPriorityFocusMode.OFF
            settings.renderOrder.showSortVolumes = false
            settings.renderOrder.showSortAnchors = false
            settings.renderOrder.showProjectedSortPositions = false
            settings.renderOrder.showTerrainIndices = false
            settings.renderOrder.showTerrainHeatmap = false
        }
        if (disabled("culling.enabled")) {
            settings.culling.showVisibleArea = false
            settings.culling.showObjectBounds = false
            settings.culling.showEntityBounds = false
        }
        if (disabled("camera.enabled")) {
            settings.camera.showVisibleArea = false
            settings.camera.showWorldBounds = false
            settings.camera.showClampBounds = false
        }
    }
}
