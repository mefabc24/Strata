package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.mefabc24.strata.debug.*
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.ui.*
import com.mefabc24.strata.world.World

internal data class DebugSettingsWindowUi(
    val pickingRows: DebugDiagnosticTable,
    val cameraRows: DebugDiagnosticTable,
    val cullingRows: DebugDiagnosticTable
)

/** Builds Debug Window sections and binds controls to DebugSettings. */
internal class DebugSettingsWindowBuilder(
    private val ui: StrataUi,
    private val settings: DebugSettings,
    private val simulation: SimulationController,
    private val eventMonitor: DebugEventMonitor,
    private val world: World,
    private val synchronizers: DebugControlBindings
) {
    private lateinit var pickingRows: DebugDiagnosticTable
    private lateinit var cameraRows: DebugDiagnosticTable
    private lateinit var cullingRows: DebugDiagnosticTable

    fun build(column: StrataColumn): DebugSettingsWindowUi {
        column.buildDebug()
        return DebugSettingsWindowUi(pickingRows, cameraRows, cullingRows)
    }

    private fun StrataColumn.buildDebug() {
        val groups = DebugHierarchyGroup.entries.associateWith { group ->
            settingsExpander(group.label) {}.content
        }
        val general = groups.getValue(DebugHierarchyGroup.GENERAL)
        val worldSettings = groups.getValue(DebugHierarchyGroup.WORLD)
        val sceneElements = groups.getValue(DebugHierarchyGroup.SCENE_ELEMENTS)
        val cameraSettings = groups.getValue(DebugHierarchyGroup.CAMERA)
        val performance = groups.getValue(DebugHierarchyGroup.PERFORMANCE)
        val simulationSettings = groups.getValue(DebugHierarchyGroup.SIMULATION)
        val diagnostics = groups.getValue(DebugHierarchyGroup.DIAGNOSTICS)
        val presets = groups.getValue(DebugHierarchyGroup.PRESETS)

        var selectedPreset = DebugPresetSelection.DEFAULT
        presets.row(
            spacing = 6f,
            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 3f)
        ) {
            label("Preset").cell { growX(); left() }
            dropdown(
                DebugPresetSelection.entries,
                selectedPreset,
                displayText = DebugPresetSelection::label
            ) { selectedPreset = it }.cell { width(142f); height(28f) }
            button("Apply") {
                selectedPreset.applyTo(settings)
                syncControls()
            }.cell { width(58f); height(28f) }
        }.applyDebugSettingBackground().cell { fillAvailableX(); height(34f) }
        presets.separator()
        presets.compactAction("Save current configuration as DEFAULT") {
            settings.saveDefaultVisualConfiguration()
            settings.notify(
                "Default debug configuration saved",
                DebugNotificationSeverity.SUCCESS
            )
        }

        general.settingsExpander("Target filter") {
            boundDropdown(
                "Visualization filter",
                DebugVisualizationFilter.entries,
                { settings.visualizationFilter },
                { it.name.toDisplayName() }
            ) { settings.visualizationFilter = it }
        }

        performance.settingsExpander("Overlays") {
                toggleRows(
                    toggle("Performance overlay", { settings.performance.overlayEnabled }) {
                        settings.performance.overlayEnabled = it
                    },
                    toggle("World stats overlay", { settings.worldStats.enabled }) {
                        settings.worldStats.enabled = it
                    }
                )
        }
        performance.settingsExpander("History & logging") {
                toggleRows(
                    toggle("Terminal logging", { settings.performance.terminalLoggingEnabled }) {
                        settings.performance.terminalLoggingEnabled = it
                    }
                )
                boundStepper(
                    "Terminal interval",
                    { settings.performance.terminalLoggingIntervalSeconds },
                    0.25f,
                    30f,
                    0.25f
                ) { settings.performance.terminalLoggingIntervalSeconds = it }
                boundStepper(
                    "History length",
                    { settings.performance.historyLength.toFloat() },
                    30f,
                    2000f,
                    10f
                ) { settings.performance.historyLength = it.toInt() }
                boundDropdown(
                    "Graph metric",
                    DebugPerformanceMetric.entries,
                    { settings.performance.historyMetric },
                    { it.name.toDisplayName() }
                ) { settings.performance.historyMetric = it }
                compactActions(
                    "Start" to { settings.performance.startHistoryRecording() },
                    "Stop" to { settings.performance.stopHistoryRecording() },
                    "Clear" to { settings.performance.clearHistory() }
                )
        }
        simulationSettings.settingToggleRow(
            "Simulation enabled",
            { settings.simulation.enabled },
            { settings.simulation.enabled = it }
        )
        simulationSettings.boundStepper(
                "Custom time scale",
                { simulation.timeScale },
                0.05f,
                16f,
                0.05f
            ) { simulation.timeScale = it }
        simulationSettings.simpleToggle(
                "Freeze visual animations",
                { settings.simulation.freezeVisualAnimations }
            ) { settings.simulation.freezeVisualAnimations = it }
        simulationSettings.compactAction("Reset to normal speed") {
                simulation.resetTimeScale()
        }
        worldSettings.settingsExpander("Visibility") {
                buildWorldVisibilitySettings()
        }
        worldSettings.featureExpander(
                "Grid",
                { settings.grid.enabled },
                { settings.grid.enabled = it },
                enabledLabel = "Grid visible"
            ) { buildGridSettings() }
        worldSettings.settingsExpander(
                "Information"
            ) { buildWorldInfoSettings() }
        worldSettings.settingsExpander("Rendering") {
            settingsExpander("Render order") {
            boundDropdown(
                "Order",
                RenderOrderDebugMode.entries,
                { settings.renderOrder.mode },
                { it.name.toDisplayName() }
            ) { settings.renderOrder.mode = it }
            toggleRows(
                toggle("Order labels", { settings.renderOrder.showLabels }) {
                    settings.renderOrder.showLabels = it
                },
                toggle("Priority labels", { settings.renderOrder.showPriorityLabels }) {
                    settings.renderOrder.showPriorityLabels = it
                },
                toggle("Priority colors", { settings.renderOrder.colorByPriority }) {
                    settings.renderOrder.colorByPriority = it
                },
                toggle("Terrain indices", { settings.renderOrder.showTerrainIndices }) {
                    settings.renderOrder.showTerrainIndices = it
                },
                toggle("Terrain heatmap", { settings.renderOrder.showTerrainHeatmap }) {
                    settings.renderOrder.showTerrainHeatmap = it
                }
            )
            toggleRows(
                toggle("Sort volumes", { settings.renderOrder.showSortVolumes }) {
                    settings.renderOrder.showSortVolumes = it
                },
                toggle("Sort anchors", { settings.renderOrder.showSortAnchors }) {
                    settings.renderOrder.showSortAnchors = it
                },
                toggle(
                    "Projected positions",
                    { settings.renderOrder.showProjectedSortPositions }
                ) {
                    settings.renderOrder.showProjectedSortPositions = it
                }
            )
            boundStepper(
                "Geometry line width",
                { settings.renderOrder.sortGeometryLineWidth },
                0.25f,
                8f,
                0.25f
            ) { settings.renderOrder.sortGeometryLineWidth = it }
            boundDropdown(
                "Priority focus",
                RenderPriorityFocusMode.entries,
                { settings.renderOrder.priorityFocusMode },
                { it.name.toDisplayName() }
            ) { settings.renderOrder.priorityFocusMode = it }
            boundStepper(
                "Selected priority",
                { settings.renderOrder.selectedPriority.toFloat() },
                -100f,
                100f,
                1f
            ) { settings.renderOrder.selectedPriority = it.toInt() }
            boundStepper(
                "Priority color alpha",
                { settings.renderOrder.priorityColorAlpha },
                0f,
                1f,
                0.05f
            ) { settings.renderOrder.priorityColorAlpha = it }
            boundDropdown(
                "Heatmap color steps",
                TerrainHeatmapSteps.entries,
                { settings.renderOrder.terrainHeatmapSteps },
                { it.name.toDisplayName() }
            ) { settings.renderOrder.terrainHeatmapSteps = it }
            }
        }
        diagnostics.featureExpander(
            "Event Bus Monitor",
            { settings.eventBus.enabled },
            { settings.eventBus.enabled = it },
            enabledLabel = "Monitor visible"
        ) {
            toggleRows(
                toggle(
                    "Capture",
                    { settings.eventBus.captureEnabled }
                ) {
                    settings.eventBus.captureEnabled = it
                },
                toggle(
                    "Newest first",
                    { settings.eventBus.newestFirst }
                ) {
                    settings.eventBus.newestFirst = it
                }
            )
            boundStepper(
                "Visible records",
                { settings.eventBus.maximumVisibleRecords.toFloat() },
                1f,
                25f,
                1f
            ) { settings.eventBus.maximumVisibleRecords = it.toInt() }
            compactAction("Clear history") {
                if (eventMonitor.records.isNotEmpty()) {
                    eventMonitor.clear()
                    settings.notify("Event history cleared", DebugNotificationSeverity.INFO)
                }
            }
        }
        sceneElements.settingsExpander(
                "Objects"
            ) { buildObjectSettings() }
        sceneElements.settingsExpander(
                "Entities"
            ) { buildEntitySettings() }
        diagnostics.settingsExpander("Picking") {
            toggleRows(
                toggle("Sprite bounds", { settings.picking.showSpriteBounds }) { settings.picking.showSpriteBounds = it },
                toggle("Cursor marker", { settings.picking.showCursorHit }) { settings.picking.showCursorHit = it }
            )
            pickingRows = diagnosticTable()
            compactAction("Clear locked target") {
                settings.worldState.pickingSelection.clear()
            }
        }
        diagnostics.settingsExpander("Culling") {
            toggleRows(
                toggle("Render check area", { settings.culling.showVisibleArea }) {
                    settings.culling.showVisibleArea = it
                },
                toggle("Object culling bounds", { settings.culling.showObjectBounds }) {
                    settings.culling.showObjectBounds = it
                },
                toggle("Entity culling bounds", { settings.culling.showEntityBounds }) {
                    settings.culling.showEntityBounds = it
                }
            )
            cullingRows = diagnosticTable()
        }
        cameraSettings.apply {
            toggleRows(
                toggle("Visible area", { settings.camera.showVisibleArea }) { settings.camera.showVisibleArea = it },
                toggle("World bounds", { settings.camera.showWorldBounds }) { settings.camera.showWorldBounds = it },
                toggle("Clamp bounds", { settings.camera.showClampBounds }) { settings.camera.showClampBounds = it }
            )
            simpleToggle(
                "Disable restrictions",
                { settings.camera.disableRestrictions }
            ) {
                settings.camera.disableRestrictions = it
                settings.notify(
                    if (it) "Camera restrictions disabled" else "Camera restrictions enabled",
                    DebugNotificationSeverity.INFO
                )
            }
            cameraRows = diagnosticTable()
        }

        general.featureExpander(
            "Notifications",
            { settings.notifications.enabled },
            { settings.notifications.enabled = it },
            enabledLabel = "Notifications enabled"
        ) {
            boundDropdown(
                "Position",
                DebugNotificationPosition.entries,
                { settings.notifications.position },
                { it.name.toDisplayName() }
            ) { settings.notifications.position = it }
        }
    }

    private fun StrataColumn.buildGridSettings() {
        boundDropdown(
            "Layer",
            DebugGridRenderLayer.entries,
            { settings.grid.renderLayer },
            { it.name.toDisplayName() }
        ) { settings.grid.renderLayer = it }
        boundDropdown(
            "Extent",
            DebugGridExtent.entries,
            { settings.grid.extent },
            { it.name.toDisplayName() }
        ) { settings.grid.extent = it }
        toggleRows(
            toggle("Background", { settings.grid.backgroundColor != null }) { enabled ->
                settings.grid.backgroundColor = if (enabled) Color(1f, 1f, 1f, 0.2f) else null
            },
            toggle("Hover background", { settings.grid.hoverBackgroundColor != null }) { enabled ->
                settings.grid.hoverBackgroundColor = if (enabled) Color(1f, 0f, 0f, 0.5f) else null
            }
        )
        boundStepper("Line width", { settings.grid.lineWidth }, 0.25f, 8f, 0.25f) {
            settings.grid.lineWidth = it
        }
        boundColorAlpha("Grid color", { settings.grid.color }, 0.2f) { alpha ->
            settings.grid.color = settings.grid.color.apply { a = alpha }
        }
        boundColorAlpha("Hover color", { settings.grid.hoverColor }, 0.5f) { alpha ->
            settings.grid.hoverColor = settings.grid.hoverColor.apply { a = alpha }
        }
        boundColorAlpha("Background color", { settings.grid.backgroundColor }, 0.2f) { alpha ->
            settings.grid.backgroundColor?.let { settings.grid.backgroundColor = it.apply { a = alpha } }
        }
        boundColorAlpha("Hover background", { settings.grid.hoverBackgroundColor }, 0.5f) { alpha ->
            settings.grid.hoverBackgroundColor?.let { settings.grid.hoverBackgroundColor = it.apply { a = alpha } }
        }
    }

    private fun StrataColumn.buildObjectSettings() {
        toggleRows(
            toggle("Occupied tiles", { settings.objects.showOccupiedTiles }) {
                settings.objects.showOccupiedTiles = it
            },
            toggle("Origin tile", { settings.objects.showOriginTile }) {
                settings.objects.showOriginTile = it
            },
            toggle("Sprite bounds", { settings.objects.showSpriteBounds }) {
                settings.objects.showSpriteBounds = it
            },
            toggle("Tile fill", { settings.objects.occupiedTileFillColor != null }) { enabled ->
                settings.objects.occupiedTileFillColor =
                    if (enabled) Color(0.2f, 0.65f, 1f, 0.18f) else null
            }
        )
        boundStepper("Line width", { settings.objects.lineWidth }, 0.25f, 8f, 0.25f) {
            settings.objects.lineWidth = it
        }
        boundColorAlpha("Fill color", { settings.objects.occupiedTileFillColor }, 0.18f) { alpha ->
            settings.objects.occupiedTileFillColor?.let {
                settings.objects.occupiedTileFillColor = it.apply { a = alpha }
            }
        }
    }

    private fun StrataColumn.buildEntitySettings() {
        toggleRows(
            toggle("Current tile", { settings.entities.showCurrentTile }) { settings.entities.showCurrentTile = it },
            toggle("Position", { settings.entities.showPosition }) { settings.entities.showPosition = it },
            toggle("Direction", { settings.entities.showDirection }) { settings.entities.showDirection = it },
            toggle("Sprite bounds", { settings.entities.showSpriteBounds }) { settings.entities.showSpriteBounds = it },
            toggle("Tile fill", { settings.entities.currentTileFillColor != null }) { enabled ->
                settings.entities.currentTileFillColor =
                    if (enabled) Color(0.3f, 1f, 0.3f, 0.16f) else null
            }
        )
        boundStepper("Line width", { settings.entities.lineWidth }, 0.25f, 8f, 0.25f) {
            settings.entities.lineWidth = it
        }
        boundColorAlpha("Fill color", { settings.entities.currentTileFillColor }, 0.16f) { alpha ->
            settings.entities.currentTileFillColor?.let {
                settings.entities.currentTileFillColor = it.apply { a = alpha }
            }
        }
        settingsExpander("Movement") {
            toggleRows(
            toggle("Path", { settings.entities.showPath }) { settings.entities.showPath = it },
            toggle("Movement trail", { settings.entities.showMovementTrail }) {
                settings.entities.showMovementTrail = it
            },
            toggle("Movement vector", { settings.entities.showMovementVector }) {
                settings.entities.showMovementVector = it
            },
            toggle("Next waypoint", { settings.entities.showNextWaypoint }) {
                settings.entities.showNextWaypoint = it
            },
            toggle("Speed labels", { settings.entities.showMovementSpeed }) {
                settings.entities.showMovementSpeed = it
            },
            toggle("Tile-position offset", { settings.entities.showPositionTileOffset }) {
                settings.entities.showPositionTileOffset = it
            }
        )
        boundStepper(
            "Maximum positions",
            { settings.entities.trailMaxPositions.toFloat() },
            2f,
            1000f,
            10f
        ) { settings.entities.trailMaxPositions = it.toInt() }
        boundStepper(
            "History seconds",
            { settings.entities.trailHistoryDurationSeconds },
            0.25f,
            60f,
            0.25f
        ) { settings.entities.trailHistoryDurationSeconds = it }
        boundStepper(
            "Sample distance",
            { settings.entities.trailMinimumDistance },
            0f,
            1f,
            0.01f
        ) { settings.entities.trailMinimumDistance = it }
        boundStepper(
            "Trail opacity",
            { settings.entities.trailOpacity },
            0f,
            1f,
            0.05f
        ) { settings.entities.trailOpacity = it }
        boundStepper(
            "Vector seconds",
            { settings.entities.movementVectorScaleSeconds },
            0.1f,
            5f,
            0.1f
        ) { settings.entities.movementVectorScaleSeconds = it }
        compactAction("Clear movement trails") {
            settings.entities.clearMovementTrails()
        }
        }
    }

    private fun StrataColumn.buildWorldInfoSettings() {
        toggleRows(
            toggle("Tile coordinates", { settings.worldInfo.showTileCoordinates }) {
                settings.worldInfo.showTileCoordinates = it
            },
            toggle("Terrain IDs", { settings.worldInfo.showTerrainIds }) {
                settings.worldInfo.showTerrainIds = it
            },
            toggle("Overlay info", { settings.worldInfo.showOverlayInfo }) {
                settings.worldInfo.showOverlayInfo = it
            },
            toggle("Occupancy", { settings.worldInfo.showOccupancy }) {
                settings.worldInfo.showOccupancy = it
            },
            toggle("Missing visuals", { settings.worldInfo.showMissingTerrainVisuals }) {
                settings.worldInfo.showMissingTerrainVisuals = it
            },
            toggle("World origin", { settings.worldInfo.showOrigin }) {
                settings.worldInfo.showOrigin = it
            }
        )
        boundStepper(
            "Maximum label zoom",
            { settings.worldInfo.maximumLabelZoom },
            0.25f,
            8f,
            0.25f
        ) { settings.worldInfo.maximumLabelZoom = it }
        boundStepper(
            "Maximum labels",
            { settings.worldInfo.maximumVisibleLabels.toFloat() },
            16f,
            1024f,
            16f
        ) { settings.worldInfo.maximumVisibleLabels = it.toInt() }
        boundColorAlpha("Occupancy color", { settings.worldInfo.occupancyColor }, 0.5f) { alpha ->
            settings.worldInfo.occupancyColor = settings.worldInfo.occupancyColor.apply { a = alpha }
        }
        boundColorAlpha("Missing visual color", { settings.worldInfo.missingVisualColor }, 0.5f) { alpha ->
            settings.worldInfo.missingVisualColor =
                settings.worldInfo.missingVisualColor.apply { a = alpha }
        }
    }

    private fun StrataColumn.buildWorldVisibilitySettings() {
        toggleRows(
            toggle("Ground terrain", { settings.worldVisibility.groundTerrainVisible }) {
                settings.worldVisibility.groundTerrainVisible = it
            },
            toggle("Terrain overlays", { settings.worldVisibility.terrainOverlaysVisible }) {
                settings.worldVisibility.terrainOverlaysVisible = it
            },
            toggle("Placed objects", { settings.worldVisibility.placedObjectsVisible }) {
                settings.worldVisibility.placedObjectsVisible = it
            },
            toggle("Entities", { settings.worldVisibility.entitiesVisible }) {
                settings.worldVisibility.entitiesVisible = it
            }
        )
        val overlayIds = world.overlayLayerIds
        if (overlayIds.isNotEmpty()) {
            settingsExpander("Overlay layers") {
                overlayIds.forEach { layerId ->
                    simpleToggle(
                        layerId,
                        { settings.worldVisibility.isOverlayLayerVisible(layerId) }
                    ) { visible ->
                        settings.worldVisibility.setOverlayLayerVisible(layerId, visible)
                    }
                }
            }
        }
        compactAction("Show all categories") {
            settings.worldVisibility.showAll()
        }
    }

    private data class ToggleBinding(
        val text: String,
        val read: () -> Boolean,
        val write: (Boolean) -> Unit
    )

    private fun toggle(
        text: String,
        read: () -> Boolean,
        write: (Boolean) -> Unit
    ) = ToggleBinding(text, read, write)

    private fun StrataColumn.simpleToggle(
        text: String,
        read: () -> Boolean,
        write: (Boolean) -> Unit
    ) = settingToggleRow(text, read, write)

    private fun StrataColumn.featureExpander(
        title: String,
        read: () -> Boolean,
        write: (Boolean) -> Unit,
        enabledLabel: String = "$title enabled",
        configure: StrataColumn.() -> Unit
    ) {
        settingsExpander(title) {
            settingToggleRow(enabledLabel, read, write)
            configure()
        }
    }

    private fun StrataColumn.settingsExpander(
        title: String,
        expanded: Boolean = false,
        configure: StrataColumn.() -> Unit
    ): StrataExpander {
        val expander = expander(
            title = title,
            expanded = expanded,
            spacing = 0f,
            headerHeight = 34f,
            expandedStyle = ui.skin.get("debug-expander", StrataExpanderStyle::class.java)
        ) {
            defaults().fillAvailableX()
            configure()
        }
        getCell(expander).fillAvailableX()
        return expander
    }

    private fun StrataColumn.toggleRows(vararg controls: ToggleBinding) {
        controls.forEach { settingToggleRow(it.text, it.read, it.write) }
    }

    private fun StrataColumn.settingToggleRow(
        text: String,
        read: () -> Boolean,
        write: (Boolean) -> Unit
    ) {
        row(
            spacing = 6f,
            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 2f)
        ) {
            label(text).cell { growX(); left() }
            settingToggle(read, write).cell { width(56f); height(26f) }
        }.applyDebugSettingBackground().cell { fillAvailableX(); height(32f) }
        separator()
    }

    private fun StrataColumn.compactAction(text: String, onClick: () -> Unit) {
        compactActions(text to onClick)
    }

    private fun StrataColumn.compactActions(
        vararg actions: Pair<String, () -> Unit>
    ) {
        row(spacing = 0f, padding = StrataInsets.NONE) {
            actions.forEachIndexed { index, (label, action) ->
                if (index > 0) {
                    actor(StrataSeparator(
                        StrataSeparatorOrientation.VERTICAL,
                        ui.skin.get("debug-separator", StrataSeparatorStyle::class.java)
                    )).cell { width(2f); height(32f) }
                }
                button(label, "debug-action-card", action).cell { growX(); height(32f) }
            }
        }.cell { fillAvailableX(); height(32f) }
        separator()
    }

    private fun <T> StrataColumn.boundDropdown(
        text: String,
        options: Iterable<T>,
        read: () -> T,
        displayText: (T) -> String = { it.toString() },
        write: (T) -> Unit
    ): StrataDropdown<T> {
        lateinit var dropdown: StrataDropdown<T>
        row(
            spacing = 6f,
            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 2f)
        ) {
            label(text).cell { growX(); left() }
            dropdown = dropdown(
                options = options,
                selected = read(),
                displayText = displayText,
                onChanged = write
            ).cell { width(148f); height(28f) }
        }.applyDebugSettingBackground().cell { fillAvailableX(); height(32f) }
        separator()
        synchronizers += { dropdown.sync(read()) }
        return dropdown
    }

    private fun StrataLayout.settingToggle(
        read: () -> Boolean,
        write: (Boolean) -> Unit
    ): StrataToggleButton {
        val button = toggleButton(if (read()) "ON" else "OFF", read(), onChanged = write)
        synchronizers += {
            val checked = read()
            button.syncChecked(checked)
            button.setText(if (checked) "ON" else "OFF")
        }
        return button
    }

    private fun StrataColumn.boundStepper(
        text: String,
        read: () -> Float,
        minimum: Float,
        maximum: Float,
        step: Float,
        write: (Float) -> Unit
    ): StrataNumericStepper {
        val stepper = numericStepper(
            text, read(), minimum, maximum, step,
            valueStyleName = "debug-stepper-value",
            onChanged = write
        ).applyDebugSettingBackground().apply {
            padLeft(6f)
            padRight(6f)
        }.cell { fillAvailableX(); height(32f) }
        separator()
        synchronizers += { stepper.sync(read()) }
        return stepper
    }

    private fun StrataColumn.boundColorAlpha(
        text: String,
        read: () -> Color?,
        defaultAlpha: Float,
        write: (Float) -> Unit
    ): StrataNumericStepper {
        lateinit var preview: Image
        lateinit var stepper: StrataNumericStepper
        var displayedColor = Int.MIN_VALUE
        row(
            spacing = 6f,
            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 2f)
        ) {
            label(text).cell { growX(); left() }
            preview = actor(Image()).cell { width(24f); height(20f) }
            stepper = numericStepper(
                "", read()?.a ?: defaultAlpha, 0f, 1f, 0.05f,
                valueStyleName = "debug-stepper-value",
                onChanged = write
            ).cell { width(120f); height(28f) }
        }.applyDebugSettingBackground().cell { fillAvailableX(); height(32f) }
        separator()
        synchronizers += {
            val color = read()
            stepper.sync(color?.a ?: defaultAlpha)
            val previewColor = color ?: DISABLED_COLOR_PREVIEW
            val packed = Color.rgba8888(previewColor)
            if (packed != displayedColor) {
                preview.drawable = ui.skin.newDrawable("debug-white", previewColor)
                displayedColor = packed
            }
        }
        return stepper
    }

    private fun StrataLayout.diagnosticTable(): DebugDiagnosticTable {
        val table = actor(DebugDiagnosticTable(ui.skin))
        table.applyDebugSettingBackground()
        val cell = requireNotNull(getCell(table)).apply { fillAvailableX() }
        table.bindLayout(cell)
        return table
    }

    private fun <T : Table> T.applyDebugSettingBackground(): T = apply {
        background = ui.skin.get("debug-setting-row", StrataPanelStyle::class.java).background
    }

    private fun syncControls() {
        synchronizers.sync()
    }

    private fun String.toDisplayName() = replace('_', ' ').replace('-', ' ')
        .lowercase().replaceFirstChar(Char::titlecase)

    private companion object {
        val DISABLED_COLOR_PREVIEW = Color(0.12f, 0.12f, 0.14f, 1f)
    }
}
