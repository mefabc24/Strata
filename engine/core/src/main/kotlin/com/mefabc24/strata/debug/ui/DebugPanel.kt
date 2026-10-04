package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.mefabc24.strata.debug.*
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.debug.tools.*
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.pathfinding.PathMovementMode
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.`object`.ObjectEntry
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.render.entity.EntityEntry
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.terrain.TerrainEntry
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.ui.*
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World

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
    private val modes = availableDebugToolModes(
        buildAvailable = tools.buildAvailable && buildEntries.isNotEmpty(),
        paintAvailable = painter.entries.isNotEmpty(),
        spawnAvailable = spawner.entries.isNotEmpty()
    )
    private val toolRailState = DebugToolRailState(modes.filter {
        debugToolFlyoutSectionTitles(it).isNotEmpty()
    }.toSet())
    private val toolWindowBuilder = DebugToolWindowBuilder(
        ui = ui,
        modes = modes,
        onToolSelected = ::selectTool,
        onSettingsRequested = ::toggleToolSettings
    )
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
    private val diagnostics = DebugPanelDiagnostics(
        settings = settings,
        tools = tools,
        painter = painter,
        inspector = inspector,
        pathfinding = pathfinding,
        placement = placement,
        world = world,
        view = view,
        terrainFor = terrainFor,
        objects = objects,
        toolRailState = toolRailState,
        buildSelection = buildSelection,
        terrainSelection = terrainSelection,
        spawnSelection = spawnSelection
    )

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
    private lateinit var moveControls: StrataColumn
    private lateinit var pathControls: StrataColumn
    private lateinit var cameraControls: StrataColumn
    private lateinit var inspectorRows: DebugDiagnosticTable
    private lateinit var pickingRows: DebugDiagnosticTable
    private lateinit var cameraRows: DebugDiagnosticTable
    private lateinit var cullingRows: DebugDiagnosticTable
    private val toolStatusRows = mutableMapOf<DebugToolMode, DebugDiagnosticTable>()
    private var appliedFlyoutMode: DebugToolMode? = null
    private var appliedWindowVisibility: Pair<Boolean, Boolean>? = null
    private var buildingDebugSettings = false

    init {
        buildSelection?.selected?.let(tools::selectBuildEntry)
        terrainSelection?.selected?.let { painter.selectedEntry = it }
        spawnSelection?.selected?.let { spawner.selectedEntry = it }
        buildUi()
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
            closeToolSettings()
        }
        syncWindowLayout()
    }

    fun setDebugWindowVisible(visible: Boolean) {
        if (!settings.debugWindow.enabled) return
        settings.debugWindow.visible = visible
        syncWindowLayout()
        syncStatsOverlayPosition()
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
            center()
            touchable = Touchable.childrenOnly
        }
        ui.actor(windowLayout).cell { grow(); fill() }
    }

    private fun buildToolRail() {
        val rail = toolWindowBuilder.buildRail()
        toolRailActor = rail.actor
        toolButtons.putAll(rail.buttons)
    }

    private fun buildToolFlyout() {
        val flyout = toolWindowBuilder.buildFlyout {
            defaults().fillAvailableX()
            buildingDebugSettings = true
            buildToolSettings()
            buildingDebugSettings = false
        }
        toolFlyoutActor = flyout.actor
        toolFlyoutScroll = flyout.scroll
        toolFlyoutHeader = flyout.header
    }

    private fun StrataColumn.buildToolSettings() {
        val content = DebugToolSettingsBuilder(
            ui = ui,
            settings = settings,
            tools = tools,
            painter = painter,
            spawner = spawner,
            buildEntries = buildEntries,
            placement = placement,
            inspector = inspector,
            pathfinding = pathfinding,
            buildSelection = buildSelection,
            terrainSelection = terrainSelection,
            spawnSelection = spawnSelection,
            paintTargets = paintTargets,
            paintTargetSelection = paintTargetSelection,
            overlaySelection = overlaySelection,
            synchronizers = synchronizers,
            onLayoutChanged = ::invalidateToolFlyoutLayout
        ).build(this)
        buildControls = content.buildControls
        deleteControls = content.deleteControls
        paintControls = content.paintControls
        overlayPaintControls = content.overlayPaintControls
        spawnControls = content.spawnControls
        inspectControls = content.inspectControls
        moveControls = content.moveControls
        pathControls = content.pathControls
        cameraControls = content.cameraControls
        inspectorRows = content.inspectorRows
        toolStatusRows.putAll(content.statusRows)
    }

    private fun StrataColumn.buildDebug() {
        val content = DebugSettingsWindowBuilder(
            ui = ui,
            settings = settings,
            simulation = simulation,
            eventMonitor = eventMonitor,
            world = world,
            synchronizers = synchronizers
        ).build(this)
        pickingRows = content.pickingRows
        cameraRows = content.cameraRows
        cullingRows = content.cullingRows
    }

    private fun invalidateToolFlyoutLayout() {
        if (!::toolFlyoutScroll.isInitialized) return
        toolFlyoutScroll.content.invalidateHierarchy()
        toolFlyoutScroll.invalidateHierarchy()
        toolFlyoutActor.invalidateHierarchy()
        positionToolFlyout()
    }

    private fun syncControls() {
        synchronizers.sync()
    }

    private fun selectTool(mode: DebugToolMode) {
        tools.select(mode)
        syncControls()
        syncVisibility()
    }

    private fun toggleToolSettings(mode: DebugToolMode) {
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
                .padLeft(DebugWindowLayout.TOOL_RAIL_MARGIN)
                .center()
                .left()
        }
        windowLayout.add().grow()
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
        val debugVisible = settings.debugWindow.enabled && settings.debugWindow.visible &&
            ::debugPanelActor.isInitialized
        val availableWidth = ui.stage.viewport.worldWidth
            .takeIf { it > 0f }
            ?: Gdx.graphics.width.toFloat()
        statsOverlay.setTopPadding(DebugWindowLayout.OVERLAY_MARGIN)
        statsOverlay.setRightPadding(
            DebugWindowLayout.overlayRightPadding(
                debugVisible = debugVisible,
                debugWidth = DebugWindowLayout.debugWidth(availableWidth)
            )
        )
    }

    private fun syncVisibility() {
        if (toolRailState.syncActiveTool(tools.mode)) appliedFlyoutMode = null
        if (!settings.toolsWindow.enabled || !::toolFlyoutActor.isInitialized) return
        val displayedMode = toolRailState.settingsMode

        val previousMode = appliedFlyoutMode
        val previousOverlayVisibility = overlayPaintControls.isVisible

        buildControls.isVisible = displayedMode == DebugToolMode.BUILD
        deleteControls.isVisible = displayedMode == DebugToolMode.DELETE
        paintControls.isVisible = displayedMode == DebugToolMode.PAINT
        spawnControls.isVisible = displayedMode == DebugToolMode.SPAWN
        inspectControls.isVisible = displayedMode == DebugToolMode.INSPECT
        moveControls.isVisible = displayedMode == DebugToolMode.MOVE
        pathControls.isVisible = displayedMode == DebugToolMode.PATHFINDING
        cameraControls.isVisible = displayedMode == DebugToolMode.FREE_CAMERA

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
        diagnostics.sync(
            inspectorRows = if (::inspectorRows.isInitialized) inspectorRows else null,
            pickingRows = if (::pickingRows.isInitialized) pickingRows else null,
            cameraRows = if (::cameraRows.isInitialized) cameraRows else null,
            cullingRows = if (::cullingRows.isInitialized) cullingRows else null,
            toolStatusRows = toolStatusRows
        )
    }

}

