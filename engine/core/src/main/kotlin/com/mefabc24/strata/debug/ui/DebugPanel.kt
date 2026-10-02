package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
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
    private val navigation = DebugPanelNavigation()
    private val tabs = ui.selectionGroup(DebugPanelTab.entries, DebugPanelTab.TOOLS) { tab ->
        navigation.select(tab)
        hidePreview(); syncVisibility()
    }
    private val debugSections = ui.selectionGroup(
        DebugPanelSection.entries,
        DebugPanelSection.VISUALS
    ) { section ->
        navigation.select(section)
        syncDebugSectionVisibility(resetScroll = true)
    }
    private val modes = buildList {
        add(DebugToolMode.NONE)
        add(DebugToolMode.INSPECT)
        add(DebugToolMode.MOVE)
        add(DebugToolMode.FREE_CAMERA)
        if (tools.buildAvailable && buildEntries.isNotEmpty()) add(DebugToolMode.BUILD)
        add(DebugToolMode.DELETE)
        if (painter.entries.isNotEmpty()) add(DebugToolMode.PAINT)
        if (spawner.entries.isNotEmpty()) add(DebugToolMode.SPAWN)
        add(DebugToolMode.PATHFINDING)
    }
    private val modeSelection = ui.selectionGroup(modes, DebugToolMode.NONE) {
        hidePreview(); tools.select(it); syncVisibility()
    }
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

    private lateinit var panelActor: StrataPanel
    private lateinit var bodyScroll: StrataScrollPane
    private lateinit var toolsTab: StrataColumn
    private lateinit var debugTab: StrataColumn
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
    private lateinit var contextFooter: StrataColumn
    private lateinit var contextFooterCell: Cell<StrataColumn>
    private lateinit var contextRows: DebugDiagnosticTable
    private lateinit var previewName: Label
    private lateinit var previewImage: Image
    private lateinit var previewPopover: StrataPopover
    private lateinit var searchEmptyLabel: Label
    private val debugSectionColumns = linkedMapOf<DebugPanelSection, StrataColumn>()
    private val debugSectionButtons = linkedMapOf<DebugPanelSection, StrataSelectableButton<DebugPanelSection>>()
    private lateinit var expandAllButton: StrataButton
    private lateinit var collapseAllButton: StrataButton
    private val debugCategories = mutableListOf<DebugCategoryActor>()
    private var debugSearch: DebugSettingsSearch? = null
    private var registeringDebugSection: DebugPanelSection = DebugPanelSection.VISUALS
    private var matchingDebugSections: Set<DebugPanelSection> = DebugPanelSection.entries.toSet()
    private var previewAnchor: Actor? = null
    private var lastTab = DebugPanelTab.TOOLS
    private var lastMode = DebugToolMode.NONE

    init {
        buildSelection?.selected?.let(tools::selectBuildEntry)
        terrainSelection?.selected?.let { painter.selectedEntry = it }
        spawnSelection?.selected?.let { spawner.selectedEntry = it }
        buildUi()
        syncDebugSectionVisibility(resetScroll = false)
        buildPreview()
        synchronizers += { modeSelection.select(tools.mode) }
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
        setPanelVisible(settings.panel.visibleOnStartup)
        syncControls()
        syncVisibility()
    }

    fun setPanelVisible(visible: Boolean) {
        settings.panel.visible = visible
        ui.root.isVisible = visible
        if (!visible) hidePreview()
    }

    fun update(delta: Float) {
        statsOverlay.update(delta)
        simulationOverlay.setVisible(settings.simulation.enabled)
        simulationOverlay.update(delta)
        if (ui.root.isVisible != settings.panel.visible) {
            ui.root.isVisible = settings.panel.visible
            if (!settings.panel.visible) hidePreview()
        }
        syncControls()
        syncVisibility()
        syncDiagnostics()
    }

    fun resized() {
        val anchor = previewAnchor ?: return
        if (previewState.current != null) previewPopover.showRightOf(panelActor, anchor)
    }

    private fun buildUi() {
        ui.root.pad(12f)

        panelActor = ui.panel(
            spacing = 8f,
            padding = StrataInsets(
                top = 10f,
                left = 10f,
                bottom = 10f,
                right = 10f
            )
        ) {
            defaults().fillAvailableX()

            // Main navigation
            row(spacing = 6f) {
                defaults()
                    .fillAvailableX()
                    .uniformX()
                    .height(40f)

                DebugPanelTab.entries.forEach {
                    selectableButton(it.label, it, tabs)
                }
            }

            separator()

            // Scrollable content
            bodyScroll = scrollColumn(spacing = 8f) {
                defaults().fillAvailableX()

                stack {
                    toolsTab = column(spacing = 10f) {
                        defaults().fillAvailableX()
                        buildTools()
                    }

                    debugTab = column(spacing = 10f) {
                        defaults().fillAvailableX()
                        buildDebug()
                    }
                }.cell {
                    fillAvailableX()
                }
            }.cell {
                fillAvailableX()
                minHeight(0f)
                prefHeight(Value.prefHeight)
                maxHeight(Value.percentHeight(0.75f, ui.root))
            }

            // Fixed status footer
            contextFooter = column(spacing = 3f) {
                defaults().fillAvailableX()

                separator()

                label("STATUS").cell {
                    height(24f)
                    left()
                }

                contextRows = diagnosticTable()
            }

            contextFooterCell = getCell(contextFooter).apply {
                height(DebugContextFooterLayout.reservedHeight)
                padTop(4f)
            }
        }.cell {
            minWidth(260f)
            prefWidth(Value.percentWidth(0.34f, ui.root))
            maxWidth(460f)

            prefHeight(Value.prefHeight)
            maxHeight(Value.percentHeight(0.95f, ui.root))
            top()
            left()
        }
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

    private fun StrataColumn.buildTools() {
        label("Mode")
        responsiveGrid(105f, 40f, maximumColumns = 3) {
            modes.forEach { selectableButton(it.displayName, it, modeSelection) }
        }.cell { fillAvailableX() }
        if (!tools.buildAvailable || buildEntries.isEmpty()) {
            wrappingLabel(
                if (!tools.buildAvailable) "Build unavailable: no PlacementController"
                else "Build unavailable: no constructible objects"
            )
        }
        stack {
            buildControls = column(spacing = 8f) {
                defaults().fillAvailableX()
                label("Build object")
                responsiveGrid(130f, maximumColumns = 2) {
                    buildEntries.forEach { entry ->
                        selectableButton(entry.displayName(), entry, checkNotNull(buildSelection))
                            .previewOnHover(entry, DebugContentKind.OBJECT, entry.displayName(), entry.selectionVisual.texture)
                    }
                }.cell { fillAvailableX() }
            }
            deleteControls = column(spacing = 8f) {
                defaults().fillAvailableX()
                label("Delete brush")
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
            paintControls = column(spacing = 8f) {
                defaults().fillAvailableX()
                label("Paint brush")
                brushSizeStepper("Brush size", { settings.paint.brushSize }) {
                    settings.paint.brushSize = it
                }
                simpleToggle("Show affected area", { settings.paint.showBrushPreview }) {
                    settings.paint.showBrushPreview = it
                }
                label("Terrain")
                responsiveGrid(130f, maximumColumns = 2) {
                    painter.entries.forEach { entry ->
                        val name = entry.type.toString().toDisplayName()
                        selectableButton(name, entry, checkNotNull(terrainSelection))
                            .previewOnHover(entry, DebugContentKind.TERRAIN, name, entry.selectionTexture)
                    }
                }.cell { fillAvailableX() }
                label("Terrain target")
                responsiveGrid(130f, maximumColumns = 2) {
                    paintTargets.forEach { target ->
                        selectableButton(target.name.toDisplayName(), target, paintTargetSelection)
                    }
                }.cell { fillAvailableX() }
                overlayPaintControls = column(spacing = 6f) {
                    defaults().fillAvailableX()
                    label("Overlay")
                    responsiveGrid(130f, maximumColumns = 2) {
                        painter.overlayLayerIds.forEach { id ->
                            selectableButton(id.toDisplayName(), id, checkNotNull(overlaySelection))
                        }
                    }.cell { fillAvailableX() }
                }
            }
            spawnControls = column(spacing = 8f) {
                defaults().fillAvailableX()
                label("Spawn entity")
                responsiveGrid(130f, maximumColumns = 2) {
                    spawner.entries.forEach { entry ->
                        val name = entry.type.displayName()
                        selectableButton(name, entry, checkNotNull(spawnSelection))
                            .previewOnHover(entry, DebugContentKind.ENTITY, name, entry.selectionVisual.texture)
                    }
                }.cell { fillAvailableX() }
            }
            inspectControls = column(spacing = 8f) {
                defaults().fillAvailableX()
                label("Inspector")
                inspectorRows = diagnosticTable()
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
                button("Unfreeze all entities") {
                    val count = settings.unfreezeAllEntities()
                    settings.notify(
                        if (count == 0) "No frozen entities" else "Unfroze $count entities",
                        if (count == 0) DebugNotificationSeverity.INFO else DebugNotificationSeverity.SUCCESS
                    )
                }.cell { height(38f) }
                label("World visualization")
                label("Tile")
                toggleGrid(
                    toggle("Selected tile", { settings.inspect.showTile }) {
                        settings.inspect.showTile = it
                    }
                )
                label("Object")
                toggleGrid(
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
                label("Entity")
                toggleGrid(
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
                button("Clear selection") { inspector.clear() }.cell { height(38f) }
            }
            pathControls = column(spacing = 8f) {
                defaults().fillAvailableX()
                label("Pathfinding")
                simpleToggle(
                    "World visualization",
                    { settings.pathfinding.enabled }
                ) { settings.pathfinding.enabled = it }
                label("Movement")
                val movementModes = ui.selectionGroup(
                    PathMovementMode.entries,
                    settings.pathfinding.movementMode
                ) {
                    settings.pathfinding.movementMode = it
                }
                synchronizers += {
                    movementModes.select(settings.pathfinding.movementMode)
                }
                responsiveGrid(105f, 36f, maximumColumns = 2) {
                    selectableButton("4-way", PathMovementMode.FOUR_WAY, movementModes)
                    selectableButton("8-way", PathMovementMode.EIGHT_WAY, movementModes)
                }.cell { fillAvailableX() }
                toggleGrid(
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
                label("Diagnostic search")
                responsiveGrid(105f, 38f, maximumColumns = 2) {
                    button("Start") { pathfinding.startDiagnosticSearch() }
                    button("Step") { pathfinding.stepDiagnosticSearch() }
                    button("Continue") { pathfinding.continueDiagnosticSearch() }
                    button("Pause") { pathfinding.pauseDiagnosticSearch() }
                    button("Reset") { pathfinding.resetDiagnosticSearch() }
                }.cell { fillAvailableX() }
                boundStepper(
                    "Iterations / update",
                    { settings.pathfinding.automaticIterationsPerUpdate.toFloat() },
                    1f,
                    64f,
                    1f
                ) { settings.pathfinding.automaticIterationsPerUpdate = it.toInt() }
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
                button("Clear path") {
                    if (pathfinding.clear()) {
                        settings.notify("Path cleared", DebugNotificationSeverity.INFO)
                    }
                }.cell { height(38f) }
            }
        }.cell { fillAvailableX() }
    }

    private fun StrataColumn.buildDebug() {
        label("Find settings")
        actor(TextField("", ui.skin).apply {
            messageText = "Search by setting name"
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    applyDebugSearch(text)
                }
            })
        }).cell { fillAvailableX(); height(38f) }
        responsiveGrid(112f, 36f, maximumColumns = 4) {
            DebugPanelSection.entries.forEach { section ->
                debugSectionButtons[section] =
                    selectableButton(section.label, section, debugSections)
            }
        }.cell { fillAvailableX() }
        responsiveGrid(120f, 36f, maximumColumns = 2) {
            expandAllButton = button("Expand all") { setAllDebugCategoriesExpanded(true) }
            collapseAllButton = button("Collapse all") { setAllDebugCategoriesExpanded(false) }
        }.cell { fillAvailableX() }
        searchEmptyLabel = wrappingLabel("No debug settings match this search.").apply {
            isVisible = false
        }
        getCell(searchEmptyLabel).height(0f)
        separator()
        stack {
            DebugPanelSection.entries.forEach { section ->
                debugSectionColumns[section] = column(spacing = 8f) {
                    defaults().fillAvailableX()
                }
            }
        }.cell { fillAvailableX() }

        val visuals = debugSectionColumns.getValue(DebugPanelSection.VISUALS)
        val diagnostics = debugSectionColumns.getValue(DebugPanelSection.DIAGNOSTICS)
        val runtime = debugSectionColumns.getValue(DebugPanelSection.RUNTIME)
        val presets = debugSectionColumns.getValue(DebugPanelSection.PRESETS)

        presets.label("Presets")

        // Disable every visual diagnostic without changing operational state.
        presets.button("OFF") {
            settings.applyPreset(DebugPreset.OFF)
            syncControls()
        }.cell {
            fillAvailableX()
            height(38f)
        }

        presets.responsiveGrid(
            minimumItemWidth = 105f,
            itemHeight = 38f,
            maximumColumns = 3
        ) {
            button("Default") {
                settings.applyDefaultVisualConfiguration()
                syncControls()
            }

            DebugPreset.entries
                .filterNot { it == DebugPreset.OFF }
                .forEach { preset ->
                    button(preset.name.toDisplayName()) {
                        settings.applyPreset(preset)
                        syncControls()
                    }
                }
        }.cell {
            fillAvailableX()
        }

        // Replace the previously saved default.
        presets.button("Save current configuration as default") {
            settings.saveDefaultVisualConfiguration()

            settings.notify(
                "Default debug configuration saved",
                DebugNotificationSeverity.SUCCESS
            )
        }.cell {
            fillAvailableX()
            height(38f)
        }

        registeringDebugSection = DebugPanelSection.VISUALS
        visuals.settingsExpander("Target filter", DebugVisualCategory.GENERAL) {
            label("Visualization filter")
            val visualizationFilters = ui.selectionGroup(
                DebugVisualizationFilter.entries,
                settings.visualizationFilter
            ) {
                settings.visualizationFilter = it
            }
            synchronizers += {
                visualizationFilters.select(settings.visualizationFilter)
            }
            responsiveGrid(105f, 36f, maximumColumns = 4) {
                selectableButton("All", DebugVisualizationFilter.ALL, visualizationFilters)
                selectableButton("Selected", DebugVisualizationFilter.SELECTED, visualizationFilters)
                selectableButton("Hovered", DebugVisualizationFilter.HOVERED, visualizationFilters)
                selectableButton("Visible", DebugVisualizationFilter.VISIBLE, visualizationFilters)
            }.cell { fillAvailableX() }
        }

        runtime.settingsExpander("Performance", DebugVisualCategory.GENERAL) {
            toggleGrid(
                toggle("Performance overlay", { settings.performance.overlayEnabled }) {
                    settings.performance.overlayEnabled = it
                },
                toggle("World stats overlay", { settings.worldStats.enabled }) {
                    settings.worldStats.enabled = it
                }
            )
        }
        runtime.settingsExpander("Performance history") {
            toggleGrid(
                toggle("On-screen overlay", { settings.performance.overlayEnabled }) {
                    settings.performance.overlayEnabled = it
                },
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
            label("Graph metric")
            val metrics = ui.selectionGroup(
                DebugPerformanceMetric.entries,
                settings.performance.historyMetric
            ) { settings.performance.historyMetric = it }
            synchronizers += { metrics.select(settings.performance.historyMetric) }
            responsiveGrid(105f, 36f, maximumColumns = 2) {
                selectableButton("Frame", DebugPerformanceMetric.FRAME_TIME, metrics)
                selectableButton("Render", DebugPerformanceMetric.RENDER_TIME, metrics)
                selectableButton("Static plan", DebugPerformanceMetric.STATIC_PLAN_TIME, metrics)
                selectableButton("Dynamic plan", DebugPerformanceMetric.DYNAMIC_PLAN_TIME, metrics)
            }.cell { fillAvailableX() }
            responsiveGrid(105f, 38f, maximumColumns = 3) {
                button("Start") { settings.performance.startHistoryRecording() }
                button("Stop") { settings.performance.stopHistoryRecording() }
                button("Clear") { settings.performance.clearHistory() }
            }.cell { fillAvailableX() }
        }
        runtime.featureExpander(
            "Simulation",
            read = { settings.simulation.enabled },
            write = { settings.simulation.enabled = it }
        ) {
            boundStepper(
                "Custom time scale",
                { simulation.timeScale },
                0.05f,
                16f,
                0.05f
            ) { simulation.timeScale = it }
            simpleToggle(
                "Freeze visual animations",
                { settings.simulation.freezeVisualAnimations }
            ) { settings.simulation.freezeVisualAnimations = it }
            button("Reset to normal speed") {
                simulation.resetTimeScale()
            }.cell { height(38f) }
        }
        registeringDebugSection = DebugPanelSection.VISUALS
        visuals.settingsExpander("World visibility", DebugVisualCategory.WORLD_VISIBILITY) { buildWorldVisibilitySettings() }
        visuals.featureExpander(
            "Render order",
            DebugVisualCategory.RENDER_ORDER,
            { settings.renderOrder.enabled },
            { settings.renderOrder.enabled = it }
        ) {
            label("Order")
            val modes = ui.selectionGroup(
                RenderOrderDebugMode.entries,
                settings.renderOrder.mode
            ) {
                settings.renderOrder.mode = it
            }
            synchronizers += { modes.select(settings.renderOrder.mode) }
            responsiveGrid(130f, 36f, maximumColumns = 2) {
                selectableButton("Calculated", RenderOrderDebugMode.CALCULATED, modes)
                selectableButton("Actual", RenderOrderDebugMode.ACTUAL, modes)
            }.cell { fillAvailableX() }
            toggleGrid(
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
            label("Sort geometry")
            toggleGrid(
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
            label("Priority focus")
            val priorityFocus = ui.selectionGroup(
                RenderPriorityFocusMode.entries,
                settings.renderOrder.priorityFocusMode
            ) {
                settings.renderOrder.priorityFocusMode = it
            }
            synchronizers += {
                priorityFocus.select(settings.renderOrder.priorityFocusMode)
            }
            responsiveGrid(90f, 36f, maximumColumns = 3) {
                selectableButton("Off", RenderPriorityFocusMode.OFF, priorityFocus)
                selectableButton("Highlight", RenderPriorityFocusMode.HIGHLIGHT, priorityFocus)
                selectableButton("Isolate", RenderPriorityFocusMode.ISOLATE, priorityFocus)
            }.cell { fillAvailableX() }
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
            label("Heatmap color steps")
            val heatmapSteps = ui.selectionGroup(
                TerrainHeatmapSteps.entries,
                settings.renderOrder.terrainHeatmapSteps
            ) {
                settings.renderOrder.terrainHeatmapSteps = it
            }
            synchronizers += {
                heatmapSteps.select(settings.renderOrder.terrainHeatmapSteps)
            }
            val heatmapStepButtons =
                mutableListOf<StrataSelectableButton<TerrainHeatmapSteps>>()
            responsiveGrid(64f, 36f, maximumColumns = 4) {
                heatmapStepButtons += selectableButton(
                    "Per tile",
                    TerrainHeatmapSteps.PER_TILE,
                    heatmapSteps
                )
                heatmapStepButtons += selectableButton(
                    "32",
                    TerrainHeatmapSteps.STEPS_32,
                    heatmapSteps
                )
                heatmapStepButtons += selectableButton(
                    "16",
                    TerrainHeatmapSteps.STEPS_16,
                    heatmapSteps
                )
                heatmapStepButtons += selectableButton(
                    "8",
                    TerrainHeatmapSteps.STEPS_8,
                    heatmapSteps
                )
            }.cell { fillAvailableX() }
            synchronizers += {
                heatmapStepButtons.forEach { button ->
                    button.isDisabled = !settings.renderOrder.showTerrainHeatmap
                }
            }
        }
        registeringDebugSection = DebugPanelSection.DIAGNOSTICS
        diagnostics.featureExpander(
            "Event Bus Monitor",
            DebugVisualCategory.EVENT_MONITOR,
            { settings.eventBus.enabled },
            { settings.eventBus.enabled = it }
        ) {
            toggleGrid(
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
            button("Clear history") {
                if (eventMonitor.records.isNotEmpty()) {
                    eventMonitor.clear()
                    settings.notify("Event history cleared", DebugNotificationSeverity.INFO)
                }
            }.cell { height(38f) }
        }
        registeringDebugSection = DebugPanelSection.VISUALS
        visuals.featureExpander("Grid", DebugVisualCategory.GRID, { settings.grid.enabled }, { settings.grid.enabled = it }) { buildGridSettings() }
        visuals.featureExpander(
            "World information",
            DebugVisualCategory.WORLD_INFORMATION,
            { settings.worldInfo.enabled },
            { settings.worldInfo.enabled = it }
        ) { buildWorldInfoSettings() }
        visuals.featureExpander("Objects", DebugVisualCategory.OBJECTS, { settings.objects.enabled }, { settings.objects.enabled = it }) { buildObjectSettings() }
        visuals.featureExpander("Entities", DebugVisualCategory.ENTITIES, { settings.entities.enabled }, { settings.entities.enabled = it }) { buildEntitySettings() }

        registeringDebugSection = DebugPanelSection.DIAGNOSTICS
        diagnostics.featureExpander("Picking", DebugVisualCategory.PICKING, { settings.picking.enabled }, { settings.picking.enabled = it }) {
            toggleGrid(
                toggle("Sprite bounds", { settings.picking.showSpriteBounds }) { settings.picking.showSpriteBounds = it },
                toggle("Cursor marker", { settings.picking.showCursorHit }) { settings.picking.showCursorHit = it }
            )
            pickingRows = diagnosticTable()
            button("Clear locked target") {
                settings.worldState.pickingSelection.clear()
            }.cell { height(38f) }
        }
        diagnostics.featureExpander("Culling", DebugVisualCategory.CULLING, { settings.culling.enabled }, { settings.culling.enabled = it }) {
            toggleGrid(
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
        diagnostics.featureExpander("Camera", DebugVisualCategory.CAMERA, { settings.camera.enabled }, { settings.camera.enabled = it }) {
            toggleGrid(
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

        registeringDebugSection = DebugPanelSection.RUNTIME
        runtime.featureExpander(
            "Notifications",
            DebugVisualCategory.NOTIFICATIONS,
            { settings.notifications.enabled },
            { settings.notifications.enabled = it }
        ) {
            val positions = ui.selectionGroup(
                DebugNotificationPosition.entries,
                settings.notifications.position
            ) {
                settings.notifications.position = it
            }

            synchronizers += {
                positions.select(settings.notifications.position)
            }

            responsiveGrid(
                minimumItemWidth = 130f,
                maximumColumns = 2
            ) {
                selectableButton(
                    "Center top",
                    DebugNotificationPosition.TOP_CENTER,
                    positions
                )

                selectableButton(
                    "Bottom right",
                    DebugNotificationPosition.BOTTOM_RIGHT,
                    positions
                )
            }.cell { fillAvailableX() }
        }
        debugSearch = DebugSettingsSearch(debugCategories.map { it.searchCategory })
    }

    private fun StrataColumn.buildGridSettings() {
        label("Layer")
        val layers = ui.selectionGroup(DebugGridRenderLayer.entries, settings.grid.renderLayer) {
            settings.grid.renderLayer = it
        }
        synchronizers += { layers.select(settings.grid.renderLayer) }
        responsiveGrid(130f, maximumColumns = 2) {
            selectableButton("Below objects", DebugGridRenderLayer.BELOW_OBJECTS, layers)
            selectableButton("Above objects", DebugGridRenderLayer.ABOVE_OBJECTS, layers)
        }.cell { fillAvailableX() }
        label("Extent")
        val extents = ui.selectionGroup(DebugGridExtent.entries, settings.grid.extent) {
            settings.grid.extent = it
        }
        synchronizers += { extents.select(settings.grid.extent) }
        responsiveGrid(130f, maximumColumns = 2) {
            selectableButton("World", DebugGridExtent.WORLD, extents)
            selectableButton("Visible", DebugGridExtent.VISIBLE, extents)
        }.cell { fillAvailableX() }
        toggleGrid(
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
        boundStepper("Grid alpha", { settings.grid.color.a }, 0f, 1f, 0.05f) { alpha ->
            settings.grid.color = settings.grid.color.apply { a = alpha }
        }
        boundStepper("Hover alpha", { settings.grid.hoverColor.a }, 0f, 1f, 0.05f) { alpha ->
            settings.grid.hoverColor = settings.grid.hoverColor.apply { a = alpha }
        }
        boundStepper("Background alpha", { settings.grid.backgroundColor?.a ?: 0.2f }, 0f, 1f, 0.05f) { alpha ->
            settings.grid.backgroundColor?.let { settings.grid.backgroundColor = it.apply { a = alpha } }
        }
        boundStepper("Hover background alpha", { settings.grid.hoverBackgroundColor?.a ?: 0.5f }, 0f, 1f, 0.05f) { alpha ->
            settings.grid.hoverBackgroundColor?.let { settings.grid.hoverBackgroundColor = it.apply { a = alpha } }
        }
    }

    private fun StrataColumn.buildObjectSettings() {
        toggleGrid(
            toggle("Occupied tiles", { settings.objects.showOccupiedTiles }) { settings.objects.showOccupiedTiles = it },
            toggle("Origin tile", { settings.objects.showOriginTile }) { settings.objects.showOriginTile = it },
            toggle("Sprite bounds", { settings.objects.showSpriteBounds }) { settings.objects.showSpriteBounds = it },
            toggle("Tile fill", { settings.objects.occupiedTileFillColor != null }) { enabled ->
                settings.objects.occupiedTileFillColor =
                    if (enabled) Color(0.2f, 0.65f, 1f, 0.18f) else null
            }
        )
        boundStepper("Line width", { settings.objects.lineWidth }, 0.25f, 8f, 0.25f) {
            settings.objects.lineWidth = it
        }
        boundStepper("Fill alpha", { settings.objects.occupiedTileFillColor?.a ?: 0.18f }, 0f, 1f, 0.05f) { alpha ->
            settings.objects.occupiedTileFillColor?.let {
                settings.objects.occupiedTileFillColor = it.apply { a = alpha }
            }
        }
    }

    private fun StrataColumn.buildEntitySettings() {
        toggleGrid(
            toggle("Current tile", { settings.entities.showCurrentTile }) { settings.entities.showCurrentTile = it },
            toggle("Position", { settings.entities.showPosition }) { settings.entities.showPosition = it },
            toggle("Path", { settings.entities.showPath }) { settings.entities.showPath = it },
            toggle("Direction", { settings.entities.showDirection }) { settings.entities.showDirection = it },
            toggle("Sprite bounds", { settings.entities.showSpriteBounds }) { settings.entities.showSpriteBounds = it },
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
            },
            toggle("Tile fill", { settings.entities.currentTileFillColor != null }) { enabled ->
                settings.entities.currentTileFillColor =
                    if (enabled) Color(0.3f, 1f, 0.3f, 0.16f) else null
            }
        )
        boundStepper("Line width", { settings.entities.lineWidth }, 0.25f, 8f, 0.25f) {
            settings.entities.lineWidth = it
        }
        boundStepper("Fill alpha", { settings.entities.currentTileFillColor?.a ?: 0.16f }, 0f, 1f, 0.05f) { alpha ->
            settings.entities.currentTileFillColor?.let {
                settings.entities.currentTileFillColor = it.apply { a = alpha }
            }
        }
        label("Movement trail")
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
        button("Clear movement trails") {
            settings.entities.clearMovementTrails()
        }.cell { height(38f) }
    }

    private fun StrataColumn.buildWorldInfoSettings() {
        toggleGrid(
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
        boundStepper(
            "Occupancy alpha",
            { settings.worldInfo.occupancyColor.a },
            0f,
            1f,
            0.05f
        ) { alpha ->
            settings.worldInfo.occupancyColor = settings.worldInfo.occupancyColor.apply { a = alpha }
        }
        boundStepper(
            "Missing visual alpha",
            { settings.worldInfo.missingVisualColor.a },
            0f,
            1f,
            0.05f
        ) { alpha ->
            settings.worldInfo.missingVisualColor =
                settings.worldInfo.missingVisualColor.apply { a = alpha }
        }
    }

    private fun StrataColumn.buildWorldVisibilitySettings() {
        toggleGrid(
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
            label("Overlay layers")
            overlayIds.forEach { layerId ->
                simpleToggle(
                    layerId,
                    { settings.worldVisibility.isOverlayLayerVisible(layerId) }
                ) { visible ->
                    settings.worldVisibility.setOverlayLayerVisible(layerId, visible)
                }
            }
        }
        button("Show all categories") {
            settings.worldVisibility.showAll()
        }.cell { height(38f) }
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
        row(spacing = 8f) {
            label(text).cell { growX(); left() }
            settingToggle(read, write).cell { minWidth(64f); height(36f) }
        }
    }

    private fun StrataColumn.featureExpander(
        title: String,
        category: DebugVisualCategory? = null,
        read: () -> Boolean,
        write: (Boolean) -> Unit,
        configure: StrataColumn.() -> Unit
    ) {
        val expander = expander(
            title = title,
            expanded = false,
            spacing = 8f,
            headerHeight = 36f,
            expandedStyle = debugExpanderStyle(),
            headerContent = {
                settingToggle(read, write).cell { minWidth(64f); height(36f) }
            }
        ) {
            defaults().fillAvailableX()
            configure()
            separator()
        }
        val cell = getCell(expander).apply { fillAvailableX() }
        registerDebugCategory(expander, cell)
    }

    private fun StrataColumn.settingsExpander(
        title: String,
        category: DebugVisualCategory? = null,
        configure: StrataColumn.() -> Unit
    ) {
        val expander = expander(
            title = title,
            expanded = false,
            spacing = 8f,
            headerHeight = 36f,
            expandedStyle = debugExpanderStyle()
        ) {
            defaults().fillAvailableX()
            configure()
            separator()
        }
        val cell = getCell(expander).apply { fillAvailableX() }
        registerDebugCategory(expander, cell)
    }

    private data class DebugCategoryActor(
        val section: DebugPanelSection,
        val expander: StrataExpander,
        val cell: Cell<StrataExpander>,
        val searchCategory: DebugSearchCategory
    )

    private fun registerDebugCategory(expander: StrataExpander, cell: Cell<StrataExpander>) {
        val terms = linkedSetOf<String>()
        collectActorText(expander.content, terms)
        debugCategories += DebugCategoryActor(
            registeringDebugSection,
            expander,
            cell,
            DebugSearchCategory(
                expander.title,
                terms,
                expander.expanded,
                registeringDebugSection
            )
        )
    }

    private fun collectActorText(actor: Actor, destination: MutableSet<String>) {
        if (actor is Label) {
            actor.text.toString().trim().takeIf(String::isNotEmpty)?.let(destination::add)
        }
        if (actor is Group) actor.children.forEach { collectActorText(it, destination) }
    }

    private fun applyDebugSearch(query: String) {
        val search = debugSearch ?: return
        debugCategories.forEach { it.searchCategory.expanded = it.expander.expanded }
        val result = search.update(query)
        debugCategories.forEachIndexed { index, entry ->
            val visible = index in result.matchingIndices
            entry.expander.isVisible = visible
            entry.cell.height(if (visible) Value.prefHeight else Value.Fixed(0f))
            entry.cell.space(if (visible) 10f else 0f)
            entry.expander.expanded = entry.searchCategory.expanded
        }
        val presetMatches = query.isBlank() || actorContainsText(
            debugSectionColumns.getValue(DebugPanelSection.PRESETS),
            query
        )
        matchingDebugSections = buildSet {
            addAll(result.matchingSections)
            if (presetMatches) add(DebugPanelSection.PRESETS)
        }
        val hasMatches = matchingDebugSections.isNotEmpty()
        if (query.isNotBlank() && navigation.selectedDebugSection !in matchingDebugSections) {
            matchingDebugSections.firstOrNull()?.let(debugSections::select)
        }
        searchEmptyLabel.isVisible = !hasMatches
        requireNotNull(debugTab.getCell(searchEmptyLabel)).height(
            if (hasMatches) Value.Fixed(0f) else Value.prefHeight
        )
        syncDebugSectionVisibility(resetScroll = false)
    }

    private fun setAllDebugCategoriesExpanded(expanded: Boolean) {
        val indices = debugCategories.indices.filter {
            debugCategories[it].section == navigation.selectedDebugSection
        }.toSet()
        debugSearch?.setExpanded(indices, expanded)
        debugCategories.filter { it.section == navigation.selectedDebugSection }.forEach {
            it.searchCategory.expanded = expanded
            if (it.expander.isVisible) it.expander.expanded = expanded
        }
        debugTab.invalidateHierarchy()
        bodyScroll.invalidateHierarchy()
    }

    private fun actorContainsText(actor: Actor, query: String): Boolean {
        val normalized = query.trim()
        if (normalized.isEmpty()) return true
        val terms = linkedSetOf<String>()
        collectActorText(actor, terms)
        return terms.any { it.contains(normalized, ignoreCase = true) }
    }

    private fun syncDebugSectionVisibility(resetScroll: Boolean) {
        if (debugSectionColumns.isEmpty()) return
        val selected = navigation.selectedDebugSection
        val searching = debugSearch?.query?.isNotBlank() == true
        debugSectionColumns.forEach { (section, column) ->
            column.isVisible = section == selected && section in matchingDebugSections
        }
        debugSectionButtons.forEach { (section, button) ->
            button.isDisabled = searching && section !in matchingDebugSections
        }
        val hasExpandableCategories = debugCategories.any { it.section == selected }
        if (::expandAllButton.isInitialized) {
            expandAllButton.isDisabled = !hasExpandableCategories
            collapseAllButton.isDisabled = !hasExpandableCategories
        }
        if (resetScroll && ::bodyScroll.isInitialized) bodyScroll.scrollY = 0f
        debugTab.invalidateHierarchy()
        if (::bodyScroll.isInitialized) {
            bodyScroll.content.invalidateHierarchy()
            bodyScroll.invalidateHierarchy()
        }
    }

    private fun debugExpanderStyle(): StrataExpanderStyle = ui.skin.get(
        "debug-expander",
        StrataExpanderStyle::class.java
    )

    private fun StrataColumn.toggleGrid(vararg controls: ToggleBinding) {
        responsiveGrid(130f, 36f, maximumColumns = 2) {
            controls.forEach { binding ->
                val button = StrataToggleButton(
                    binding.text, ui.skin, ui.theme.toggleButtonStyle,
                    binding.read(), binding.write
                )
                actor(button)
                synchronizers += { button.syncChecked(binding.read()) }
            }
        }.cell { fillAvailableX() }
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

    private fun StrataLayout.boundStepper(
        text: String,
        read: () -> Float,
        minimum: Float,
        maximum: Float,
        step: Float,
        write: (Float) -> Unit
    ): StrataNumericStepper {
        val stepper = numericStepper(
            text, read(), minimum, maximum, step, onChanged = write
        ).cell { fillAvailableX(); height(38f) }
        synchronizers += { stepper.sync(read()) }
        return stepper
    }

    private fun StrataLayout.brushSizeStepper(
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
        ).cell { fillAvailableX(); height(38f) }
        synchronizers += { stepper.sync(read().toFloat()) }
        return stepper
    }

    private fun StrataLayout.wrappingLabel(text: String): Label = label(text).apply {
        setWrap(true)
    }.cell { fillAvailableX() }

    private fun StrataLayout.diagnosticTable(): DebugDiagnosticTable {
        val table = actor(DebugDiagnosticTable(ui.skin))
        val cell = requireNotNull(getCell(table)).apply { fillAvailableX() }
        table.bindLayout(cell)
        return table
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
        if (!settings.panel.visible || tabs.selected != DebugPanelTab.TOOLS) return
        previewState.show(preview)
        previewAnchor = anchor
        previewName.setText(preview.name)
        previewImage.drawable = TextureRegionDrawable(preview.texture)
        previewPopover.showRightOf(panelActor, anchor)
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

    private fun syncVisibility() {
        if (!::toolsTab.isInitialized) return

        val tab = requireNotNull(tabs.selected)
        val mode = tools.mode

        val previousTab = lastTab
        val previousMode = lastMode
        val previousOverlayVisibility = overlayPaintControls.isVisible

        if (tab != lastTab || mode != lastMode) {
            hidePreview()
            bodyScroll.scrollY = 0f
            lastTab = tab
            lastMode = mode
        }

        toolsTab.isVisible = tab == DebugPanelTab.TOOLS
        debugTab.isVisible = tab == DebugPanelTab.DEBUG

        buildControls.isVisible = mode == DebugToolMode.BUILD
        deleteControls.isVisible = mode == DebugToolMode.DELETE
        paintControls.isVisible = mode == DebugToolMode.PAINT
        spawnControls.isVisible = mode == DebugToolMode.SPAWN
        inspectControls.isVisible = mode == DebugToolMode.INSPECT
        pathControls.isVisible = mode == DebugToolMode.PATHFINDING

        overlayPaintControls.isVisible =
            mode == DebugToolMode.PAINT &&
                    painter.target == DebugPaintTarget.OVERLAY

        if (
            tab != previousTab ||
            mode != previousMode ||
            overlayPaintControls.isVisible != previousOverlayVisibility
        ) {
            toolsTab.invalidateHierarchy()
            debugTab.invalidateHierarchy()
            bodyScroll.content.invalidateHierarchy()
            bodyScroll.invalidateHierarchy()
        }
    }

    private fun syncDiagnostics() {
        inspectorRows.show(formatInspection())
        pickingRows.show(if (settings.picking.enabled) formatPicking() else emptyList())
        cameraRows.show(if (settings.camera.enabled) formatCamera() else emptyList())
        cullingRows.show(if (settings.culling.enabled) formatCulling() else emptyList())
        syncContextFooter()
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
        val status = debugContextStatus(
            DebugContextInputs(
                mode = tools.mode,
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
