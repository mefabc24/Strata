package com.mefabc24.strata.debug

import com.mefabc24.strata.pathfinding.PathMovementMode
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

/** Configures interactive tools exposed through the Tool Rail. */
class DebugToolSettings internal constructor() {
    /** Inspect tool selection and visualization behavior. */
    val inspect = DebugInspectSettings()

    /** Terrain Paint tool brush behavior. */
    val paint = DebugPaintToolSettings()

    /** Delete tool brush and stroke behavior. */
    val delete = DebugDeleteToolSettings()

    /** Pathfinding tool availability, search, and visualization behavior. */
    val pathfinding = DebugPathfindingSettings()

    internal var entitySpawnedCallback: ((WorldEntity) -> Unit)? = null
    internal var objectsPlacedCallback: ((List<PlacedObject>) -> Unit)? = null
    internal var pathTraversal: ((World, TilePosition) -> Boolean)? = null
    internal var pathCost: ((World, TilePosition, TilePosition) -> Float)? = null

    /** Configures Inspect tool selection visuals. */
    fun inspect(configure: DebugInspectSettings.() -> Unit) = inspect.apply(configure)

    /** Configures the Terrain Paint tool. */
    fun paint(configure: DebugPaintToolSettings.() -> Unit) = paint.apply(configure)

    /** Configures the Delete tool. */
    fun delete(configure: DebugDeleteToolSettings.() -> Unit) = delete.apply(configure)

    /** Configures the Pathfinding tool. */
    fun pathfinding(configure: DebugPathfindingSettings.() -> Unit) = pathfinding.apply(configure)

    /** Called after an entity was created by the Spawn tool. */
    fun onEntitySpawned(callback: (WorldEntity) -> Unit) {
        entitySpawnedCallback = callback
    }

    /** Called after a Build drag placed one or more objects. */
    fun onObjectsPlaced(callback: (List<PlacedObject>) -> Unit) {
        objectsPlacedCallback = callback
    }

    /** Supplies optional game traversal rules for pathfinding tool searches. */
    fun pathTraversal(canEnter: (World, TilePosition) -> Boolean) {
        pathTraversal = canEnter
    }

    /** Supplies optional game movement costs for pathfinding tool searches. */
    fun pathCost(cost: (World, from: TilePosition, to: TilePosition) -> Float) {
        pathCost = cost
    }
}

/** Configures Debug services whose behavior extends beyond visual rendering. */
class DebugOperationalSettings internal constructor() {
    /** Performance collection, overlay, history, and terminal logging. */
    val performance = DebugPerformanceSettings()

    /** Simulation control availability and animation-freeze behavior. */
    val simulation = DebugSimulationSettings()

    /** World statistics collection and overlay availability. */
    val worldStats = DebugWorldStatsSettings()

    /** Event Bus monitoring and presentation. */
    val eventBus = DebugEventMonitorSettings()

    /** Developer notification delivery and presentation. */
    val notifications = DebugNotifications()

    /** Whether Debug controls bypass the game's configured camera restrictions. */
    var disableCameraRestrictions: Boolean = false

    /** Configures performance collection and output. */
    fun performance(configure: DebugPerformanceSettings.() -> Unit) = performance.apply(configure)

    /** Configures simulation controls. */
    fun simulation(configure: DebugSimulationSettings.() -> Unit) = simulation.apply(configure)

    /** Configures world statistics collection and display. */
    fun worldStats(configure: DebugWorldStatsSettings.() -> Unit) = worldStats.apply(configure)

    /** Configures Event Bus monitoring. */
    fun eventBus(configure: DebugEventMonitorSettings.() -> Unit) = eventBus.apply(configure)

    /** Configures developer notifications. */
    fun notifications(configure: DebugNotifications.() -> Unit) = notifications.apply(configure)
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

    /** Whether the affected-area preview draws an outline around each tile. */
    var showTileBorders: Boolean = false
}

/** Brush options for the terrain Paint tool. */
class DebugPaintToolSettings : DebugBrushSettings()

/** Brush and stroke options for the Delete tool. */
class DebugDeleteToolSettings : DebugBrushSettings() {
    /** Whether holding the left mouse button continues deletion along cursor movement. */
    var dragEnabled: Boolean = true
}

/** Base configuration for diagnostics with a meaningful activation lifecycle. */
open class DebugFeatureSettings {
    /** Whether the diagnostic system is active. */
    var enabled: Boolean = false
}

/** Activation configuration for the world statistics overlay. */
class DebugWorldStatsSettings : DebugFeatureSettings()

/** Availability and presentation options for simulation controls. */
class DebugSimulationSettings : DebugFeatureSettings() {
    /** Stops visual animation clocks while simulation continues normally. */
    var freezeVisualAnimations: Boolean = false
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

/** Search and visualization options for the Pathfinding tool. */
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

/** Activation, capture, and presentation options for Event Bus monitoring. */
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
