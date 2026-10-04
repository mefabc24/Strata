package com.mefabc24.strata.debug

import com.badlogic.gdx.Input
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity
/** Built-in debug panel and world diagnostic configuration. */
class DebugSettings {
    internal val worldState = DebugWorldState()
    internal val entityFreezeState = DebugEntityFreezeState()

    /** Tool Rail availability, startup visibility, and shortcut. */
    val toolsWindow = DebugWindowSettings(defaultToggleKey = Input.Keys.F2)

    /** Debug Window availability, startup visibility, and shortcut. */
    val debugWindow = DebugWindowSettings(defaultToggleKey = Input.Keys.F3)

    /** Performance overlay, history, and terminal logging configuration. */
    val performance = DebugPerformanceSettings()

    /** Simulation control overlay configuration. */
    val simulation = DebugSimulationSettings()

    /** Isometric grid visualization configuration. */
    val grid = DebugGridSettings()

    /** World tile information visualization configuration. */
    val worldInfo = DebugWorldInfoSettings()

    /** Debug-only world category visibility. */
    val worldVisibility = DebugWorldVisibilitySettings()

    /** Placed-object visualization configuration. */
    val objects = DebugObjectSettings()

    /** Entity visualization configuration. */
    val entities = DebugEntitySettings()

    /** Picking visualization configuration. */
    val picking = DebugPickingSettings()

    /** Render-order visualization configuration. */
    val renderOrder = DebugRenderOrderSettings()

    /** Culling visualization configuration. */
    val culling = DebugCullingSettings()

    /** Camera visualization and debug override configuration. */
    val camera = DebugCameraSettings()

    /** World statistics overlay configuration. */
    val worldStats = DebugWorldStatsSettings()

    /** Inspect tool visualization configuration. */
    val inspect = DebugInspectSettings()

    /** Terrain Paint tool configuration. */
    val paint = DebugPaintToolSettings()

    /** Delete tool configuration. */
    val delete = DebugDeleteToolSettings()

    /** Pathfinding tool configuration. */
    val pathfinding = DebugPathfindingSettings()

    /** Event Bus monitor configuration. */
    val eventBus = DebugEventMonitorSettings()

    /** Developer notification configuration. */
    val notifications = DebugNotifications()

    /** DEFAULT preset persistence and startup behavior. */
    val presets = DebugPresetSettings()

    /** Target subset shared by object, entity, culling, and render-order visuals. */
    var visualizationFilter: DebugVisualizationFilter = DebugVisualizationFilter.ALL

    /** Currently stored default visual configuration. */
    private var defaultVisualConfiguration: DebugVisualConfiguration =
        captureVisualConfiguration()

    internal var entitySpawnedCallback: ((WorldEntity) -> Unit)? = null
    internal var objectsPlacedCallback: ((List<PlacedObject>) -> Unit)? = null
    internal var pathTraversal: ((World, TilePosition) -> Boolean)? = null
    internal var pathCost: ((World, TilePosition, TilePosition) -> Float)? = null

    fun toolsWindow(configure: DebugWindowSettings.() -> Unit) = toolsWindow.apply(configure)
    fun debugWindow(configure: DebugWindowSettings.() -> Unit) = debugWindow.apply(configure)
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
    fun worldStats(configure: DebugWorldStatsSettings.() -> Unit) = worldStats.apply(configure)
    fun inspect(configure: DebugInspectSettings.() -> Unit) = inspect.apply(configure)
    fun paint(configure: DebugPaintToolSettings.() -> Unit) = paint.apply(configure)
    fun delete(configure: DebugDeleteToolSettings.() -> Unit) = delete.apply(configure)
    fun pathfinding(configure: DebugPathfindingSettings.() -> Unit) = pathfinding.apply(configure)
    fun eventBus(configure: DebugEventMonitorSettings.() -> Unit) = eventBus.apply(configure)
    fun presets(configure: DebugPresetSettings.() -> Unit) = presets.apply(configure)

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

    /**
     * Configures persistent storage for the default visual preset.
     *
     * The game owns the preferences namespace.
     */
    @Deprecated(
        message = "Configure debug preset persistence through presets { storage(...) }.",
        replaceWith = ReplaceWith("presets { storage(preferencesName) }")
    )
    fun defaultPresetStorage(preferencesName: String) {
        presets.storage(preferencesName)
    }

    /**
     * Initializes the default preset after scene configuration is complete.
     */
    internal fun initializeDefaultVisualConfiguration() {
        val configuredDefault = captureVisualConfiguration()
        val savedDefault = presets.loadDefault()
        defaultVisualConfiguration = savedDefault ?: configuredDefault
        if (savedDefault != null && presets.applySavedDefaultOnStartup) {
            applyVisualConfiguration(savedDefault)
        }
    }

    /** Saves the current visual configuration as the default preset. */
    fun saveDefaultVisualConfiguration() {
        val configuration = captureVisualConfiguration()

        presets.saveDefault(configuration)
        defaultVisualConfiguration = configuration
    }

    /** Applies the last saved default visual configuration. */
    fun applyDefaultVisualConfiguration() {
        applyVisualConfiguration(defaultVisualConfiguration)
    }

    /** Restores every visual diagnostic to its engine default. */
    fun resetVisualConfiguration() {
        DebugVisualSettings.reset(this)
    }

    /** Restores one visual diagnostic category to its engine default. */
    fun resetVisualCategory(category: DebugVisualCategory) {
        DebugVisualSettings.reset(this, category)
    }

    internal fun validateWindowConfiguration() {
        val enabled = listOf(toolsWindow, debugWindow).filter(DebugWindowSettings::enabled)
        if (enabled.size < 2) return

        require(toolsWindow.toggleKey != debugWindow.toggleKey) {
            "Enabled debug windows must use different toggle keys; both use ${toolsWindow.toggleKey}."
        }
    }
}
