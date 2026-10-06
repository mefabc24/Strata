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

internal object DebugVisualConfigurationBindings {
    internal const val VERSION = 6

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
    private val setCopy: (Set<String>) -> Set<String> = { it.toSet() }

    private val bindings = listOf(
        binding("filter", DebugVisualCategory.GENERAL, { it.visuals.filter }, { s, v -> s.visuals.filter = v }),
        binding("performance.overlay", DebugVisualCategory.GENERAL, { it.operations.performance.overlayEnabled }, { s, v -> s.operations.performance.overlayEnabled = v }),
        binding("performance.historyOverlay", DebugVisualCategory.GENERAL, { it.operations.performance.historyOverlayEnabled }, { s, v -> s.operations.performance.historyOverlayEnabled = v }),
        binding("performance.historyMetric", DebugVisualCategory.GENERAL, { it.operations.performance.historyMetric }, { s, v -> s.operations.performance.historyMetric = v }),
        binding("performance.overlayInterval", DebugVisualCategory.GENERAL, { it.operations.performance.overlayRefreshIntervalSeconds }, { s, v -> s.operations.performance.overlayRefreshIntervalSeconds = v }),
        binding("grid.enabled", DebugVisualCategory.GRID, { it.visuals.grid.enabled }, { s, v -> s.visuals.grid.enabled = v }),
        binding("grid.layer", DebugVisualCategory.GRID, { it.visuals.grid.renderLayer }, { s, v -> s.visuals.grid.renderLayer = v }),
        binding("grid.extent", DebugVisualCategory.GRID, { it.visuals.grid.extent }, { s, v -> s.visuals.grid.extent = v }),
        binding("grid.color", DebugVisualCategory.GRID, { it.visuals.grid.color }, { s, v -> s.visuals.grid.color = v }, colorCopy),
        binding("grid.hoverColor", DebugVisualCategory.GRID, { it.visuals.grid.hoverColor }, { s, v -> s.visuals.grid.hoverColor = v }, colorCopy),
        binding("grid.lineWidth", DebugVisualCategory.GRID, { it.visuals.grid.lineWidth }, { s, v -> s.visuals.grid.lineWidth = v }),
        binding("grid.backgroundEnabled", DebugVisualCategory.GRID, { it.visuals.grid.showBackground }, { s, v -> s.visuals.grid.showBackground = v }),
        binding("grid.background", DebugVisualCategory.GRID, { it.visuals.grid.backgroundColor }, { s, v -> s.visuals.grid.backgroundColor = v }, colorCopy),
        binding("grid.hoverBackgroundEnabled", DebugVisualCategory.GRID, { it.visuals.grid.showHoverBackground }, { s, v -> s.visuals.grid.showHoverBackground = v }),
        binding("grid.hoverBackground", DebugVisualCategory.GRID, { it.visuals.grid.hoverBackgroundColor }, { s, v -> s.visuals.grid.hoverBackgroundColor = v }, colorCopy),
        binding("worldInfo.coordinates", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.showTileCoordinates }, { s, v -> s.visuals.worldInfo.showTileCoordinates = v }),
        binding("worldInfo.terrainIds", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.showTerrainIds }, { s, v -> s.visuals.worldInfo.showTerrainIds = v }),
        binding("worldInfo.overlays", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.showOverlayInfo }, { s, v -> s.visuals.worldInfo.showOverlayInfo = v }),
        binding("worldInfo.occupancy", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.showOccupancy }, { s, v -> s.visuals.worldInfo.showOccupancy = v }),
        binding("worldInfo.missing", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.showMissingTerrainVisuals }, { s, v -> s.visuals.worldInfo.showMissingTerrainVisuals = v }),
        binding("worldInfo.origin", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.showOrigin }, { s, v -> s.visuals.worldInfo.showOrigin = v }),
        binding("worldInfo.zoom", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.maximumLabelZoom }, { s, v -> s.visuals.worldInfo.maximumLabelZoom = v }),
        binding("worldInfo.labels", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.maximumVisibleLabels }, { s, v -> s.visuals.worldInfo.maximumVisibleLabels = v }),
        binding("worldInfo.labelColor", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.labelColor }, { s, v -> s.visuals.worldInfo.labelColor = v }, colorCopy),
        binding("worldInfo.occupancyColor", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.occupancyColor }, { s, v -> s.visuals.worldInfo.occupancyColor = v }, colorCopy),
        binding("worldInfo.missingColor", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.missingVisualColor }, { s, v -> s.visuals.worldInfo.missingVisualColor = v }, colorCopy),
        binding("worldInfo.originColor", DebugVisualCategory.WORLD_INFORMATION, { it.visuals.worldInfo.originColor }, { s, v -> s.visuals.worldInfo.originColor = v }, colorCopy),
        binding("visibility.ground", DebugVisualCategory.WORLD_VISIBILITY, { it.visuals.worldVisibility.groundTerrainVisible }, { s, v -> s.visuals.worldVisibility.groundTerrainVisible = v }),
        binding("visibility.overlays", DebugVisualCategory.WORLD_VISIBILITY, { it.visuals.worldVisibility.terrainOverlaysVisible }, { s, v -> s.visuals.worldVisibility.terrainOverlaysVisible = v }),
        binding("visibility.objects", DebugVisualCategory.WORLD_VISIBILITY, { it.visuals.worldVisibility.placedObjectsVisible }, { s, v -> s.visuals.worldVisibility.placedObjectsVisible = v }),
        binding("visibility.entities", DebugVisualCategory.WORLD_VISIBILITY, { it.visuals.worldVisibility.entitiesVisible }, { s, v -> s.visuals.worldVisibility.entitiesVisible = v }),
        binding("visibility.layers", DebugVisualCategory.WORLD_VISIBILITY, { it.visuals.worldVisibility.hiddenOverlayLayerIds() }, { s, v -> s.visuals.worldVisibility.restoreHiddenOverlayLayers(v) }, setCopy),
        binding("objects.tiles", DebugVisualCategory.OBJECTS, { it.visuals.objects.showOccupiedTiles }, { s, v -> s.visuals.objects.showOccupiedTiles = v }),
        binding("objects.origin", DebugVisualCategory.OBJECTS, { it.visuals.objects.showOriginTile }, { s, v -> s.visuals.objects.showOriginTile = v }),
        binding("objects.bounds", DebugVisualCategory.OBJECTS, { it.visuals.objects.showSpriteBounds }, { s, v -> s.visuals.objects.showSpriteBounds = v }),
        binding("objects.width", DebugVisualCategory.OBJECTS, { it.visuals.objects.lineWidth }, { s, v -> s.visuals.objects.lineWidth = v }),
        binding("objects.tileColor", DebugVisualCategory.OBJECTS, { it.visuals.objects.occupiedTileColor }, { s, v -> s.visuals.objects.occupiedTileColor = v }, colorCopy),
        binding("objects.fillEnabled", DebugVisualCategory.OBJECTS, { it.visuals.objects.showOccupiedTileFill }, { s, v -> s.visuals.objects.showOccupiedTileFill = v }),
        binding("objects.fill", DebugVisualCategory.OBJECTS, { it.visuals.objects.occupiedTileFillColor }, { s, v -> s.visuals.objects.occupiedTileFillColor = v }, colorCopy),
        binding("objects.originColor", DebugVisualCategory.OBJECTS, { it.visuals.objects.originTileColor }, { s, v -> s.visuals.objects.originTileColor = v }, colorCopy),
        binding("objects.boundsColor", DebugVisualCategory.OBJECTS, { it.visuals.objects.spriteBoundsColor }, { s, v -> s.visuals.objects.spriteBoundsColor = v }, colorCopy),
        binding("entities.tile", DebugVisualCategory.ENTITIES, { it.visuals.entities.showCurrentTile }, { s, v -> s.visuals.entities.showCurrentTile = v }),
        binding("entities.position", DebugVisualCategory.ENTITIES, { it.visuals.entities.showPosition }, { s, v -> s.visuals.entities.showPosition = v }),
        binding("entities.path", DebugVisualCategory.ENTITIES, { it.visuals.entities.showPath }, { s, v -> s.visuals.entities.showPath = v }),
        binding("entities.direction", DebugVisualCategory.ENTITIES, { it.visuals.entities.showDirection }, { s, v -> s.visuals.entities.showDirection = v }),
        binding("entities.bounds", DebugVisualCategory.ENTITIES, { it.visuals.entities.showSpriteBounds }, { s, v -> s.visuals.entities.showSpriteBounds = v }),
        binding("entities.trail", DebugVisualCategory.ENTITIES, { it.visuals.entities.showMovementTrail }, { s, v -> s.visuals.entities.showMovementTrail = v }),
        binding("entities.vector", DebugVisualCategory.ENTITIES, { it.visuals.entities.showMovementVector }, { s, v -> s.visuals.entities.showMovementVector = v }),
        binding("entities.waypoint", DebugVisualCategory.ENTITIES, { it.visuals.entities.showNextWaypoint }, { s, v -> s.visuals.entities.showNextWaypoint = v }),
        binding("entities.speed", DebugVisualCategory.ENTITIES, { it.visuals.entities.showMovementSpeed }, { s, v -> s.visuals.entities.showMovementSpeed = v }),
        binding("entities.offset", DebugVisualCategory.ENTITIES, { it.visuals.entities.showPositionTileOffset }, { s, v -> s.visuals.entities.showPositionTileOffset = v }),
        binding("entities.trailPositions", DebugVisualCategory.ENTITIES, { it.visuals.entities.trailMaxPositions }, { s, v -> s.visuals.entities.trailMaxPositions = v }),
        binding("entities.trailSeconds", DebugVisualCategory.ENTITIES, { it.visuals.entities.trailHistoryDurationSeconds }, { s, v -> s.visuals.entities.trailHistoryDurationSeconds = v }),
        binding("entities.trailDistance", DebugVisualCategory.ENTITIES, { it.visuals.entities.trailMinimumDistance }, { s, v -> s.visuals.entities.trailMinimumDistance = v }),
        binding("entities.trailOpacity", DebugVisualCategory.ENTITIES, { it.visuals.entities.trailOpacity }, { s, v -> s.visuals.entities.trailOpacity = v }),
        binding("entities.vectorScale", DebugVisualCategory.ENTITIES, { it.visuals.entities.movementVectorScaleSeconds }, { s, v -> s.visuals.entities.movementVectorScaleSeconds = v }),
        binding("entities.width", DebugVisualCategory.ENTITIES, { it.visuals.entities.lineWidth }, { s, v -> s.visuals.entities.lineWidth = v }),
        binding("entities.tileColor", DebugVisualCategory.ENTITIES, { it.visuals.entities.currentTileColor }, { s, v -> s.visuals.entities.currentTileColor = v }, colorCopy),
        binding("entities.fillEnabled", DebugVisualCategory.ENTITIES, { it.visuals.entities.showCurrentTileFill }, { s, v -> s.visuals.entities.showCurrentTileFill = v }),
        binding("entities.fill", DebugVisualCategory.ENTITIES, { it.visuals.entities.currentTileFillColor }, { s, v -> s.visuals.entities.currentTileFillColor = v }, colorCopy),
        binding("entities.positionColor", DebugVisualCategory.ENTITIES, { it.visuals.entities.positionColor }, { s, v -> s.visuals.entities.positionColor = v }, colorCopy),
        binding("entities.pathColor", DebugVisualCategory.ENTITIES, { it.visuals.entities.pathColor }, { s, v -> s.visuals.entities.pathColor = v }, colorCopy),
        binding("entities.directionColor", DebugVisualCategory.ENTITIES, { it.visuals.entities.directionColor }, { s, v -> s.visuals.entities.directionColor = v }, colorCopy),
        binding("entities.trailColor", DebugVisualCategory.ENTITIES, { it.visuals.entities.trailColor }, { s, v -> s.visuals.entities.trailColor = v }, colorCopy),
        binding("entities.vectorColor", DebugVisualCategory.ENTITIES, { it.visuals.entities.movementVectorColor }, { s, v -> s.visuals.entities.movementVectorColor = v }, colorCopy),
        binding("entities.waypointColor", DebugVisualCategory.ENTITIES, { it.visuals.entities.nextWaypointColor }, { s, v -> s.visuals.entities.nextWaypointColor = v }, colorCopy),
        binding("entities.offsetColor", DebugVisualCategory.ENTITIES, { it.visuals.entities.positionTileOffsetColor }, { s, v -> s.visuals.entities.positionTileOffsetColor = v }, colorCopy),
        binding("entities.boundsColor", DebugVisualCategory.ENTITIES, { it.visuals.entities.spriteBoundsColor }, { s, v -> s.visuals.entities.spriteBoundsColor = v }, colorCopy),
        binding("picking.enabled", DebugVisualCategory.PICKING, { it.visuals.picking.enabled }, { s, v -> s.visuals.picking.enabled = v }),
        binding("picking.bounds", DebugVisualCategory.PICKING, { it.visuals.picking.showSpriteBounds }, { s, v -> s.visuals.picking.showSpriteBounds = v }),
        binding("picking.cursor", DebugVisualCategory.PICKING, { it.visuals.picking.showCursorHit }, { s, v -> s.visuals.picking.showCursorHit = v }),
        binding("render.mode", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.mode }, { s, v -> s.visuals.renderOrder.mode = v }),
        binding("render.labels", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.showLabels }, { s, v -> s.visuals.renderOrder.showLabels = v }),
        binding("render.priorityLabels", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.showPriorityLabels }, { s, v -> s.visuals.renderOrder.showPriorityLabels = v }),
        binding("render.priorityColors", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.colorByPriority }, { s, v -> s.visuals.renderOrder.colorByPriority = v }),
        binding("render.focus", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.priorityFocusMode }, { s, v -> s.visuals.renderOrder.priorityFocusMode = v }),
        binding("render.priority", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.selectedPriority }, { s, v -> s.visuals.renderOrder.selectedPriority = v }),
        binding("render.volumes", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.showSortVolumes }, { s, v -> s.visuals.renderOrder.showSortVolumes = v }),
        binding("render.anchors", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.showSortAnchors }, { s, v -> s.visuals.renderOrder.showSortAnchors = v }),
        binding("render.projected", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.showProjectedSortPositions }, { s, v -> s.visuals.renderOrder.showProjectedSortPositions = v }),
        binding("render.terrainIndices", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.showTerrainIndices }, { s, v -> s.visuals.renderOrder.showTerrainIndices = v }),
        binding("render.heatmap", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.showTerrainHeatmap }, { s, v -> s.visuals.renderOrder.showTerrainHeatmap = v }),
        binding("render.heatmapSteps", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.terrainHeatmapSteps }, { s, v -> s.visuals.renderOrder.terrainHeatmapSteps = v }),
        binding("render.alpha", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.priorityColorAlpha }, { s, v -> s.visuals.renderOrder.priorityColorAlpha = v }),
        binding("render.width", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.sortGeometryLineWidth }, { s, v -> s.visuals.renderOrder.sortGeometryLineWidth = v }),
        binding("render.highlightColor", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.priorityHighlightColor }, { s, v -> s.visuals.renderOrder.priorityHighlightColor = v }, colorCopy),
        binding("render.volumeColor", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.sortVolumeColor }, { s, v -> s.visuals.renderOrder.sortVolumeColor = v }, colorCopy),
        binding("render.backColor", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.sortBackAnchorColor }, { s, v -> s.visuals.renderOrder.sortBackAnchorColor = v }, colorCopy),
        binding("render.frontColor", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.sortFrontAnchorColor }, { s, v -> s.visuals.renderOrder.sortFrontAnchorColor = v }, colorCopy),
        binding("render.projectedColor", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.projectedSortPositionColor }, { s, v -> s.visuals.renderOrder.projectedSortPositionColor = v }, colorCopy),
        binding("render.heatmapStart", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.terrainHeatmapStartColor }, { s, v -> s.visuals.renderOrder.terrainHeatmapStartColor = v }, colorCopy),
        binding("render.heatmapEnd", DebugVisualCategory.RENDER_ORDER, { it.visuals.renderOrder.terrainHeatmapEndColor }, { s, v -> s.visuals.renderOrder.terrainHeatmapEndColor = v }, colorCopy),
        binding("culling.area", DebugVisualCategory.CULLING, { it.visuals.culling.showVisibleArea }, { s, v -> s.visuals.culling.showVisibleArea = v }),
        binding("culling.objects", DebugVisualCategory.CULLING, { it.visuals.culling.showObjectBounds }, { s, v -> s.visuals.culling.showObjectBounds = v }),
        binding("culling.entities", DebugVisualCategory.CULLING, { it.visuals.culling.showEntityBounds }, { s, v -> s.visuals.culling.showEntityBounds = v }),
        binding("culling.areaColor", DebugVisualCategory.CULLING, { it.visuals.culling.visibleAreaColor }, { s, v -> s.visuals.culling.visibleAreaColor = v }, colorCopy),
        binding("culling.objectDrawnColor", DebugVisualCategory.CULLING, { it.visuals.culling.objectDrawnColor }, { s, v -> s.visuals.culling.objectDrawnColor = v }, colorCopy),
        binding("culling.objectCulledColor", DebugVisualCategory.CULLING, { it.visuals.culling.objectCulledColor }, { s, v -> s.visuals.culling.objectCulledColor = v }, colorCopy),
        binding("culling.entityDrawnColor", DebugVisualCategory.CULLING, { it.visuals.culling.entityDrawnColor }, { s, v -> s.visuals.culling.entityDrawnColor = v }, colorCopy),
        binding("culling.entityCulledColor", DebugVisualCategory.CULLING, { it.visuals.culling.entityCulledColor }, { s, v -> s.visuals.culling.entityCulledColor = v }, colorCopy),
        binding("camera.area", DebugVisualCategory.CAMERA, { it.visuals.camera.showVisibleArea }, { s, v -> s.visuals.camera.showVisibleArea = v }),
        binding("camera.world", DebugVisualCategory.CAMERA, { it.visuals.camera.showWorldBounds }, { s, v -> s.visuals.camera.showWorldBounds = v }),
        binding("camera.clamp", DebugVisualCategory.CAMERA, { it.visuals.camera.showClampBounds }, { s, v -> s.visuals.camera.showClampBounds = v }),
        binding("worldStats.enabled", DebugVisualCategory.GENERAL, { it.operations.worldStats.enabled }, { s, v -> s.operations.worldStats.enabled = v }),
        binding("events.records", DebugVisualCategory.EVENT_MONITOR, { it.operations.eventBus.maximumVisibleRecords }, { s, v -> s.operations.eventBus.maximumVisibleRecords = v }),
        binding("events.order", DebugVisualCategory.EVENT_MONITOR, { it.operations.eventBus.newestFirst }, { s, v -> s.operations.eventBus.newestFirst = v }),
        binding("notifications.position", DebugVisualCategory.NOTIFICATIONS, { it.operations.notifications.position }, { s, v -> s.operations.notifications.position = v })
    )

    fun capture(settings: DebugSettings): DebugVisualConfiguration = DebugVisualConfiguration(
        VERSION,
        bindings.associate { it.key to it.read(settings) }
    )

    fun apply(settings: DebugSettings, configuration: DebugVisualConfiguration) {
        require(configuration.version in 1..VERSION) {
            "Unsupported debug visual configuration version ${configuration.version}."
        }

        var values = if (configuration.version < 4) {
            migrateLegacyFillValues(configuration.values)
        } else {
            configuration.values
        }
        if (configuration.version < 5) {
            values = migratePickingActivation(values)
        }

        bindings.forEach { binding ->
            if (values.containsKey(binding.key)) {
                binding.write(settings, values[binding.key])
            }
        }

        if (configuration.version == 1) {
            migrateLegacyFeatureGates(settings, configuration)
        }
    }

    private fun migrateLegacyFillValues(
        values: Map<String, Any?>
    ): Map<String, Any?> {
        val migrated = values.toMutableMap()
        val defaults = DebugSettings()

        fun migrate(
            colorKey: String,
            enabledKey: String,
            defaultColor: Color
        ) {
            if (!migrated.containsKey(colorKey)) return

            val legacyColor = migrated[colorKey] as Color?

            migrated[enabledKey] = legacyColor != null
            migrated[colorKey] = legacyColor ?: defaultColor
        }

        migrate(
            "grid.background",
            "grid.backgroundEnabled",
            defaults.visuals.grid.backgroundColor
        )

        migrate(
            "grid.hoverBackground",
            "grid.hoverBackgroundEnabled",
            defaults.visuals.grid.hoverBackgroundColor
        )

        migrate(
            "objects.fill",
            "objects.fillEnabled",
            defaults.visuals.objects.occupiedTileFillColor
        )

        migrate(
            "entities.fill",
            "entities.fillEnabled",
            defaults.visuals.entities.currentTileFillColor
        )

        return migrated
    }

    private fun migratePickingActivation(values: Map<String, Any?>): Map<String, Any?> {
        if (values.containsKey("picking.enabled")) return values
        return values + (
            "picking.enabled" to
                (values["picking.bounds"] == true || values["picking.cursor"] == true)
            )
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
            settings.visuals.worldInfo.showTileCoordinates = false
            settings.visuals.worldInfo.showTerrainIds = false
            settings.visuals.worldInfo.showOverlayInfo = false
            settings.visuals.worldInfo.showOccupancy = false
            settings.visuals.worldInfo.showMissingTerrainVisuals = false
            settings.visuals.worldInfo.showOrigin = false
        }
        if (disabled("objects.enabled")) {
            settings.visuals.objects.showOccupiedTiles = false
            settings.visuals.objects.showOriginTile = false
            settings.visuals.objects.showSpriteBounds = false
            settings.visuals.objects.showOccupiedTileFill = false
        }
        if (disabled("entities.enabled")) {
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
        }
        if (disabled("picking.enabled")) {
            settings.visuals.picking.showSpriteBounds = false
            settings.visuals.picking.showCursorHit = false
        }
        if (disabled("render.enabled")) {
            settings.visuals.renderOrder.showLabels = false
            settings.visuals.renderOrder.showPriorityLabels = false
            settings.visuals.renderOrder.colorByPriority = false
            settings.visuals.renderOrder.priorityFocusMode = RenderPriorityFocusMode.OFF
            settings.visuals.renderOrder.showSortVolumes = false
            settings.visuals.renderOrder.showSortAnchors = false
            settings.visuals.renderOrder.showProjectedSortPositions = false
            settings.visuals.renderOrder.showTerrainIndices = false
            settings.visuals.renderOrder.showTerrainHeatmap = false
        }
        if (disabled("culling.enabled")) {
            settings.visuals.culling.showVisibleArea = false
            settings.visuals.culling.showObjectBounds = false
            settings.visuals.culling.showEntityBounds = false
        }
        if (disabled("camera.enabled")) {
            settings.visuals.camera.showVisibleArea = false
            settings.visuals.camera.showWorldBounds = false
            settings.visuals.camera.showClampBounds = false
        }
    }
}
