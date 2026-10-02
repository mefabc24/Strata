package com.mefabc24.strata.debug

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.pathfinding.PathMovementMode
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

enum class DebugGridRenderLayer { BELOW_OBJECTS, ABOVE_OBJECTS }
enum class DebugGridExtent { WORLD, VISIBLE }
enum class RenderOrderDebugMode { CALCULATED, ACTUAL }
enum class RenderPriorityFocusMode { OFF, HIGHLIGHT, ISOLATE }
enum class TerrainHeatmapSteps(val colorLevelCount: Int?) {
    PER_TILE(null),
    STEPS_32(32),
    STEPS_16(16),
    STEPS_8(8)
}

/** Built-in debug panel and world diagnostic configuration. */
class DebugSettings {
    internal val worldState = DebugWorldState()
    internal val entityFreezeState = DebugEntityFreezeState()
    val panel = DebugPanelSettings()
    val performance = DebugPerformanceSettings()
    val simulation = DebugSimulationSettings()
    val grid = DebugGridSettings()
    val worldInfo = DebugWorldInfoSettings()
    val worldVisibility = DebugWorldVisibilitySettings()
    val objects = DebugObjectSettings()
    val entities = DebugEntitySettings()
    val picking = DebugPickingSettings()
    val renderOrder = DebugRenderOrderSettings()
    val culling = DebugCullingSettings()
    val camera = DebugCameraSettings()
    val worldStats = DebugFeatureSettings()
    val inspect = DebugInspectSettings()
    val paint = DebugPaintToolSettings()
    val delete = DebugDeleteToolSettings()
    val pathfinding = DebugPathfindingSettings()
    val eventBus = DebugEventMonitorSettings()
    val notifications = DebugNotifications()

    /** Session-owned user presets for visual diagnostics. */
    val customPresets = DebugCustomPresetStore()

    /** Target subset shared by object, entity, culling, and render-order visuals. */
    var visualizationFilter: DebugVisualizationFilter = DebugVisualizationFilter.ALL

    internal var entitySpawnedCallback: ((WorldEntity) -> Unit)? = null
    internal var objectsPlacedCallback: ((List<PlacedObject>) -> Unit)? = null
    internal var pathTraversal: ((World, TilePosition) -> Boolean)? = null
    internal var pathCost: ((World, TilePosition, TilePosition) -> Float)? = null

    fun panel(configure: DebugPanelSettings.() -> Unit) = panel.apply(configure)
    fun performance(configure: DebugPerformanceSettings.() -> Unit) = performance.apply(configure)
    fun simulation(configure: DebugSimulationSettings.() -> Unit) = simulation.apply(configure)
    fun grid(configure: DebugGridSettings.() -> Unit) = grid.apply(configure)
    fun worldInfo(configure: DebugWorldInfoSettings.() -> Unit) = worldInfo.apply(configure)
    fun worldVisibility(configure: DebugWorldVisibilitySettings.() -> Unit) =
        worldVisibility.apply(configure)
    fun objects(configure: DebugObjectSettings.() -> Unit) = objects.apply(configure)
    fun entities(configure: DebugEntitySettings.() -> Unit) = entities.apply(configure)
    fun picking(configure: DebugPickingSettings.() -> Unit) = picking.apply(configure)
    fun renderOrder(configure: DebugRenderOrderSettings.() -> Unit) = renderOrder.apply(configure)
    fun culling(configure: DebugCullingSettings.() -> Unit) = culling.apply(configure)
    fun camera(configure: DebugCameraSettings.() -> Unit) = camera.apply(configure)
    fun worldStats(configure: DebugFeatureSettings.() -> Unit) = worldStats.apply(configure)
    fun inspect(configure: DebugInspectSettings.() -> Unit) = inspect.apply(configure)
    fun paint(configure: DebugPaintToolSettings.() -> Unit) = paint.apply(configure)
    fun delete(configure: DebugDeleteToolSettings.() -> Unit) = delete.apply(configure)
    fun pathfinding(configure: DebugPathfindingSettings.() -> Unit) = pathfinding.apply(configure)
    fun eventBus(configure: DebugEventMonitorSettings.() -> Unit) = eventBus.apply(configure)

    /** Returns whether debug controls have frozen engine movement for [entity]. */
    fun isEntityFrozen(entity: WorldEntity): Boolean =
        entityFreezeState.isFrozen(entity)

    /** Returns whether any debug tool currently holds [entity]. */
    fun isEntityHeld(entity: WorldEntity): Boolean =
        entityFreezeState.isHeld(entity)

    /** Freezes or unfreezes engine-owned movement for [entity]. */
    fun setEntityFrozen(entity: WorldEntity, frozen: Boolean) {
        entityFreezeState.setFrozen(entity, frozen)
    }

    /** Unfreezes every entity currently held by the debug runtime. */
    fun unfreezeAllEntities(): Int = entityFreezeState.clear()

    /** Emits a short developer-facing notification. */
    fun notify(
        message: String,
        severity: DebugNotificationSeverity = DebugNotificationSeverity.INFO,
        durationSeconds: Float = notifications.defaultDurationSeconds
    ) {
        notifications.emit(message, severity, durationSeconds)
    }

    /** Called after an entity was created by the debug Spawn tool. */
    fun onEntitySpawned(callback: (WorldEntity) -> Unit) {
        entitySpawnedCallback = callback
    }

    /** Called after a Build drag placed one or more objects. */
    fun onObjectsPlaced(callback: (List<PlacedObject>) -> Unit) {
        objectsPlacedCallback = callback
    }

    /** Supplies optional game traversal rules for debug path searches. */
    fun pathTraversal(canEnter: (World, TilePosition) -> Boolean) {
        pathTraversal = canEnter
    }

    /** Supplies optional game movement costs for debug path searches. */
    fun pathCost(cost: (World, from: TilePosition, to: TilePosition) -> Float) {
        pathCost = cost
    }

    /** Applies a visual-only settings preset. */
    fun applyPreset(preset: DebugPreset) {
        DebugPresets.apply(this, preset)
    }

    /** Captures the current visual diagnostics without operational tool state. */
    fun captureVisualConfiguration(): DebugVisualConfiguration =
        DebugVisualSettings.capture(this)

    /** Applies a previously captured visual diagnostics configuration. */
    fun applyVisualConfiguration(configuration: DebugVisualConfiguration) {
        DebugVisualSettings.apply(this, configuration)
    }

    /** Restores every visual diagnostic to its engine default. */
    fun resetVisualConfiguration() {
        DebugVisualSettings.reset(this)
    }

    /** Restores one visual diagnostic category to its engine default. */
    fun resetVisualCategory(category: DebugVisualCategory) {
        DebugVisualSettings.reset(this, category)
    }
}

class DebugPanelSettings {
    /** Whether the debug panel is enabled. */
    var enabled: Boolean = false

    /** Whether the panel should be visible when initialized. */
    var visibleOnStartup: Boolean = false

    /** Current runtime visibility of the panel. */
    var visible: Boolean = false
        internal set

    var toggleKey: Int = Input.Keys.F3
        set(value) {
            require(value >= 0) {
                "Debug panel toggle key must be non-negative."
            }
            field = value
        }
}

/** Independent output controls for the shared renderer performance metrics. */
class DebugPerformanceSettings internal constructor() {
    private val terminalLogger = DebugPerformanceLogger()
    val history = DebugPerformanceHistory()

    /** Whether the on-screen performance overlay is visible. */
    var overlayEnabled: Boolean = false

    /** Whether performance summaries are periodically written to the terminal log. */
    var terminalLoggingEnabled: Boolean
        get() = terminalLogger.enabled
        set(value) { terminalLogger.enabled = value }

    /** Seconds of rendered time accumulated between terminal performance summaries. */
    var terminalLoggingIntervalSeconds: Float
        get() = terminalLogger.intervalSeconds
        set(value) { terminalLogger.intervalSeconds = value }

    var historyLength: Int
        get() = history.capacity
        set(value) { history.capacity = value }

    var historyRecording: Boolean
        get() = history.recording
        set(value) { history.recording = value }

    var historyMetric: DebugPerformanceMetric = DebugPerformanceMetric.FRAME_TIME

    fun startHistoryRecording() {
        history.recording = true
    }

    fun stopHistoryRecording() {
        history.recording = false
    }

    fun clearHistory() {
        history.clear()
    }

    internal fun record(stats: com.mefabc24.strata.render.RenderStats, delta: Float) {
        terminalLogger.record(stats, delta)
        history.record(stats, delta)
    }
}

/** Common square-brush options for debug world editing tools. */
open class DebugBrushSettings {
    /** Odd side length of the square affected tile area. */
    var brushSize: Int = 1
        set(value) {
            require(value in 1..9 && value % 2 == 1) {
                "Debug brush size must be an odd number between 1 and 9."
            }
            field = value
        }

    /** Whether the current affected tile area is drawn under the cursor. */
    var showBrushPreview: Boolean = true
}

/** Brush options for the terrain Paint tool. */
class DebugPaintToolSettings : DebugBrushSettings()

/** Brush and stroke options for the Delete tool. */
class DebugDeleteToolSettings : DebugBrushSettings() {
    /** Whether holding the left mouse button continues deletion along cursor movement. */
    var dragEnabled: Boolean = true
}

open class DebugFeatureSettings {
    var enabled: Boolean = false
}

class DebugSimulationSettings : DebugFeatureSettings() {
    /** Stops visual animation clocks while simulation continues normally. */
    var freezeVisualAnimations: Boolean = false
}

/** Information drawn directly over visible world tiles. */
class DebugWorldInfoSettings : DebugFeatureSettings() {
    var showTileCoordinates: Boolean = false
    var showTerrainIds: Boolean = false
    var showOverlayInfo: Boolean = false
    var showOccupancy: Boolean = false
    var showMissingTerrainVisuals: Boolean = false
    var showOrigin: Boolean = true

    /** Labels are omitted beyond this camera zoom to keep them readable. */
    var maximumLabelZoom: Float = 1.5f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "World information maximum label zoom must be finite and positive."
            }
            field = value
        }

    /** Upper bound used to sample labels across large visible tile ranges. */
    var maximumVisibleLabels: Int = 256
        set(value) {
            require(value in 1..4096) {
                "Maximum visible world labels must be between 1 and 4096."
            }
            field = value
        }

    private var storedLabelColor = Color.WHITE.cpy()
    var labelColor: Color
        get() = storedLabelColor.cpy()
        set(value) { storedLabelColor = value.cpy() }

    private var storedOccupancyColor = Color(1f, 0.35f, 0.15f, 0.28f)
    var occupancyColor: Color
        get() = storedOccupancyColor.cpy()
        set(value) { storedOccupancyColor = value.cpy() }

    private var storedMissingVisualColor = Color(1f, 0.1f, 0.65f, 0.42f)
    var missingVisualColor: Color
        get() = storedMissingVisualColor.cpy()
        set(value) { storedMissingVisualColor = value.cpy() }

    private var storedOriginColor = Color(0.2f, 1f, 0.85f, 1f)
    var originColor: Color
        get() = storedOriginColor.cpy()
        set(value) { storedOriginColor = value.cpy() }
}

/** Debug-only category visibility. These flags never alter world contents. */
class DebugWorldVisibilitySettings {
    var groundTerrainVisible: Boolean = true
    var terrainOverlaysVisible: Boolean = true
    var placedObjectsVisible: Boolean = true
    var entitiesVisible: Boolean = true

    private val hiddenOverlayLayers = mutableSetOf<String>()

    fun isOverlayLayerVisible(layerId: String): Boolean = layerId !in hiddenOverlayLayers

    fun setOverlayLayerVisible(layerId: String, visible: Boolean) {
        require(layerId.isNotBlank()) { "Overlay layer ID must not be blank." }
        if (visible) hiddenOverlayLayers.remove(layerId) else hiddenOverlayLayers.add(layerId)
    }

    internal fun hiddenOverlayLayerIds(): Set<String> = hiddenOverlayLayers.toSet()

    internal fun restoreHiddenOverlayLayers(layerIds: Set<String>) {
        hiddenOverlayLayers.clear()
        hiddenOverlayLayers.addAll(layerIds)
    }

    /** Restores the normal renderer view for every category and overlay layer. */
    fun showAll() {
        groundTerrainVisible = true
        terrainOverlaysVisible = true
        placedObjectsVisible = true
        entitiesVisible = true
        hiddenOverlayLayers.clear()
    }
}

class DebugPickingSettings : DebugFeatureSettings() {
    var showSpriteBounds: Boolean = true
    var showCursorHit: Boolean = true
}

class DebugRenderOrderSettings : DebugFeatureSettings() {
    var mode: RenderOrderDebugMode = RenderOrderDebugMode.CALCULATED
    var showLabels: Boolean = true
    var showPriorityLabels: Boolean = false
    var colorByPriority: Boolean = false
    var priorityFocusMode: RenderPriorityFocusMode = RenderPriorityFocusMode.OFF
    var selectedPriority: Int = 0
    var showSortVolumes: Boolean = false
    var showSortAnchors: Boolean = false
    var showProjectedSortPositions: Boolean = false
    var showTerrainIndices: Boolean = false
    var showTerrainHeatmap: Boolean = false
    var terrainHeatmapSteps: TerrainHeatmapSteps = TerrainHeatmapSteps.PER_TILE

    var priorityColorAlpha: Float = 0.2f
        set(value) {
            require(value.isFinite() && value in 0f..1f) {
                "Render priority color alpha must be between zero and one."
            }
            field = value
        }

    var sortGeometryLineWidth: Float = 2f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Sort geometry line width must be finite and positive."
            }
            field = value
        }

    private var storedPriorityHighlightColor = Color(1f, 0.92f, 0.2f, 1f)
    var priorityHighlightColor: Color
        get() = storedPriorityHighlightColor.cpy()
        set(value) { storedPriorityHighlightColor = value.cpy() }

    private var storedSortVolumeColor = Color(0.25f, 0.9f, 1f, 1f)
    var sortVolumeColor: Color
        get() = storedSortVolumeColor.cpy()
        set(value) { storedSortVolumeColor = value.cpy() }

    private var storedSortBackAnchorColor = Color(0.35f, 1f, 0.4f, 1f)
    var sortBackAnchorColor: Color
        get() = storedSortBackAnchorColor.cpy()
        set(value) { storedSortBackAnchorColor = value.cpy() }

    private var storedSortFrontAnchorColor = Color(1f, 0.35f, 0.25f, 1f)
    var sortFrontAnchorColor: Color
        get() = storedSortFrontAnchorColor.cpy()
        set(value) { storedSortFrontAnchorColor = value.cpy() }

    private var storedProjectedSortPositionColor = Color(1f, 0.3f, 0.9f, 1f)
    var projectedSortPositionColor: Color
        get() = storedProjectedSortPositionColor.cpy()
        set(value) { storedProjectedSortPositionColor = value.cpy() }

    private var storedTerrainHeatmapStartColor = Color(0.1f, 0.65f, 1f, 0.55f)
    var terrainHeatmapStartColor: Color
        get() = storedTerrainHeatmapStartColor.cpy()
        set(value) { storedTerrainHeatmapStartColor = value.cpy() }

    private var storedTerrainHeatmapEndColor = Color(1f, 0.2f, 0.25f, 0.55f)
    var terrainHeatmapEndColor: Color
        get() = storedTerrainHeatmapEndColor.cpy()
        set(value) { storedTerrainHeatmapEndColor = value.cpy() }
}

class DebugCullingSettings : DebugFeatureSettings() {
    var showVisibleArea: Boolean = true
    var showObjectBounds: Boolean = true
    var showEntityBounds: Boolean = true

    private var storedVisibleAreaColor = Color(0.72f, 0.35f, 1f, 1f)
    var visibleAreaColor: Color
        get() = storedVisibleAreaColor.cpy()
        set(value) { storedVisibleAreaColor = value.cpy() }

    private var storedObjectDrawnColor = Color(0.15f, 0.85f, 1f, 1f)
    var objectDrawnColor: Color
        get() = storedObjectDrawnColor.cpy()
        set(value) { storedObjectDrawnColor = value.cpy() }

    private var storedObjectCulledColor = Color(0.08f, 0.32f, 0.42f, 0.8f)
    var objectCulledColor: Color
        get() = storedObjectCulledColor.cpy()
        set(value) { storedObjectCulledColor = value.cpy() }

    private var storedEntityDrawnColor = Color(1f, 0.68f, 0.15f, 1f)
    var entityDrawnColor: Color
        get() = storedEntityDrawnColor.cpy()
        set(value) { storedEntityDrawnColor = value.cpy() }

    private var storedEntityCulledColor = Color(0.48f, 0.28f, 0.06f, 0.8f)
    var entityCulledColor: Color
        get() = storedEntityCulledColor.cpy()
        set(value) { storedEntityCulledColor = value.cpy() }
}

class DebugCameraSettings : DebugFeatureSettings() {
    var showVisibleArea: Boolean = true
    var showWorldBounds: Boolean = true
    var showClampBounds: Boolean = true

    var disableRestrictions: Boolean = false
}

/** Visualization options owned by the Inspect tool. */
class DebugInspectSettings {
    var freezeEntityAnimation: Boolean = true
    var showTile: Boolean = true
    var showObjectFootprint: Boolean = true
    var showObjectOrigin: Boolean = true
    var showObjectSpriteBounds: Boolean = true
    var showEntityTile: Boolean = true
    var showEntityPosition: Boolean = true
    var showEntityPath: Boolean = true
    var showEntityDirection: Boolean = true
    var showEntitySpriteBounds: Boolean = true
}

class DebugPathfindingSettings : DebugFeatureSettings() {
    init { enabled = true }

    /** Tile transitions used by the pathfinding debug tool. */
    var movementMode: PathMovementMode = PathMovementMode.FOUR_WAY

    var showExploredNodes: Boolean = true
    var showFinalPath: Boolean = true
    var showOpenSet: Boolean = false
    var showClosedSet: Boolean = false
    var showGCost: Boolean = false
    var showHCost: Boolean = false
    var showFCost: Boolean = false
    var showParentDirections: Boolean = false
    var showExplorationOrder: Boolean = false
    var showRejectedTransitions: Boolean = false

    var maximumLabelZoom: Float = 1.5f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Pathfinding maximum label zoom must be finite and positive."
            }
            field = value
        }

    var maximumVisibleLabels: Int = 128
        set(value) {
            require(value in 1..4096) {
                "Maximum visible pathfinding labels must be between 1 and 4096."
            }
            field = value
        }

    var maximumRejectedTransitions: Int = 2048
        set(value) {
            require(value in 0..16384) {
                "Maximum rejected pathfinding transitions must be between 0 and 16384."
            }
            field = value
        }

    var automaticIterationsPerUpdate: Int = 1
        set(value) {
            require(value in 1..1024) {
                "Automatic pathfinding iterations per update must be between 1 and 1024."
            }
            field = value
        }

    /** Removes completed sections from debug-assigned entity route diagnostics. */
    var consumeReachedWaypoints: Boolean = true

    /** Multiplier for the debug tool's fallback speed when assigning an idle entity. */
    var entitySpeedMultiplier: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Debug pathfinding entity speed multiplier must be finite and positive."
            }
            field = value
        }
}

class DebugEventMonitorSettings : DebugFeatureSettings() {
    var captureEnabled: Boolean = true
    var maximumVisibleRecords: Int = 8
        set(value) {
            require(value in 1..25) {
                "Maximum visible Event Bus Monitor records must be between 1 and 25."
            }
            field = value
        }
    var newestFirst: Boolean = true
}

/** Runtime configuration for placed-object diagnostics. */
class DebugObjectSettings : DebugFeatureSettings() {
    var showOccupiedTiles: Boolean = true
    var showOriginTile: Boolean = true
    var showSpriteBounds: Boolean = false

    var lineWidth: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Debug object line width must be finite and positive."
            }
            field = value
        }

    private var storedOccupiedTileColor = Color(0.2f, 0.85f, 1f, 1f)
    var occupiedTileColor: Color
        get() = storedOccupiedTileColor.cpy()
        set(value) { storedOccupiedTileColor = value.cpy() }

    private var storedOccupiedTileFillColor: Color? = Color(0.2f, 0.65f, 1f, 0.18f)
    var occupiedTileFillColor: Color?
        get() = storedOccupiedTileFillColor?.cpy()
        set(value) { storedOccupiedTileFillColor = value?.cpy() }

    private var storedOriginTileColor = Color(1f, 0.35f, 0.2f, 1f)
    var originTileColor: Color
        get() = storedOriginTileColor.cpy()
        set(value) { storedOriginTileColor = value.cpy() }

    private var storedSpriteBoundsColor = Color(1f, 0.2f, 0.75f, 1f)
    var spriteBoundsColor: Color
        get() = storedSpriteBoundsColor.cpy()
        set(value) { storedSpriteBoundsColor = value.cpy() }
}

/** Runtime configuration for world-entity diagnostics. */
class DebugEntitySettings : DebugFeatureSettings() {
    var showCurrentTile: Boolean = true
    var showPosition: Boolean = true
    var showPath: Boolean = true
    var showDirection: Boolean = false
    var showSpriteBounds: Boolean = false
    var showMovementTrail: Boolean = false
    var showMovementVector: Boolean = false
    var showNextWaypoint: Boolean = false
    var showMovementSpeed: Boolean = false
    var showPositionTileOffset: Boolean = false

    var trailMaxPositions: Int = 120
        set(value) {
            require(value in 2..4096) {
                "Entity trail maximum positions must be between 2 and 4096."
            }
            field = value
        }

    var trailHistoryDurationSeconds: Float = 5f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Entity trail history duration must be finite and positive."
            }
            field = value
        }

    var trailMinimumDistance: Float = 0.02f
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "Entity trail minimum distance must be finite and non-negative."
            }
            field = value
        }

    var trailOpacity: Float = 0.65f
        set(value) {
            require(value.isFinite() && value in 0f..1f) {
                "Entity trail opacity must be between zero and one."
            }
            field = value
        }

    /** Length of the velocity arrow in seconds of current movement. */
    var movementVectorScaleSeconds: Float = 0.5f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Entity movement vector scale must be finite and positive."
            }
            field = value
        }

    internal var trailClearGeneration: Long = 0L
        private set

    /** Clears engine-owned movement trail history on the next debug update. */
    fun clearMovementTrails() {
        trailClearGeneration++
    }

    var lineWidth: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Debug entity line width must be finite and positive."
            }
            field = value
        }

    private var storedCurrentTileColor = Color(0.4f, 1f, 0.3f, 1f)
    var currentTileColor: Color
        get() = storedCurrentTileColor.cpy()
        set(value) { storedCurrentTileColor = value.cpy() }

    private var storedCurrentTileFillColor: Color? = Color(0.3f, 1f, 0.3f, 0.16f)
    var currentTileFillColor: Color?
        get() = storedCurrentTileFillColor?.cpy()
        set(value) { storedCurrentTileFillColor = value?.cpy() }

    private var storedPositionColor = Color(1f, 0.3f, 0.2f, 1f)
    var positionColor: Color
        get() = storedPositionColor.cpy()
        set(value) { storedPositionColor = value.cpy() }

    private var storedPathColor = Color(1f, 0.85f, 0.2f, 1f)
    var pathColor: Color
        get() = storedPathColor.cpy()
        set(value) { storedPathColor = value.cpy() }

    private var storedDirectionColor = Color(0.3f, 0.75f, 1f, 1f)
    var directionColor: Color
        get() = storedDirectionColor.cpy()
        set(value) { storedDirectionColor = value.cpy() }

    private var storedTrailColor = Color(0.2f, 0.9f, 1f, 1f)
    var trailColor: Color
        get() = storedTrailColor.cpy()
        set(value) { storedTrailColor = value.cpy() }

    private var storedMovementVectorColor = Color(1f, 0.45f, 0.15f, 1f)
    var movementVectorColor: Color
        get() = storedMovementVectorColor.cpy()
        set(value) { storedMovementVectorColor = value.cpy() }

    private var storedNextWaypointColor = Color(1f, 0.2f, 0.75f, 1f)
    var nextWaypointColor: Color
        get() = storedNextWaypointColor.cpy()
        set(value) { storedNextWaypointColor = value.cpy() }

    private var storedPositionTileOffsetColor = Color(0.8f, 0.5f, 1f, 1f)
    var positionTileOffsetColor: Color
        get() = storedPositionTileOffsetColor.cpy()
        set(value) { storedPositionTileOffsetColor = value.cpy() }

    private var storedSpriteBoundsColor = Color(1f, 0.3f, 0.9f, 1f)
    var spriteBoundsColor: Color
        get() = storedSpriteBoundsColor.cpy()
        set(value) { storedSpriteBoundsColor = value.cpy() }
}

/** Runtime configuration for the isometric world-grid overlay. */
class DebugGridSettings : DebugFeatureSettings() {
    var renderLayer: DebugGridRenderLayer = DebugGridRenderLayer.BELOW_OBJECTS
    var extent: DebugGridExtent = DebugGridExtent.WORLD

    private var storedColor = Color(0.4f, 0.8f, 0.5f, 1f)
    var color: Color
        get() = storedColor.cpy()
        set(value) { storedColor = value.cpy() }

    private var storedHoverColor = Color(1f, 0.85f, 0.2f, 1f)
    var hoverColor: Color
        get() = storedHoverColor.cpy()
        set(value) { storedHoverColor = value.cpy() }

    var lineWidth: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Debug grid line width must be finite and positive."
            }
            field = value
        }

    private var storedBackgroundColor: Color? = null
    var backgroundColor: Color?
        get() = storedBackgroundColor?.cpy()
        set(value) { storedBackgroundColor = value?.cpy() }

    private var storedHoverBackgroundColor: Color? = null
    var hoverBackgroundColor: Color?
        get() = storedHoverBackgroundColor?.cpy()
        set(value) { storedHoverBackgroundColor = value?.cpy() }

    internal fun copy(): DebugGridSettings = DebugGridSettings().also {
        it.enabled = enabled
        it.renderLayer = renderLayer
        it.extent = extent
        it.color = color
        it.hoverColor = hoverColor
        it.lineWidth = lineWidth
        it.backgroundColor = backgroundColor
        it.hoverBackgroundColor = hoverBackgroundColor
    }
}

enum class DebugPreset { OFF, MINIMAL, PLACEMENT, ENTITIES, RENDERING, EVERYTHING }

internal object DebugPresets {
    fun apply(settings: DebugSettings, preset: DebugPreset) {
        disableVisuals(settings)
        when (preset) {
            DebugPreset.OFF -> Unit
            DebugPreset.MINIMAL -> {
                settings.performance.overlayEnabled = true
                settings.grid.enabled = true
                settings.notifications.enabled = true
            }
            DebugPreset.PLACEMENT -> {
                settings.grid.enabled = true
                settings.objects.enabled = true
                settings.objects.showOccupiedTiles = true
                settings.objects.showOriginTile = true
                settings.objects.occupiedTileFillColor = Color(0.2f, 0.65f, 1f, 0.18f)
                settings.notifications.enabled = true
            }
            DebugPreset.ENTITIES -> {
                settings.entities.enabled = true
                settings.entities.showCurrentTile = true
                settings.entities.showPosition = true
                settings.entities.showPath = true
                settings.entities.showDirection = true
                settings.entities.showSpriteBounds = true
                settings.entities.currentTileFillColor = Color(0.3f, 1f, 0.3f, 0.16f)
                settings.notifications.enabled = true
            }
            DebugPreset.RENDERING -> {
                settings.performance.overlayEnabled = true
                settings.objects.enabled = true
                settings.objects.showSpriteBounds = true
                settings.entities.enabled = true
                settings.entities.showSpriteBounds = true
                settings.renderOrder.enabled = true
                settings.renderOrder.showLabels = true
                settings.culling.enabled = true
                settings.culling.showVisibleArea = true
                settings.culling.showObjectBounds = true
                settings.culling.showEntityBounds = true
                settings.camera.enabled = true
                settings.camera.showVisibleArea = true
                settings.camera.showWorldBounds = true
                settings.camera.showClampBounds = true
                settings.notifications.enabled = true
            }
            DebugPreset.EVERYTHING -> {
                settings.performance.overlayEnabled = true
                settings.simulation.enabled = true
                settings.grid.enabled = true
                settings.grid.backgroundColor = Color(1f, 1f, 1f, 0.2f)
                settings.grid.hoverBackgroundColor = Color(1f, 0f, 0f, 0.5f)
                settings.worldInfo.enabled = true
                settings.worldInfo.showTileCoordinates = true
                settings.worldInfo.showTerrainIds = true
                settings.worldInfo.showOverlayInfo = true
                settings.worldInfo.showOccupancy = true
                settings.worldInfo.showMissingTerrainVisuals = true
                settings.objects.enabled = true
                settings.objects.showOccupiedTiles = true
                settings.objects.showOriginTile = true
                settings.objects.showSpriteBounds = true
                settings.objects.occupiedTileFillColor = Color(0.2f, 0.65f, 1f, 0.18f)
                settings.entities.enabled = true
                settings.entities.showCurrentTile = true
                settings.entities.showPosition = true
                settings.entities.showPath = true
                settings.entities.showDirection = true
                settings.entities.showSpriteBounds = true
                settings.entities.currentTileFillColor = Color(0.3f, 1f, 0.3f, 0.16f)
                settings.picking.enabled = true
                settings.picking.showSpriteBounds = true
                settings.picking.showCursorHit = true
                settings.renderOrder.enabled = true
                settings.renderOrder.showLabels = true
                settings.culling.enabled = true
                settings.culling.showVisibleArea = true
                settings.culling.showObjectBounds = true
                settings.culling.showEntityBounds = true
                settings.camera.enabled = true
                settings.camera.showVisibleArea = true
                settings.camera.showWorldBounds = true
                settings.camera.showClampBounds = true
                settings.worldStats.enabled = true
                settings.eventBus.enabled = true
                settings.camera.disableRestrictions = true
                settings.notifications.enabled = true
            }
        }
    }

    private fun disableVisuals(settings: DebugSettings) {
        settings.performance.overlayEnabled = false
        settings.simulation.enabled = false
        settings.grid.enabled = false
        settings.worldInfo.enabled = false
        settings.worldVisibility.showAll()
        settings.objects.enabled = false
        settings.entities.enabled = false
        settings.picking.enabled = false
        settings.renderOrder.enabled = false
        settings.culling.enabled = false
        settings.camera.enabled = false
        settings.worldStats.enabled = false
        settings.eventBus.enabled = false
        settings.camera.disableRestrictions = false
        settings.notifications.enabled = false
    }
}
