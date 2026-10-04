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
                { settings.visuals.filter },
                { it.name.toDisplayName() }
            ) { settings.visuals.filter = it }
        }

        performance.settingsExpander("Overlays") {
                toggleRows(
                    toggle("Performance overlay", { settings.operations.performance.overlayEnabled }) {
                        settings.operations.performance.overlayEnabled = it
                    },
                    toggle("World stats overlay", { settings.operations.worldStats.enabled }) {
                        settings.operations.worldStats.enabled = it
                    }
                )
        }
        performance.settingsExpander("History & logging") {
                toggleRows(
                    toggle("Terminal logging", { settings.operations.performance.terminalLoggingEnabled }) {
                        settings.operations.performance.terminalLoggingEnabled = it
                    }
                )
                boundStepper(
                    "Terminal interval",
                    { settings.operations.performance.terminalLoggingIntervalSeconds },
                    0.25f,
                    30f,
                    0.25f
                ) { settings.operations.performance.terminalLoggingIntervalSeconds = it }
                boundStepper(
                    "History length",
                    { settings.operations.performance.historyLength.toFloat() },
                    30f,
                    2000f,
                    10f
                ) { settings.operations.performance.historyLength = it.toInt() }
                boundDropdown(
                    "Graph metric",
                    DebugPerformanceMetric.entries,
                    { settings.operations.performance.historyMetric },
                    { it.name.toDisplayName() }
                ) { settings.operations.performance.historyMetric = it }
                compactActions(
                    "Start" to { settings.operations.performance.startHistoryRecording() },
                    "Stop" to { settings.operations.performance.stopHistoryRecording() },
                    "Clear" to { settings.operations.performance.clearHistory() }
                )
        }
        simulationSettings.settingToggleRow(
            "Simulation enabled",
            { settings.operations.simulation.enabled },
            { settings.operations.simulation.enabled = it }
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
                { settings.operations.simulation.freezeVisualAnimations }
            ) { settings.operations.simulation.freezeVisualAnimations = it }
        simulationSettings.compactAction("Reset to normal speed") {
                simulation.resetTimeScale()
        }
        worldSettings.settingsExpander("Visibility") {
                buildWorldVisibilitySettings()
        }
        worldSettings.featureExpander(
                "Grid",
                { settings.visuals.grid.enabled },
                { settings.visuals.grid.enabled = it },
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
                { settings.visuals.renderOrder.mode },
                { it.name.toDisplayName() }
            ) { settings.visuals.renderOrder.mode = it }
            toggleRows(
                toggle("Order labels", { settings.visuals.renderOrder.showLabels }) {
                    settings.visuals.renderOrder.showLabels = it
                },
                toggle("Priority labels", { settings.visuals.renderOrder.showPriorityLabels }) {
                    settings.visuals.renderOrder.showPriorityLabels = it
                },
                toggle("Priority colors", { settings.visuals.renderOrder.colorByPriority }) {
                    settings.visuals.renderOrder.colorByPriority = it
                },
                toggle("Terrain indices", { settings.visuals.renderOrder.showTerrainIndices }) {
                    settings.visuals.renderOrder.showTerrainIndices = it
                },
                toggle("Terrain heatmap", { settings.visuals.renderOrder.showTerrainHeatmap }) {
                    settings.visuals.renderOrder.showTerrainHeatmap = it
                }
            )
            toggleRows(
                toggle("Sort volumes", { settings.visuals.renderOrder.showSortVolumes }) {
                    settings.visuals.renderOrder.showSortVolumes = it
                },
                toggle("Sort anchors", { settings.visuals.renderOrder.showSortAnchors }) {
                    settings.visuals.renderOrder.showSortAnchors = it
                },
                toggle(
                    "Projected positions",
                    { settings.visuals.renderOrder.showProjectedSortPositions }
                ) {
                    settings.visuals.renderOrder.showProjectedSortPositions = it
                }
            )
            boundStepper(
                "Geometry line width",
                { settings.visuals.renderOrder.sortGeometryLineWidth },
                0.25f,
                8f,
                0.25f
            ) { settings.visuals.renderOrder.sortGeometryLineWidth = it }
            boundDropdown(
                "Priority focus",
                RenderPriorityFocusMode.entries,
                { settings.visuals.renderOrder.priorityFocusMode },
                { it.name.toDisplayName() }
            ) { settings.visuals.renderOrder.priorityFocusMode = it }
            boundStepper(
                "Selected priority",
                { settings.visuals.renderOrder.selectedPriority.toFloat() },
                -100f,
                100f,
                1f
            ) { settings.visuals.renderOrder.selectedPriority = it.toInt() }
            boundStepper(
                "Priority color alpha",
                { settings.visuals.renderOrder.priorityColorAlpha },
                0f,
                1f,
                0.05f
            ) { settings.visuals.renderOrder.priorityColorAlpha = it }
            boundDropdown(
                "Heatmap color steps",
                TerrainHeatmapSteps.entries,
                { settings.visuals.renderOrder.terrainHeatmapSteps },
                { it.name.toDisplayName() }
            ) { settings.visuals.renderOrder.terrainHeatmapSteps = it }
            }
        }
        diagnostics.featureExpander(
            "Event Bus Monitor",
            { settings.operations.eventBus.enabled },
            { settings.operations.eventBus.enabled = it },
            enabledLabel = "Monitor visible"
        ) {
            toggleRows(
                toggle(
                    "Capture",
                    { settings.operations.eventBus.captureEnabled }
                ) {
                    settings.operations.eventBus.captureEnabled = it
                },
                toggle(
                    "Newest first",
                    { settings.operations.eventBus.newestFirst }
                ) {
                    settings.operations.eventBus.newestFirst = it
                }
            )
            boundStepper(
                "Visible records",
                { settings.operations.eventBus.maximumVisibleRecords.toFloat() },
                1f,
                25f,
                1f
            ) { settings.operations.eventBus.maximumVisibleRecords = it.toInt() }
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
                toggle("Sprite bounds", { settings.visuals.picking.showSpriteBounds }) { settings.visuals.picking.showSpriteBounds = it },
                toggle("Cursor marker", { settings.visuals.picking.showCursorHit }) { settings.visuals.picking.showCursorHit = it }
            )
            pickingRows = diagnosticTable()
            compactAction("Clear locked target") {
                settings.worldState.pickingSelection.clear()
            }
        }
        diagnostics.settingsExpander("Culling") {
            toggleRows(
                toggle("Render check area", { settings.visuals.culling.showVisibleArea }) {
                    settings.visuals.culling.showVisibleArea = it
                },
                toggle("Object culling bounds", { settings.visuals.culling.showObjectBounds }) {
                    settings.visuals.culling.showObjectBounds = it
                },
                toggle("Entity culling bounds", { settings.visuals.culling.showEntityBounds }) {
                    settings.visuals.culling.showEntityBounds = it
                }
            )
            cullingRows = diagnosticTable()
        }
        cameraSettings.apply {
            toggleRows(
                toggle("Visible area", { settings.visuals.camera.showVisibleArea }) { settings.visuals.camera.showVisibleArea = it },
                toggle("World bounds", { settings.visuals.camera.showWorldBounds }) { settings.visuals.camera.showWorldBounds = it },
                toggle("Clamp bounds", { settings.visuals.camera.showClampBounds }) { settings.visuals.camera.showClampBounds = it }
            )
            simpleToggle(
                "Disable restrictions",
                { settings.operations.disableCameraRestrictions }
            ) {
                settings.operations.disableCameraRestrictions = it
                settings.notify(
                    if (it) "Camera restrictions disabled" else "Camera restrictions enabled",
                    DebugNotificationSeverity.INFO
                )
            }
            cameraRows = diagnosticTable()
        }

        general.featureExpander(
            "Notifications",
            { settings.operations.notifications.enabled },
            { settings.operations.notifications.enabled = it },
            enabledLabel = "Notifications enabled"
        ) {
            boundDropdown(
                "Position",
                DebugNotificationPosition.entries,
                { settings.operations.notifications.position },
                { it.name.toDisplayName() }
            ) { settings.operations.notifications.position = it }
        }
    }

    private fun StrataColumn.buildGridSettings() {
        boundDropdown(
            "Layer",
            DebugGridRenderLayer.entries,
            { settings.visuals.grid.renderLayer },
            { it.name.toDisplayName() }
        ) { settings.visuals.grid.renderLayer = it }
        boundDropdown(
            "Extent",
            DebugGridExtent.entries,
            { settings.visuals.grid.extent },
            { it.name.toDisplayName() }
        ) { settings.visuals.grid.extent = it }
        toggleRows(
            toggle("Background", { settings.visuals.grid.showBackground }) {
                settings.visuals.grid.showBackground = it
            },
            toggle("Hover background", { settings.visuals.grid.showHoverBackground }) {
                settings.visuals.grid.showHoverBackground = it
            }
        )
        boundStepper("Line width", { settings.visuals.grid.lineWidth }, 0.25f, 8f, 0.25f) {
            settings.visuals.grid.lineWidth = it
        }
        boundColorAlpha("Grid color", { settings.visuals.grid.color }, 0.2f) { alpha ->
            settings.visuals.grid.color = settings.visuals.grid.color.apply { a = alpha }
        }
        boundColorAlpha("Hover color", { settings.visuals.grid.hoverColor }, 0.5f) { alpha ->
            settings.visuals.grid.hoverColor = settings.visuals.grid.hoverColor.apply { a = alpha }
        }
        boundColorAlpha("Background color", { settings.visuals.grid.backgroundColor }, 0.2f) { alpha ->
            settings.visuals.grid.backgroundColor = settings.visuals.grid.backgroundColor.apply { a = alpha }
        }

        boundColorAlpha("Hover background", { settings.visuals.grid.hoverBackgroundColor }, 0.5f) { alpha ->
            settings.visuals.grid.hoverBackgroundColor = settings.visuals.grid.hoverBackgroundColor.apply { a = alpha }
        }
    }

    private fun StrataColumn.buildObjectSettings() {
        toggleRows(
            toggle("Occupied tiles", { settings.visuals.objects.showOccupiedTiles }) {
                settings.visuals.objects.showOccupiedTiles = it
            },
            toggle("Origin tile", { settings.visuals.objects.showOriginTile }) {
                settings.visuals.objects.showOriginTile = it
            },
            toggle("Sprite bounds", { settings.visuals.objects.showSpriteBounds }) {
                settings.visuals.objects.showSpriteBounds = it
            },
            toggle("Tile fill", { settings.visuals.objects.showOccupiedTileFill }) {
                settings.visuals.objects.showOccupiedTileFill = it
            }
        )
        boundStepper("Line width", { settings.visuals.objects.lineWidth }, 0.25f, 8f, 0.25f) {
            settings.visuals.objects.lineWidth = it
        }
        boundColorAlpha("Fill color", { settings.visuals.objects.occupiedTileFillColor }, 0.18f) { alpha ->
            settings.visuals.objects.occupiedTileFillColor =
                settings.visuals.objects.occupiedTileFillColor.apply { a = alpha }
        }
    }

    private fun StrataColumn.buildEntitySettings() {
        toggleRows(
            toggle("Current tile", { settings.visuals.entities.showCurrentTile }) { settings.visuals.entities.showCurrentTile = it },
            toggle("Position", { settings.visuals.entities.showPosition }) { settings.visuals.entities.showPosition = it },
            toggle("Direction", { settings.visuals.entities.showDirection }) { settings.visuals.entities.showDirection = it },
            toggle("Sprite bounds", { settings.visuals.entities.showSpriteBounds }) { settings.visuals.entities.showSpriteBounds = it },
            toggle("Tile fill", { settings.visuals.entities.showCurrentTileFill }) {
                settings.visuals.entities.showCurrentTileFill = it
            }
        )
        boundStepper("Line width", { settings.visuals.entities.lineWidth }, 0.25f, 8f, 0.25f) {
            settings.visuals.entities.lineWidth = it
        }
        boundColorAlpha("Fill color", { settings.visuals.entities.currentTileFillColor }, 0.16f) { alpha ->
            settings.visuals.entities.currentTileFillColor =
                settings.visuals.entities.currentTileFillColor.apply { a = alpha }
        }
        settingsExpander("Movement") {
            toggleRows(
            toggle("Path", { settings.visuals.entities.showPath }) { settings.visuals.entities.showPath = it },
            toggle("Movement trail", { settings.visuals.entities.showMovementTrail }) {
                settings.visuals.entities.showMovementTrail = it
            },
            toggle("Movement vector", { settings.visuals.entities.showMovementVector }) {
                settings.visuals.entities.showMovementVector = it
            },
            toggle("Next waypoint", { settings.visuals.entities.showNextWaypoint }) {
                settings.visuals.entities.showNextWaypoint = it
            },
            toggle("Speed labels", { settings.visuals.entities.showMovementSpeed }) {
                settings.visuals.entities.showMovementSpeed = it
            },
            toggle("Tile-position offset", { settings.visuals.entities.showPositionTileOffset }) {
                settings.visuals.entities.showPositionTileOffset = it
            }
        )
        boundStepper(
            "Maximum positions",
            { settings.visuals.entities.trailMaxPositions.toFloat() },
            2f,
            1000f,
            10f
        ) { settings.visuals.entities.trailMaxPositions = it.toInt() }
        boundStepper(
            "History seconds",
            { settings.visuals.entities.trailHistoryDurationSeconds },
            0.25f,
            60f,
            0.25f
        ) { settings.visuals.entities.trailHistoryDurationSeconds = it }
        boundStepper(
            "Sample distance",
            { settings.visuals.entities.trailMinimumDistance },
            0f,
            1f,
            0.01f
        ) { settings.visuals.entities.trailMinimumDistance = it }
        boundStepper(
            "Trail opacity",
            { settings.visuals.entities.trailOpacity },
            0f,
            1f,
            0.05f
        ) { settings.visuals.entities.trailOpacity = it }
        boundStepper(
            "Vector seconds",
            { settings.visuals.entities.movementVectorScaleSeconds },
            0.1f,
            5f,
            0.1f
        ) { settings.visuals.entities.movementVectorScaleSeconds = it }
        compactAction("Clear movement trails") {
            settings.visuals.entities.clearMovementTrails()
        }
        }
    }

    private fun StrataColumn.buildWorldInfoSettings() {
        toggleRows(
            toggle("Tile coordinates", { settings.visuals.worldInfo.showTileCoordinates }) {
                settings.visuals.worldInfo.showTileCoordinates = it
            },
            toggle("Terrain IDs", { settings.visuals.worldInfo.showTerrainIds }) {
                settings.visuals.worldInfo.showTerrainIds = it
            },
            toggle("Overlay info", { settings.visuals.worldInfo.showOverlayInfo }) {
                settings.visuals.worldInfo.showOverlayInfo = it
            },
            toggle("Occupancy", { settings.visuals.worldInfo.showOccupancy }) {
                settings.visuals.worldInfo.showOccupancy = it
            },
            toggle("Missing visuals", { settings.visuals.worldInfo.showMissingTerrainVisuals }) {
                settings.visuals.worldInfo.showMissingTerrainVisuals = it
            },
            toggle("World origin", { settings.visuals.worldInfo.showOrigin }) {
                settings.visuals.worldInfo.showOrigin = it
            }
        )
        boundStepper(
            "Maximum label zoom",
            { settings.visuals.worldInfo.maximumLabelZoom },
            0.25f,
            8f,
            0.25f
        ) { settings.visuals.worldInfo.maximumLabelZoom = it }
        boundStepper(
            "Maximum labels",
            { settings.visuals.worldInfo.maximumVisibleLabels.toFloat() },
            16f,
            1024f,
            16f
        ) { settings.visuals.worldInfo.maximumVisibleLabels = it.toInt() }
        boundColorAlpha("Occupancy color", { settings.visuals.worldInfo.occupancyColor }, 0.5f) { alpha ->
            settings.visuals.worldInfo.occupancyColor = settings.visuals.worldInfo.occupancyColor.apply { a = alpha }
        }
        boundColorAlpha("Missing visual color", { settings.visuals.worldInfo.missingVisualColor }, 0.5f) { alpha ->
            settings.visuals.worldInfo.missingVisualColor =
                settings.visuals.worldInfo.missingVisualColor.apply { a = alpha }
        }
    }

    private fun StrataColumn.buildWorldVisibilitySettings() {
        toggleRows(
            toggle("Ground terrain", { settings.visuals.worldVisibility.groundTerrainVisible }) {
                settings.visuals.worldVisibility.groundTerrainVisible = it
            },
            toggle("Terrain overlays", { settings.visuals.worldVisibility.terrainOverlaysVisible }) {
                settings.visuals.worldVisibility.terrainOverlaysVisible = it
            },
            toggle("Placed objects", { settings.visuals.worldVisibility.placedObjectsVisible }) {
                settings.visuals.worldVisibility.placedObjectsVisible = it
            },
            toggle("Entities", { settings.visuals.worldVisibility.entitiesVisible }) {
                settings.visuals.worldVisibility.entitiesVisible = it
            }
        )
        val overlayIds = world.overlayLayerIds
        if (overlayIds.isNotEmpty()) {
            settingsExpander("Overlay layers") {
                overlayIds.forEach { layerId ->
                    simpleToggle(
                        layerId,
                        { settings.visuals.worldVisibility.isOverlayLayerVisible(layerId) }
                    ) { visible ->
                        settings.visuals.worldVisibility.setOverlayLayerVisible(layerId, visible)
                    }
                }
            }
        }
        compactAction("Show all categories") {
            settings.visuals.worldVisibility.showAll()
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
