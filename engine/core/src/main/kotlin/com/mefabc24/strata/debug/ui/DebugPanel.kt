package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.mefabc24.strata.debug.*
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.debug.inspector.formatEntityPosition
import com.mefabc24.strata.debug.inspector.formatFootprint
import com.mefabc24.strata.debug.inspector.formatTilePosition
import com.mefabc24.strata.debug.inspector.formatTilePositions
import com.mefabc24.strata.debug.tools.*
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.pathfinding.PathMovementMode
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.`object`.ObjectEntry
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.render.entity.EntityEntry
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.render.debug.cullingDebugCounts
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.terrain.TerrainEntry
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.ui.*
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import java.util.Locale

internal class DebugPanel(
    private val ui: StrataUi,
    private val settings: DebugSettings,
    private val tools: DebugToolController,
    private val painter: DebugTerrainPainter,
    private val spawner: DebugEntitySpawner,
    private val buildEntries: List<ObjectEntry>,
    private val placement: PlacementController?,
    private val inspector: DebugInspector,
    private val pathfinding: DebugPathfindingTool,
    private val simulation: SimulationController,
    private val eventMonitor: DebugEventMonitor,
    private val world: World,
    private val view: IsoWorldView,
    private val terrainFor: (Tile) -> TerrainId,
    private val objects: ObjectRegistry,
    private val entities: EntityRegistry
) {
    private val statsOverlay = DebugStatsOverlay(
        ui, { view.renderStats }, settings.performance, { settings.performance.overlayEnabled },
        { settings.worldStats.enabled }, world, placement, eventMonitor, settings.eventBus
    )
    private val simulationOverlay = DebugSimulationOverlay(ui, simulation)
    private val synchronizers = DebugControlBindings()
    private val previewState = DebugContentPreviewState()
    private val modes = availableDebugToolModes(
        buildAvailable = tools.buildAvailable && buildEntries.isNotEmpty(),
        paintAvailable = painter.entries.isNotEmpty(),
        spawnAvailable = spawner.entries.isNotEmpty()
    )
    private val toolRailState = DebugToolRailState(modes.filterNot {
        it == DebugToolMode.NONE
    }.toSet())
    private val buildSelection = buildEntries.takeIf { it.isNotEmpty() }?.let { entries ->
        ui.selectionGroup(entries, entries.first()) { tools.selectBuildEntry(it) }
    }
    private val terrainSelection = painter.entries.takeIf { it.isNotEmpty() }?.let { entries ->
        ui.selectionGroup(entries, entries.first()) { painter.selectedEntry = it }
    }
    private val spawnSelection = spawner.entries.takeIf { it.isNotEmpty() }?.let { entries ->
        ui.selectionGroup(entries, entries.first()) { spawner.selectedEntry = it }
    }
    private val paintTargets = buildList {
        add(DebugPaintTarget.GROUND)
        if (painter.overlayLayerIds.isNotEmpty()) add(DebugPaintTarget.OVERLAY)
    }
    private val paintTargetSelection = ui.selectionGroup(paintTargets, DebugPaintTarget.GROUND) {
        painter.target = it
    }
    private val overlaySelection = painter.overlayLayerIds.takeIf { it.isNotEmpty() }?.let { layers ->
        ui.selectionGroup(layers, layers.first()) { painter.selectedOverlayLayerId = it }
    }

    private lateinit var windowLayout: Table
    private lateinit var toolRailActor: StrataPanel
    private lateinit var toolFlyoutActor: StrataPanel
    private lateinit var toolFlyoutScroll: StrataScrollPane
    private lateinit var toolFlyoutHeader: Label
    private lateinit var debugPanelActor: StrataPanel
    private lateinit var debugScroll: StrataScrollPane
    private lateinit var debugTab: StrataColumn
    private val toolButtons = linkedMapOf<DebugToolMode, DebugToolRailButton>()
    private lateinit var buildControls: StrataColumn
    private lateinit var deleteControls: StrataColumn
    private lateinit var paintControls: StrataColumn
    private lateinit var overlayPaintControls: StrataColumn
    private lateinit var spawnControls: StrataColumn
    private lateinit var inspectControls: StrataColumn
    private lateinit var pathControls: StrataColumn
    private lateinit var inspectorRows: DebugDiagnosticTable
    private lateinit var pickingRows: DebugDiagnosticTable
    private lateinit var cameraRows: DebugDiagnosticTable
    private lateinit var cullingRows: DebugDiagnosticTable
    private lateinit var contextRows: DebugDiagnosticTable
    private lateinit var previewName: Label
    private lateinit var previewImage: Image
    private lateinit var previewPopover: StrataPopover
    private var previewAnchor: Actor? = null
    private var lastMode = DebugToolMode.NONE
    private var appliedFlyoutMode: DebugToolMode? = null
    private var appliedWindowVisibility: Pair<Boolean, Boolean>? = null
    private var buildingDebugSettings = false

    init {
        buildSelection?.selected?.let(tools::selectBuildEntry)
        terrainSelection?.selected?.let { painter.selectedEntry = it }
        spawnSelection?.selected?.let { spawner.selectedEntry = it }
        buildUi()
        if (settings.toolsWindow.enabled) buildPreview()
        synchronizers += {
            toolButtons.forEach { (mode, button) ->
                button.selected = tools.mode == mode
            }
        }
        terrainSelection?.let { group ->
            synchronizers += { painter.selectedEntry?.let(group::select) }
        }
        spawnSelection?.let { group ->
            synchronizers += { spawner.selectedEntry?.let(group::select) }
        }
        synchronizers += { paintTargetSelection.select(painter.target) }
        overlaySelection?.let { group ->
            synchronizers += { painter.selectedOverlayLayerId?.let(group::select) }
        }
        setToolsWindowVisible(settings.toolsWindow.visibleOnStartup)
        setDebugWindowVisible(settings.debugWindow.visibleOnStartup)
        syncStatsOverlayPosition()
        syncControls()
        syncVisibility()
    }

    fun setToolsWindowVisible(visible: Boolean) {
        if (!settings.toolsWindow.enabled) return
        settings.toolsWindow.visible = visible
        if (!visible) {
            hidePreview()
            closeToolSettings()
        }
        syncWindowLayout()
    }

    fun setDebugWindowVisible(visible: Boolean) {
        if (!settings.debugWindow.enabled) return
        settings.debugWindow.visible = visible
        syncWindowLayout()
    }

    fun update(delta: Float) {
        statsOverlay.update(delta)
        simulationOverlay.setVisible(settings.simulation.enabled)
        simulationOverlay.update(delta)
        syncWindowLayout()
        syncControls()
        syncVisibility()
        positionToolFlyout()
        syncStatsOverlayPosition()
        syncDiagnostics()
    }

    fun resized() {
        syncWindowLayout(force = true)
        positionToolFlyout()
        syncStatsOverlayPosition()
        if (!settings.toolsWindow.enabled) return
        val anchor = previewAnchor ?: return
        if (previewState.current != null) previewPopover.showRightOf(toolFlyoutActor, anchor)
    }

    private fun buildUi() {
        ui.root.pad(0f)

        if (settings.toolsWindow.enabled) {
            buildToolRail()
            buildToolFlyout()
        }

        if (settings.debugWindow.enabled) {
            debugPanelActor = ui.panel(
                styleName = null,
                spacing = 0f,
                padding = StrataInsets.NONE
            ) {
                defaults().fillAvailableX()
                debugScroll = scrollColumn(spacing = 0f) {
                    defaults().fillAvailableX()
                    debugTab = column(spacing = 0f) {
                        defaults().fillAvailableX()
                        buildingDebugSettings = true
                        buildDebug()
                        buildingDebugSettings = false
                    }
                }.cell {
                    fillAvailableX()
                    minHeight(0f)
                    prefHeight(Value.prefHeight)
                    maxHeight(Value.percentHeight(0.90f, ui.root))
                }
            }
            debugPanelActor.remove()
        }

        // Remove empty cells left by the temporarily attached panels.
        ui.root.clearChildren()

        windowLayout = Table().apply {
            top()
            touchable = Touchable.childrenOnly
        }
        ui.actor(windowLayout).cell { growX(); fillX(); top() }
    }

    private fun buildToolRail() {
        val style = ui.skin.get(
            "debug-tool-rail-button",
            DebugToolRailButtonStyle::class.java
        )
        toolRailActor = ui.panel(
            styleName = null,
            spacing = 0f,
            padding = StrataInsets.NONE
        ) {
            scrollColumn(spacing = 0f, padding = StrataInsets.NONE) {
                defaults().fillAvailableX()
                modes.forEachIndexed { index, mode ->
                    val button = DebugToolRailButton(
                        mode = mode,
                        label = debugToolRailLabel(mode),
                        icon = ui.skin.getDrawable(debugToolIconName(mode)),
                        skin = ui.skin,
                        style = style,
                        onSelected = ::selectTool,
                        onSettingsRequested = ::toggleToolSettings
                    )
                    toolButtons[mode] = actor(button).cell {
                        width(DebugWindowLayout.TOOL_RAIL_WIDTH)
                        height(DebugWindowLayout.TOOL_BUTTON_HEIGHT)
                    }
                    if (index != modes.lastIndex) separator()
                }
            }.cell { grow(); minHeight(0f) }
        }
        toolRailActor.remove()
    }

    private fun buildToolFlyout() {
        toolFlyoutActor = ui.panel(
            styleName = "debug-panel",
            spacing = 0f,
            padding = StrataInsets.NONE
        ) {
            defaults().fillAvailableX()
            toolFlyoutHeader = label("", "title").cell {
                height(34f)
                padLeft(10f)
                padRight(10f)
                left()
            }
            separator()
            toolFlyoutScroll = scrollColumn(
                spacing = 0f,
                padding = StrataInsets.NONE
            ) {
                defaults().fillAvailableX()
                buildingDebugSettings = true
                buildToolSettings()
                buildingDebugSettings = false
                column(spacing = 0f) {
                    defaults().fillAvailableX()
                    separator()
                    label("STATUS").cell {
                        height(28f)
                        padLeft(8f)
                        left()
                    }
                    contextRows = diagnosticTable()
                }
            }.cell {
                grow()
                minHeight(0f)
            }
        }
        toolFlyoutActor.remove()
        toolFlyoutActor.isVisible = false
        ui.stage.addActor(toolFlyoutActor)
    }

    private fun buildPreview() {
        previewPopover = ui.popover(width = 128f, height = 154f) {
            defaults().fillAvailableX()
            previewName = label("").apply {
                setAlignment(Align.center); setWrap(true)
            }.cell { height(30f) }
            previewImage = actor(Image().apply { setScaling(Scaling.fit) })
                .cell { grow(); fill(); minHeight(96f) }
        }
    }

    private fun StrataColumn.buildToolSettings() {
        stack {
            buildControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander("Selection") {
                    responsiveGrid(130f, maximumColumns = 2) {
                        buildEntries.forEach { entry ->
                            selectableButton(
                                entry.displayName(),
                                entry,
                                checkNotNull(buildSelection)
                            ).previewOnHover(
                                entry,
                                DebugContentKind.OBJECT,
                                entry.displayName(),
                                entry.selectionVisual.texture
                            )
                        }
                    }.cell { fillAvailableX() }
                }
            }
            deleteControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSection("Delete brush")
                brushSizeStepper("Brush size", { settings.delete.brushSize }) {
                    settings.delete.brushSize = it
                }
                simpleToggle("Drag deletion", { settings.delete.dragEnabled }) {
                    settings.delete.dragEnabled = it
                }
                simpleToggle("Show affected area", { settings.delete.showBrushPreview }) {
                    settings.delete.showBrushPreview = it
                }
            }
            paintControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander("Brush") {
                    brushSizeStepper("Brush size", { settings.paint.brushSize }) {
                        settings.paint.brushSize = it
                    }
                    simpleToggle("Show affected area", { settings.paint.showBrushPreview }) {
                        settings.paint.showBrushPreview = it
                    }
                }
                toolSettingsExpander("Terrain") {
                    responsiveGrid(130f, maximumColumns = 2) {
                        painter.entries.forEach { entry ->
                            val name = entry.type.toString().toDisplayName()
                            selectableButton(name, entry, checkNotNull(terrainSelection))
                                .previewOnHover(
                                    entry,
                                    DebugContentKind.TERRAIN,
                                    name,
                                    entry.selectionTexture
                                )
                        }
                    }.cell { fillAvailableX() }
                }
                toolSettingsExpander("Target") {
                    responsiveGrid(130f, maximumColumns = 2) {
                        paintTargets.forEach { target ->
                            selectableButton(
                                target.name.toDisplayName(),
                                target,
                                paintTargetSelection
                            )
                        }
                    }.cell { fillAvailableX() }
                    overlayPaintControls = column(spacing = 0f) {
                        defaults().fillAvailableX()
                        row(
                            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 3f)
                        ) {
                            label("Overlay layer").cell { growX(); left() }
                        }.cell { fillAvailableX(); height(30f) }
                        responsiveGrid(130f, maximumColumns = 2) {
                            painter.overlayLayerIds.forEach { id ->
                                selectableButton(
                                    id.toDisplayName(),
                                    id,
                                    checkNotNull(overlaySelection)
                                )
                            }
                        }.cell { fillAvailableX() }
                    }
                }
            }
            spawnControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander("Selection") {
                    responsiveGrid(130f, maximumColumns = 2) {
                        spawner.entries.forEach { entry ->
                            val name = entry.type.displayName()
                            selectableButton(name, entry, checkNotNull(spawnSelection))
                                .previewOnHover(
                                    entry,
                                    DebugContentKind.ENTITY,
                                    name,
                                    entry.selectionVisual.texture
                                )
                        }
                    }.cell { fillAvailableX() }
                }
            }
            inspectControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander("Selection") {
                    inspectorRows = diagnosticTable()
                    compactAction("Clear selection") { inspector.clear() }
                }
                toolSettingsExpander("Entity control") {
                simpleToggle(
                    "Frozen",
                    {
                        (inspector.selection as? DebugInspection.EntityTarget)
                            ?.entity
                            ?.let(settings::isEntityFrozen)
                            ?: false
                    }
                ) { frozen ->
                    (inspector.selection as? DebugInspection.EntityTarget)
                        ?.entity
                        ?.let {
                            settings.setEntityFrozen(it, frozen)
                            settings.notify(
                                if (frozen) "Entity frozen" else "Entity unfrozen",
                                DebugNotificationSeverity.SUCCESS
                            )
                        }
                }
                simpleToggle(
                    "Freeze animation",
                    { settings.inspect.freezeEntityAnimation }
                ) { settings.inspect.freezeEntityAnimation = it }
                compactAction("Unfreeze all entities") {
                    val count = settings.unfreezeAllEntities()
                    settings.notify(
                        if (count == 0) "No frozen entities" else "Unfroze $count entities",
                        if (count == 0) DebugNotificationSeverity.INFO else DebugNotificationSeverity.SUCCESS
                    )
                }
                }
                toolSettingsExpander("Visualization") {
                settingGroupLabel("Tile")
                toggleRows(
                    toggle("Selected tile", { settings.inspect.showTile }) {
                        settings.inspect.showTile = it
                    }
                )
                settingGroupLabel("Object")
                toggleRows(
                    toggle("Footprint", { settings.inspect.showObjectFootprint }) {
                        settings.inspect.showObjectFootprint = it
                    },
                    toggle("Origin", { settings.inspect.showObjectOrigin }) {
                        settings.inspect.showObjectOrigin = it
                    },
                    toggle("Sprite bounds", { settings.inspect.showObjectSpriteBounds }) {
                        settings.inspect.showObjectSpriteBounds = it
                    }
                )
                settingGroupLabel("Entity")
                toggleRows(
                    toggle("Current tile", { settings.inspect.showEntityTile }) {
                        settings.inspect.showEntityTile = it
                    },
                    toggle("Position", { settings.inspect.showEntityPosition }) {
                        settings.inspect.showEntityPosition = it
                    },
                    toggle("Path", { settings.inspect.showEntityPath }) {
                        settings.inspect.showEntityPath = it
                    },
                    toggle("Direction", { settings.inspect.showEntityDirection }) {
                        settings.inspect.showEntityDirection = it
                    },
                    toggle("Sprite bounds", { settings.inspect.showEntitySpriteBounds }) {
                        settings.inspect.showEntitySpriteBounds = it
                    }
                )
                }
            }
            pathControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                val movementModes = ui.selectionGroup(
                    PathMovementMode.entries,
                    settings.pathfinding.movementMode
                ) {
                    settings.pathfinding.movementMode = it
                }
                synchronizers += {
                    movementModes.select(settings.pathfinding.movementMode)
                }
                toolSettingsExpander("Search") {
                    responsiveGrid(105f, 36f, maximumColumns = 2) {
                        selectableButton("4-way", PathMovementMode.FOUR_WAY, movementModes)
                        selectableButton("8-way", PathMovementMode.EIGHT_WAY, movementModes)
                    }.cell { fillAvailableX() }
                }
                toolSettingsExpander("Visualization") {
                    simpleToggle(
                        "World visualization",
                        { settings.pathfinding.enabled }
                    ) { settings.pathfinding.enabled = it }
                    toggleRows(
                        toggle("Explored nodes", { settings.pathfinding.showExploredNodes }) {
                            settings.pathfinding.showExploredNodes = it
                        },
                        toggle("Final path", { settings.pathfinding.showFinalPath }) {
                            settings.pathfinding.showFinalPath = it
                        },
                        toggle("Open set", { settings.pathfinding.showOpenSet }) {
                            settings.pathfinding.showOpenSet = it
                        },
                        toggle("Closed set", { settings.pathfinding.showClosedSet }) {
                            settings.pathfinding.showClosedSet = it
                        },
                        toggle("G cost", { settings.pathfinding.showGCost }) {
                            settings.pathfinding.showGCost = it
                        },
                        toggle("H cost", { settings.pathfinding.showHCost }) {
                            settings.pathfinding.showHCost = it
                        },
                        toggle("F cost", { settings.pathfinding.showFCost }) {
                            settings.pathfinding.showFCost = it
                        },
                        toggle("Parent direction", { settings.pathfinding.showParentDirections }) {
                            settings.pathfinding.showParentDirections = it
                        },
                        toggle("Exploration order", { settings.pathfinding.showExplorationOrder }) {
                            settings.pathfinding.showExplorationOrder = it
                        },
                        toggle("Rejected transitions", { settings.pathfinding.showRejectedTransitions }) {
                            settings.pathfinding.showRejectedTransitions = it
                        }
                    )
                    boundStepper(
                        "Maximum label zoom",
                        { settings.pathfinding.maximumLabelZoom },
                        0.25f,
                        8f,
                        0.25f
                    ) { settings.pathfinding.maximumLabelZoom = it }
                    boundStepper(
                        "Maximum labels",
                        { settings.pathfinding.maximumVisibleLabels.toFloat() },
                        16f,
                        1024f,
                        16f
                    ) { settings.pathfinding.maximumVisibleLabels = it.toInt() }
                    boundStepper(
                        "Recorded rejections",
                        { settings.pathfinding.maximumRejectedTransitions.toFloat() },
                        0f,
                        8192f,
                        128f
                    ) { settings.pathfinding.maximumRejectedTransitions = it.toInt() }
                }
                toolSettingsExpander("Diagnostic search") {
                    compactActions(
                        "Start" to { pathfinding.startDiagnosticSearch() },
                        "Step" to { pathfinding.stepDiagnosticSearch() }
                    )
                    compactActions(
                        "Continue" to { pathfinding.continueDiagnosticSearch() },
                        "Pause" to { pathfinding.pauseDiagnosticSearch() },
                        "Reset" to { pathfinding.resetDiagnosticSearch() }
                    )
                    boundStepper(
                        "Iterations / update",
                        { settings.pathfinding.automaticIterationsPerUpdate.toFloat() },
                        1f,
                        64f,
                        1f
                    ) { settings.pathfinding.automaticIterationsPerUpdate = it.toInt() }
                }
                toolSettingsExpander("Traversal") {
                    simpleToggle(
                        "Consume reached nodes",
                        { settings.pathfinding.consumeReachedWaypoints }
                    ) { settings.pathfinding.consumeReachedWaypoints = it }
                    boundStepper(
                        "Entity speed multiplier",
                        { settings.pathfinding.entitySpeedMultiplier },
                        0.25f,
                        10f,
                        0.25f
                    ) { settings.pathfinding.entitySpeedMultiplier = it }
                    compactAction("Clear path") {
                        if (pathfinding.clear()) {
                            settings.notify("Path cleared", DebugNotificationSeverity.INFO)
                        }
                    }
                }
            }
        }.cell { fillAvailableX() }
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
    ) {
        settingToggleRow(text, read, write)
    }

    private fun StrataColumn.toolSection(text: String) {
        row(
            padding = StrataInsets.symmetric(horizontal = 8f, vertical = 3f)
        ) {
            label(text).cell { growX(); left() }
        }.apply {
            background = ui.skin.get(
                "debug-expander",
                StrataExpanderStyle::class.java
            ).headerBackground
        }.cell { fillAvailableX(); height(30f) }
        separator()
    }

    private fun StrataColumn.settingGroupLabel(text: String) {
        row(
            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 2f)
        ) {
            label(text, "title").cell { growX(); left() }
        }.applyDebugSettingBackground().cell { fillAvailableX(); height(28f) }
        separator()
    }

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
        onExpandedChanged: (Boolean) -> Unit = {},
        configure: StrataColumn.() -> Unit
    ): StrataExpander {
        val expander = expander(
            title = title,
            expanded = expanded,
            spacing = 0f,
            headerHeight = 34f,
            expandedStyle = debugExpanderStyle(),
            onExpandedChanged = onExpandedChanged
        ) {
            defaults().fillAvailableX()
            configure()
        }
        getCell(expander).fillAvailableX()
        return expander
    }

    private fun StrataColumn.toolSettingsExpander(
        title: String,
        expanded: Boolean = true,
        configure: StrataColumn.() -> Unit
    ): StrataExpander = settingsExpander(
        title = title,
        expanded = expanded,
        onExpandedChanged = { invalidateToolFlyoutLayout() },
        configure = configure
    )

    private fun invalidateToolFlyoutLayout() {
        if (!::toolFlyoutScroll.isInitialized) return
        toolFlyoutScroll.content.invalidateHierarchy()
        toolFlyoutScroll.invalidateHierarchy()
        toolFlyoutActor.invalidateHierarchy()
        positionToolFlyout()
    }

    private fun debugExpanderStyle(): StrataExpanderStyle = ui.skin.get(
        "debug-expander",
        StrataExpanderStyle::class.java
    )

    private fun StrataColumn.toggleRows(vararg controls: ToggleBinding) {
        controls.forEach { binding ->
            settingToggleRow(binding.text, binding.read, binding.write)
        }
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

    private fun StrataColumn.compactAction(
        text: String,
        onClick: () -> Unit
    ) {
        compactActions(text to onClick)
    }

    private fun StrataColumn.compactActions(
        vararg actions: Pair<String, () -> Unit>
    ) {
        row(
            spacing = 4f,
            padding = StrataInsets.NONE
        ) {
            actions.forEach { (label, action) ->
                button(label, "debug-action-card", action).cell {
                    growX()
                    height(32f)
                }
            }
        }.cell {
            fillAvailableX()
            height(32f)
        }
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
        val button = toggleButton(
            if (read()) "ON" else "OFF",
            read(),
            onChanged = write
        )
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
        }.cell {
            fillAvailableX()
            height(32f)
        }
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
                "",
                read()?.a ?: defaultAlpha,
                0f,
                1f,
                0.05f,
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

    private fun StrataColumn.brushSizeStepper(
        text: String,
        read: () -> Int,
        write: (Int) -> Unit
    ): StrataNumericStepper {
        val stepper = numericStepper(
            text,
            read().toFloat(),
            1f,
            9f,
            2f,
            decimals = 0,
            onChanged = { write(it.toInt()) }
        ).applyDebugSettingBackground().cell {
            fillAvailableX()
            height(32f)
            padLeft(6f)
            padRight(6f)
        }
        separator()
        synchronizers += { stepper.sync(read().toFloat()) }
        return stepper
    }

    private fun StrataLayout.wrappingLabel(text: String): Label = label(text).apply {
        setWrap(true)
    }.cell { fillAvailableX() }

    private fun StrataLayout.diagnosticTable(): DebugDiagnosticTable {
        val table = actor(DebugDiagnosticTable(ui.skin))
        table.applyDebugSettingBackground()
        val cell = requireNotNull(getCell(table)).apply { fillAvailableX() }
        table.bindLayout(cell)
        return table
    }

    private fun <T : Table> T.applyDebugSettingBackground(): T = apply {
        if (buildingDebugSettings) {
            background = ui.skin.get(
                "debug-setting-row",
                StrataPanelStyle::class.java
            ).background
        }
    }

    private fun <A : Actor> A.previewOnHover(
        key: Any,
        kind: DebugContentKind,
        name: String,
        texture: TextureRegion
    ): A = onHover(
        entered = { actor ->
            showPreview(DebugContentPreview(key, kind, name, texture), actor)
        },
        exited = { hidePreview(key) }
    )

    private fun showPreview(preview: DebugContentPreview, anchor: Actor) {
        if (!settings.toolsWindow.visible) return
        previewState.show(preview)
        previewAnchor = anchor
        previewName.setText(preview.name)
        previewImage.drawable = TextureRegionDrawable(preview.texture)
        previewPopover.showRightOf(toolFlyoutActor, anchor)
    }

    private fun hidePreview(key: Any? = null) {
        previewState.hide(key)
        if (previewState.current == null && ::previewPopover.isInitialized) {
            previewAnchor = null
            previewPopover.hide()
        }
    }

    private fun syncControls() {
        synchronizers.sync()
    }

    private fun selectTool(mode: DebugToolMode) {
        hidePreview()
        tools.select(mode)
        syncControls()
        syncVisibility()
    }

    private fun toggleToolSettings(mode: DebugToolMode) {
        hidePreview()
        toolRailState.toggleSettings(mode)
        syncVisibility()
        positionToolFlyout()
    }

    private fun closeToolSettings() {
        toolRailState.closeSettings()
        appliedFlyoutMode = null
        if (::toolFlyoutActor.isInitialized) toolFlyoutActor.isVisible = false
    }

    private fun positionToolFlyout() {
        val mode = toolRailState.settingsMode ?: return
        if (!settings.toolsWindow.visible || !::toolFlyoutActor.isInitialized) return
        val anchor = toolButtons[mode] ?: return
        val viewportWidth = ui.stage.viewport.worldWidth
            .takeIf { it > 0f }
            ?: Gdx.graphics.width.toFloat()
        val viewportHeight = ui.stage.viewport.worldHeight
            .takeIf { it > 0f }
            ?: Gdx.graphics.height.toFloat()
        val debugVisible = settings.debugWindow.enabled && settings.debugWindow.visible &&
            ::debugPanelActor.isInitialized
        val rightInset = if (debugVisible) {
            DebugWindowLayout.debugWidth(viewportWidth)
        } else {
            0f
        }
        toolFlyoutActor.width = DebugWindowLayout.TOOL_FLYOUT_WIDTH
        toolFlyoutActor.invalidateHierarchy()
        toolFlyoutActor.validate()
        val railRight = toolRailActor.localToStageCoordinates(
            com.badlogic.gdx.math.Vector2(toolRailActor.width, 0f)
        ).x
        val bounds = debugToolFlyoutBounds(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            railRight = railRight,
            anchorTop = anchor.topInStage().y,
            preferredWidth = DebugWindowLayout.TOOL_FLYOUT_WIDTH,
            preferredHeight = toolFlyoutActor.prefHeight,
            rightInset = rightInset
        )
        toolFlyoutActor.setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
        toolFlyoutActor.validate()
        toolFlyoutActor.toFront()
    }

    private fun syncWindowLayout(force: Boolean = false) {
        if (!::windowLayout.isInitialized) return
        val toolsVisible = settings.toolsWindow.enabled && settings.toolsWindow.visible &&
            ::toolRailActor.isInitialized
        val debugVisible = settings.debugWindow.enabled && settings.debugWindow.visible &&
            ::debugPanelActor.isInitialized
        val desired = toolsVisible to debugVisible
        if (!force && desired == appliedWindowVisibility) return
        val availableWidth = ui.stage.viewport.worldWidth
            .takeIf { it > 0f }
            ?: Gdx.graphics.width.toFloat()
        val debugWidth = DebugWindowLayout.debugWidth(availableWidth)

        windowLayout.clearChildren()
        if (toolsVisible) {
            windowLayout.add(toolRailActor)
                .width(DebugWindowLayout.TOOL_RAIL_WIDTH)
                .minHeight(0f)
                .prefHeight(Value.prefHeight)
                .maxHeight(Value.percentHeight(1f, ui.root))
                .top()
                .left()
        }
        windowLayout.add().growX()
        if (debugVisible) {
            windowLayout.add(debugPanelActor)
                .width(debugWidth)
                .minHeight(0f)
                .prefHeight(Value.prefHeight)
                .maxHeight(Value.percentHeight(1f, ui.root))
                .top()
                .right()
        }
        appliedWindowVisibility = desired
        windowLayout.isVisible = toolsVisible || debugVisible
        windowLayout.invalidateHierarchy()
        ui.root.invalidateHierarchy()
    }

    private fun syncStatsOverlayPosition() {
        val toolsVisible = settings.toolsWindow.enabled && settings.toolsWindow.visible &&
            ::toolRailActor.isInitialized
        statsOverlay.setTopPadding(DebugWindowLayout.OVERLAY_MARGIN)
        statsOverlay.setLeftPadding(
            DebugWindowLayout.overlayLeftPadding(
                toolsVisible = toolsVisible,
                flyoutVisible = toolsVisible && ::toolFlyoutActor.isInitialized &&
                    toolFlyoutActor.isVisible,
                flyoutWidth = if (::toolFlyoutActor.isInitialized) {
                    toolFlyoutActor.width
                } else {
                    DebugWindowLayout.TOOL_FLYOUT_WIDTH
                }
            )
        )
    }

    private fun syncVisibility() {
        if (!settings.toolsWindow.enabled || !::toolFlyoutActor.isInitialized) return
        val activeMode = tools.mode
        val displayedMode = toolRailState.settingsMode

        val previousMode = appliedFlyoutMode
        val previousOverlayVisibility = overlayPaintControls.isVisible

        if (activeMode != lastMode) {
            hidePreview()
            lastMode = activeMode
        }

        buildControls.isVisible = displayedMode == DebugToolMode.BUILD
        deleteControls.isVisible = displayedMode == DebugToolMode.DELETE
        paintControls.isVisible = displayedMode == DebugToolMode.PAINT
        spawnControls.isVisible = displayedMode == DebugToolMode.SPAWN
        inspectControls.isVisible = displayedMode == DebugToolMode.INSPECT
        pathControls.isVisible = displayedMode == DebugToolMode.PATHFINDING

        overlayPaintControls.isVisible =
            displayedMode == DebugToolMode.PAINT &&
                painter.target == DebugPaintTarget.OVERLAY

        toolFlyoutActor.isVisible = displayedMode != null && settings.toolsWindow.visible
        if (displayedMode != null) {
            toolFlyoutHeader.setText("${displayedMode.displayName} Tool")
        }

        if (
            displayedMode != previousMode ||
            overlayPaintControls.isVisible != previousOverlayVisibility
        ) {
            if (displayedMode != previousMode) toolFlyoutScroll.scrollY = 0f
            appliedFlyoutMode = displayedMode
            toolFlyoutScroll.content.invalidateHierarchy()
            toolFlyoutScroll.invalidateHierarchy()
            toolFlyoutActor.invalidateHierarchy()
            positionToolFlyout()
        }
    }

    private fun syncDiagnostics() {
        if (settings.toolsWindow.enabled) {
            inspectorRows.show(formatInspection())
            syncContextFooter()
        }
        if (settings.debugWindow.enabled) {
            pickingRows.show(if (settings.picking.hasActiveVisuals) formatPicking() else emptyList())
            cameraRows.show(if (settings.camera.hasActiveVisuals) formatCamera() else emptyList())
            cullingRows.show(if (settings.culling.hasActiveVisuals) formatCulling() else emptyList())
        }
    }

    private fun formatPicking(): List<DebugDiagnosticRow> {
        val x = Gdx.input.x.toFloat()
        val y = Gdx.input.y.toFloat()
        val snapshot = settings.worldState.picking
        val diagnostics = resolvePickingDiagnostics(
            selection = settings.worldState.pickingSelection,
            hoverTarget = snapshot?.picked,
            cursorScreen = com.badlogic.gdx.math.Vector2(x, y),
            cursorWorld = view.screenToWorld(x, y),
            cursorGrid = view.pickGrid(x, y),
            cursorTile = view.pickTile(x, y),
            world = world,
            tileCenterWorld = view::tileCenterWorld,
            objectOriginWorld = view::objectOriginWorld,
            entityWorld = view::entityWorld,
            worldToScreen = view::worldToScreen
        )
        val entityNames = diagnostics.entities.map { it.entity::class.displayName() }
        return diagnosticRows(
            "Mode" to diagnostics.mode.name.toDisplayName(),
            "Screen" to "${diagnostics.screen.x.format()}, ${diagnostics.screen.y.format()}",
            "World" to "${diagnostics.world.x.format()}, ${diagnostics.world.y.format()}",
            "Grid" to formatTilePosition(diagnostics.grid),
            "Tile" to (diagnostics.tile?.let(::formatTilePosition) ?: "—"),
            "Object" to display(
                diagnostics.placedObject?.placeable?.javaClass?.simpleName
            ),
            "Entity" to display(entityNames.takeIf { it.isNotEmpty() }),
            "Alpha" to alphaText(diagnostics.alphaAccepted),
            "Bounds" to display(diagnostics.bounds)
        )
    }

    private fun formatCamera(): List<DebugDiagnosticRow> {
        val camera = view.cameraDebugSnapshot()
        return diagnosticRows(
            "Position" to "${camera.x.format()}, ${camera.y.format()}",
            "Zoom" to camera.zoom.format(),
            "Viewport" to "${camera.viewportWidth.format()} x ${camera.viewportHeight.format()}",
            "Camera view" to camera.visibleArea.toString(),
            "World" to camera.worldBounds.toString(),
            "Clamp" to camera.clampBounds.toString()
        )
    }

    private fun formatCulling(): List<DebugDiagnosticRow> {
        val counts = cullingDebugCounts(view.renderDebugSnapshot)
        return diagnosticRows(
            "Objects" to "${counts.objectsDrawn} drawn, ${counts.objectsCulled} culled",
            "Entities" to "${counts.entitiesDrawn} drawn, ${counts.entitiesCulled} culled",
            "Object color" to "Cyan; dimmed when culled",
            "Entity color" to "Orange; dimmed when culled",
            "Area" to "Renderer bounds-overlap check"
        )
    }

    private fun syncContextFooter() {
        val displayedMode = toolRailState.settingsMode
        if (displayedMode == null) {
            contextRows.show(emptyList())
            return
        }
        val status = debugContextStatus(
            DebugContextInputs(
                mode = displayedMode,
                buildObject = buildSelection?.selected?.displayName(),
                placementAvailable = placement != null,
                placementDiagnostic = placement?.currentDiagnostic,
                buildDragging = tools.buildDragging,
                buildPreviewCount = tools.buildPreviewCount,
                paintTerrain = terrainSelection?.selected?.type?.toString()?.toDisplayName(),
                paintLayer = when (painter.target) {
                    DebugPaintTarget.GROUND -> "Ground"
                    DebugPaintTarget.OVERLAY -> "Overlay: ${painter.selectedOverlayLayerId?.toDisplayName()}"
                },
                spawnEntity = spawnSelection?.selected?.type?.displayName(),
                inspection = inspectionSummary(),
                pathWaypoints = pathfinding.waypoints,
                pathEntity = pathfinding.selectedEntity?.entity?.let {
                    it::class.displayName()
                },
                pathEntityWaiting = settings.worldState.pathfindingEntityWaiting,
                pathResult = pathfinding.result,
                movePreview = settings.worldState.movePreview
            )
        )
        contextRows.show(status?.rows.orEmpty())
    }

    private fun inspectionSummary(): String? = when (val selected = inspector.selection) {
        is DebugInspection.EntityTarget ->
            "${selected.entity.entity::class.displayName()} (Entity)"
        is DebugInspection.ObjectTarget ->
            "${selected.placedObject.placeable::class.displayName()} (Object)"
        is DebugInspection.TileTarget ->
            "${formatTilePosition(selected.position)} (Tile)"
        null -> null
    }

    private fun renderInspectionRows(
        placedObject: com.mefabc24.strata.world.PlacedObject? = null,
        entity: com.mefabc24.strata.world.WorldEntity? = null
    ): List<Pair<String, String>> {
        val item = view.renderDebugSnapshot
            ?.items
            ?.firstOrNull { snapshot ->
                when {
                    placedObject != null ->
                        snapshot.placedObject === placedObject

                    entity != null ->
                        snapshot.entity === entity

                    else -> false
                }
            }
            ?: return emptyList()

        val sort = item.sort

        return listOf(
            "Render index" to when (settings.renderOrder.mode) {
                RenderOrderDebugMode.CALCULATED -> item.index.toString()
                RenderOrderDebugMode.ACTUAL -> item.actualIndex?.toString() ?: "not drawn"
            },
            "Drawn" to item.drawn.toString(),
            "Render priority" to (sort?.renderPriority?.toString() ?: "unavailable"),
            "Sort volume" to if (sort != null) {
                "[${sort.minX.format()}, ${sort.maxX.format()}] x " +
                        "[${sort.minY.format()}, ${sort.maxY.format()}]"
            } else {
                "unavailable"
            },
            "Front Y" to (sort?.projectedFrontY?.format() ?: "unavailable"),
            "Render bounds" to display(item.bounds)
        )
    }

    private fun formatInspection(): List<DebugDiagnosticRow> =
        when (val selected = inspector.selection) {
        null -> diagnosticRows("Status" to "Click an entity, object, or tile")
        is DebugInspection.EntityTarget -> {
            val entity = selected.entity
            val visual = view.resolvedEntityVisual(entity)
            diagnosticRows(
                *(
                        listOf(
                            "Entity" to entity.entity::class.displayName(),
                            "Position" to formatEntityPosition(entity.position),
                            "Tile" to formatTilePosition(entity.currentTile),
                            "Direction" to entity.direction.toString(),
                            "Frozen" to settings.isEntityFrozen(entity).toString(),
                            "Animation frozen" to (
                                settings.isEntityFrozen(entity) &&
                                    settings.inspect.freezeEntityAnimation
                                ).toString(),
                            "Moving" to entity.isMoving.toString(),
                            "Waypoints" to entity.remainingWaypoints.size.toString(),
                            "Path" to formatTilePositions(entity.remainingPath),
                            "Animation" to animationText(
                                visual?.state?.toString(),
                                visual?.stateTime,
                                visual?.visual?.sprite
                            ),
                            "Sprite bounds" to display(view.entitySpriteBounds(entity))
                        ) + renderInspectionRows(entity = entity)
                        ).toTypedArray()
            )
        }
        is DebugInspection.ObjectTarget -> {
            val placed = selected.placedObject
            val visual = objects.resolve(placed, view.animationTime)
            diagnosticRows(
                *(
                        listOf(
                            "Object" to placed.placeable::class.displayName(),
                            "Origin" to "(${placed.x}, ${placed.y})",
                            "Footprint" to formatFootprint(placed.placeable.footprint),
                            "Occupied" to formatTilePositions(placed.occupiedTiles()),
                            "Animation" to animationText(
                                visual?.state?.toString(),
                                visual?.stateTime,
                                visual?.visual?.sprite
                            ),
                            "Sprite Bounds" to display(view.objectSpriteBounds(placed))
                        ) + renderInspectionRows(placedObject = placed)
                        ).toTypedArray()
            )
        }
        is DebugInspection.TileTarget -> {
            val position = selected.position
            val tile = world.getTile(position)
            val overlays = world.overlayLayerIds.mapNotNull { id ->
                world.getOverlayTile(id, position.x, position.y)?.let { id to terrainFor(it) }
            }
            val tileEntities = world.getEntities().filter { it.currentTile == position }
            diagnosticRows(
                "Tile" to formatTilePosition(position),
                "Terrain" to display(tile?.let(terrainFor)),
                "Overlays" to display(overlays.takeIf { it.isNotEmpty() }),
                "Object" to display(
                    world.getObjectAt(position)?.placeable?.javaClass?.simpleName
                ),
                "Entities" to display(
                    tileEntities.map { it.entity::class.simpleName }.takeIf { it.isNotEmpty() }
                )
            )
        }
    }

    private fun animationText(
        state: String?,
        stateTime: Float?,
        sprite: com.mefabc24.strata.render.sprite.SpriteFrames?
    ): String {
        if (sprite == null || stateTime == null) return "unavailable"
        return "${state ?: "default"}, t=${stateTime.format()}, " +
            "frame ${sprite.frameIndexAt(stateTime) + 1}/${sprite.frameCount}, " +
            (sprite.frameDuration?.let { "${it.format()} s" } ?: "static")
    }

    private fun alphaText(value: Boolean?): String = when (value) {
        true -> "accepted"
        false -> "rejected"
        null -> "unavailable"
    }

    private companion object {
        val DISABLED_COLOR_PREVIEW = Color(0.12f, 0.12f, 0.14f, 1f)
    }
}

internal fun diagnosticRows(
    vararg rows: Pair<String, String>
): List<DebugDiagnosticRow> = rows.map { (key, value) ->
    DebugDiagnosticRow(key, value)
}

private fun display(value: Any?): String = value?.toString() ?: "—"
private fun Float.format() = String.format(Locale.ROOT, "%.2f", this)
private fun Double.format() = String.format(Locale.ROOT, "%.2f", this)
private fun String.toDisplayName() = replace('_', ' ').replace('-', ' ')
    .lowercase().replaceFirstChar(Char::titlecase)
private fun kotlin.reflect.KClass<*>.displayName() = simpleName?.toDisplayName() ?: toString()
private fun ObjectEntry.displayName() = type.displayName()
