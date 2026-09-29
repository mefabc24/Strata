package com.mefabc24.strata.debug

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

enum class DebugGridRenderLayer { BELOW_OBJECTS, ABOVE_OBJECTS }
enum class DebugGridExtent { WORLD, VISIBLE }

/** Built-in debug panel and world diagnostic configuration. */
class DebugSettings {
    internal val worldState = DebugWorldState()
    internal val entityFreezeState = DebugEntityFreezeState()
    val panel = DebugPanelSettings()
    val performance = DebugPerformanceLogger()
    val simulation = DebugFeatureSettings()
    val grid = DebugGridSettings()
    val objects = DebugObjectSettings()
    val entities = DebugEntitySettings()
    val picking = DebugPickingSettings()
    val renderOrder = DebugRenderOrderSettings()
    val culling = DebugCullingSettings()
    val camera = DebugCameraSettings()
    val worldStats = DebugFeatureSettings()
    val inspect = DebugInspectSettings()
    val pathfinding = DebugPathfindingSettings()
    val eventBus = DebugEventMonitorSettings()
    val notifications = DebugNotifications()

    internal var entitySpawnedCallback: ((WorldEntity) -> Unit)? = null
    internal var objectsPlacedCallback: ((List<PlacedObject>) -> Unit)? = null
    internal var pathTraversal: ((World, TilePosition) -> Boolean)? = null

    fun panel(configure: DebugPanelSettings.() -> Unit) = panel.apply(configure)
    fun performance(configure: DebugPerformanceLogger.() -> Unit) = performance.apply(configure)
    fun simulation(configure: DebugFeatureSettings.() -> Unit) = simulation.apply(configure)
    fun grid(configure: DebugGridSettings.() -> Unit) = grid.apply(configure)
    fun objects(configure: DebugObjectSettings.() -> Unit) = objects.apply(configure)
    fun entities(configure: DebugEntitySettings.() -> Unit) = entities.apply(configure)
    fun picking(configure: DebugPickingSettings.() -> Unit) = picking.apply(configure)
    fun renderOrder(configure: DebugRenderOrderSettings.() -> Unit) = renderOrder.apply(configure)
    fun culling(configure: DebugCullingSettings.() -> Unit) = culling.apply(configure)
    fun camera(configure: DebugCameraSettings.() -> Unit) = camera.apply(configure)
    fun worldStats(configure: DebugFeatureSettings.() -> Unit) = worldStats.apply(configure)
    fun inspect(configure: DebugInspectSettings.() -> Unit) = inspect.apply(configure)
    fun pathfinding(configure: DebugPathfindingSettings.() -> Unit) = pathfinding.apply(configure)
    fun eventBus(configure: DebugEventMonitorSettings.() -> Unit) = eventBus.apply(configure)

    /** Returns whether debug controls have frozen engine movement for [entity]. */
    fun isEntityFrozen(entity: WorldEntity): Boolean =
        entityFreezeState.isFrozen(entity)

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

    /** Applies a visual-only settings preset. */
    fun applyPreset(preset: DebugPreset) {
        DebugPresets.apply(this, preset)
    }
}

class DebugPanelSettings {
    var enabled: Boolean = false
    var visible: Boolean = true
    var expanded: Boolean = false
    var toggleKey: Int = Input.Keys.F3
        set(value) {
            require(value >= 0) { "Debug panel toggle key must be non-negative." }
            field = value
        }
}

open class DebugFeatureSettings {
    var enabled: Boolean = false
}

class DebugPickingSettings : DebugFeatureSettings() {
    var showSpriteBounds: Boolean = true
    var showCursorHit: Boolean = true
}

class DebugRenderOrderSettings : DebugFeatureSettings() {
    var showLabels: Boolean = true
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

    var showExploredNodes: Boolean = true
    var showFinalPath: Boolean = true
}

class DebugEventMonitorSettings : DebugFeatureSettings() {
    var paused: Boolean = false
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
                settings.performance.enabled = true
                settings.grid.enabled = true
            }
            DebugPreset.PLACEMENT -> {
                settings.grid.enabled = true
                settings.objects.enabled = true
                settings.objects.showOccupiedTiles = true
                settings.objects.showOriginTile = true
                settings.objects.occupiedTileFillColor = Color(0.2f, 0.65f, 1f, 0.18f)
            }
            DebugPreset.ENTITIES -> {
                settings.entities.enabled = true
                settings.entities.showCurrentTile = true
                settings.entities.showPosition = true
                settings.entities.showPath = true
                settings.entities.showDirection = true
                settings.entities.showSpriteBounds = true
                settings.entities.currentTileFillColor = Color(0.3f, 1f, 0.3f, 0.16f)
            }
            DebugPreset.RENDERING -> {
                settings.performance.enabled = true
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
            }
            DebugPreset.EVERYTHING -> {
                settings.performance.enabled = true
                settings.simulation.enabled = true
                settings.grid.enabled = true
                settings.grid.backgroundColor = Color(1f, 1f, 1f, 0.2f)
                settings.grid.hoverBackgroundColor = Color(1f, 0f, 0f, 0.5f)
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
            }
        }
    }

    private fun disableVisuals(settings: DebugSettings) {
        settings.performance.enabled = false
        settings.simulation.enabled = false
        settings.grid.enabled = false
        settings.objects.enabled = false
        settings.entities.enabled = false
        settings.picking.enabled = false
        settings.renderOrder.enabled = false
        settings.culling.enabled = false
        settings.camera.enabled = false
        settings.worldStats.enabled = false
        settings.eventBus.enabled = false
    }
}
