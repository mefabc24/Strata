package com.mefabc24.strata.debug

import com.mefabc24.strata.world.WorldEntity

/**
 * Configures the built-in Debug System for one scene.
 *
 * The game-defined configuration is applied over engine defaults. When [presets]
 * has storage and automatic startup loading enabled, a saved DEFAULT visual
 * configuration is applied last. Presets never restore UI, tool, or operational state.
 */
class DebugSettings {
    /** Debug user-interface availability, startup visibility, and shortcuts. */
    val ui = DebugUiSettings()

    /** Visual diagnostics rendered over the game world. */
    val visuals = DebugVisualSettings()

    /** Interactive developer tools and their game integration callbacks. */
    val tools = DebugToolSettings()

    /** Performance, simulation, monitoring, notification, and runtime overrides. */
    val operations = DebugOperationalSettings()

    /** DEFAULT visual preset persistence and startup behavior. */
    val presets = DebugPresetSettings()

    internal val worldState = DebugWorldState()
    internal val entityFreezeState = DebugEntityFreezeState()

    /** Configures Debug user interfaces without enabling or disabling Debug features. */
    fun ui(configure: DebugUiSettings.() -> Unit) = ui.apply(configure)

    /** Configures the game's initial visual diagnostics. */
    fun visuals(configure: DebugVisualSettings.() -> Unit) = visuals.apply(configure)

    /** Configures interactive developer tools. */
    fun tools(configure: DebugToolSettings.() -> Unit) = tools.apply(configure)

    /** Configures operational Debug services and runtime overrides. */
    fun operations(configure: DebugOperationalSettings.() -> Unit) = operations.apply(configure)

    /** Configures DEFAULT visual preset persistence and startup application. */
    fun presets(configure: DebugPresetSettings.() -> Unit) = presets.apply(configure)

    /** Currently stored default visual configuration. */
    private var defaultVisualConfiguration: DebugVisualConfiguration =
        captureVisualConfiguration()

    /** Returns whether debug controls have frozen engine movement for [entity]. */
    fun isEntityFrozen(entity: WorldEntity): Boolean = entityFreezeState.isFrozen(entity)

    /** Returns whether any debug tool currently holds [entity]. */
    fun isEntityHeld(entity: WorldEntity): Boolean = entityFreezeState.isHeld(entity)

    /** Freezes or unfreezes engine-owned movement for [entity]. */
    fun setEntityFrozen(entity: WorldEntity, frozen: Boolean) {
        entityFreezeState.setFrozen(entity, frozen)
    }

    /** Unfreezes every entity currently held by the Debug runtime. */
    fun unfreezeAllEntities(): Int = entityFreezeState.clear()

    /** Emits a short developer-facing notification. */
    fun notify(
        message: String,
        severity: DebugNotificationSeverity = DebugNotificationSeverity.INFO,
        durationSeconds: Float = operations.notifications.defaultDurationSeconds
    ) {
        operations.notifications.emit(message, severity, durationSeconds)
    }

    /** Applies a visual-only settings preset. */
    fun applyPreset(preset: DebugPreset) {
        DebugPresets.apply(this, preset)
    }

    /** Captures visual preferences without UI, tool, or operational runtime state. */
    fun captureVisualConfiguration(): DebugVisualConfiguration =
        DebugVisualConfigurationBindings.capture(this)

    /** Applies visual preferences without changing UI, tool, or operational runtime state. */
    fun applyVisualConfiguration(configuration: DebugVisualConfiguration) {
        DebugVisualConfigurationBindings.apply(this, configuration)
    }

    /** Initializes DEFAULT after the game-defined scene configuration is complete. */
    internal fun initializeDefaultVisualConfiguration() {
        val configuredDefault = captureVisualConfiguration()
        val savedDefault = presets.loadDefault()
        defaultVisualConfiguration = savedDefault ?: configuredDefault
        if (savedDefault != null && presets.applySavedDefaultOnStartup) {
            applyVisualConfiguration(savedDefault)
        }
    }

    /** Saves the current visual configuration as DEFAULT. */
    fun saveDefaultVisualConfiguration() {
        val configuration = captureVisualConfiguration()
        presets.saveDefault(configuration)
        defaultVisualConfiguration = configuration
    }

    /** Applies the last loaded or saved DEFAULT visual configuration. */
    fun applyDefaultVisualConfiguration() {
        applyVisualConfiguration(defaultVisualConfiguration)
    }

    /** Restores every visual preference to its engine default. */
    fun resetVisualConfiguration() {
        DebugVisualConfigurationBindings.reset(this)
    }

    /** Restores one visual preference category to its engine default. */
    fun resetVisualCategory(category: DebugVisualCategory) {
        DebugVisualConfigurationBindings.reset(this, category)
    }

    internal fun validateWindowConfiguration() {
        ui.validate()
    }
}

/** Groups visual diagnostics that can be captured by visual presets. */
class DebugVisualSettings internal constructor() {
    /** Target subset shared by object, entity, culling, and render-order visuals. */
    var filter: DebugVisualizationFilter = DebugVisualizationFilter.ALL

    /** Isometric world-grid visualization. */
    val grid = DebugGridSettings()

    /** Information drawn over world tiles. */
    val worldInfo = DebugWorldInfoSettings()

    /** Debug-only visibility of world rendering categories. */
    val worldVisibility = DebugWorldVisibilitySettings()

    /** Placed-object visual diagnostics. */
    val objects = DebugObjectSettings()

    /** Entity visual diagnostics. */
    val entities = DebugEntitySettings()

    /** Picking visual diagnostics. */
    val picking = DebugPickingSettings()

    /** Render-order visual diagnostics. */
    val renderOrder = DebugRenderOrderSettings()

    /** Renderer-culling visual diagnostics. */
    val culling = DebugCullingSettings()

    /** Camera-bound visual diagnostics. */
    val camera = DebugCameraSettings()

    /** Configures the world grid. */
    fun grid(configure: DebugGridSettings.() -> Unit) = grid.apply(configure)

    /** Configures information drawn over world tiles. */
    fun worldInfo(configure: DebugWorldInfoSettings.() -> Unit) = worldInfo.apply(configure)

    /** Configures debug-only world category visibility. */
    fun worldVisibility(configure: DebugWorldVisibilitySettings.() -> Unit) =
        worldVisibility.apply(configure)

    /** Configures placed-object visual diagnostics. */
    fun objects(configure: DebugObjectSettings.() -> Unit) = objects.apply(configure)

    /** Configures entity visual diagnostics. */
    fun entities(configure: DebugEntitySettings.() -> Unit) = entities.apply(configure)

    /** Configures picking visual diagnostics. */
    fun picking(configure: DebugPickingSettings.() -> Unit) = picking.apply(configure)

    /** Configures render-order visual diagnostics. */
    fun renderOrder(configure: DebugRenderOrderSettings.() -> Unit) = renderOrder.apply(configure)

    /** Configures culling visual diagnostics. */
    fun culling(configure: DebugCullingSettings.() -> Unit) = culling.apply(configure)

    /** Configures camera-bound visual diagnostics. */
    fun camera(configure: DebugCameraSettings.() -> Unit) = camera.apply(configure)
}
