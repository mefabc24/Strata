package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.mefabc24.strata.debug.*
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.debug.tools.*
import com.mefabc24.strata.iso.IsoWorldView
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
import java.util.Locale

private enum class DebugPanelTab(val label: String) { TOOLS("Tools"), DEBUG("Debug") }
private sealed interface PaintLayer {
    val id: String?
    data object Ground : PaintLayer { override val id: String? = null }
    data class Overlay(override val id: String) : PaintLayer
    val label: String get() = id?.toDisplayName() ?: "Ground"
}

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
    simulation: SimulationController,
    private val world: World,
    private val view: IsoWorldView,
    private val terrainFor: (Tile) -> TerrainId,
    private val objects: ObjectRegistry,
    private val entities: EntityRegistry
) {
    private val statsOverlay = DebugStatsOverlay(
        ui, { view.renderStats }, { settings.performance.enabled },
        { settings.worldStats.enabled }, world, placement
    )
    private val simulationOverlay = DebugSimulationOverlay(ui, simulation)
    private val synchronizers = DebugControlBindings()
    private val previewState = DebugContentPreviewState()
    private val tabs = ui.selectionGroup(DebugPanelTab.entries, DebugPanelTab.TOOLS) {
        hidePreview(); syncVisibility()
    }
    private val modes = buildList {
        add(DebugToolMode.NONE)
        add(DebugToolMode.INSPECT)
        if (tools.buildAvailable && buildEntries.isNotEmpty()) add(DebugToolMode.BUILD)
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
    private val paintLayers = listOf(PaintLayer.Ground) + world.overlayLayerIds.map(PaintLayer::Overlay)
    private val layerSelection = ui.selectionGroup(paintLayers, paintLayers.first()) {
        painter.layerId = it.id
    }

    private lateinit var panelActor: StrataPanel
    private lateinit var bodyScroll: StrataScrollPane
    private lateinit var toolsTab: StrataColumn
    private lateinit var debugTab: StrataColumn
    private lateinit var buildControls: StrataColumn
    private lateinit var paintControls: StrataColumn
    private lateinit var spawnControls: StrataColumn
    private lateinit var inspectControls: StrataColumn
    private lateinit var pathControls: StrataColumn
    private lateinit var inspectorLabel: Label
    private lateinit var pathLabel: Label
    private lateinit var pickingLabel: Label
    private lateinit var cameraLabel: Label
    private lateinit var cullingLabel: Label
    private lateinit var placementLabel: Label
    private lateinit var previewName: Label
    private lateinit var previewImage: Image
    private lateinit var previewPopover: StrataPopover
    private var previewAnchor: Actor? = null
    private var lastTab = DebugPanelTab.TOOLS
    private var lastMode = DebugToolMode.NONE

    init {
        buildSelection?.selected?.let(tools::selectBuildEntry)
        terrainSelection?.selected?.let { painter.selectedEntry = it }
        spawnSelection?.selected?.let { spawner.selectedEntry = it }
        buildUi()
        buildPreview()
        synchronizers += { modeSelection.select(tools.mode) }
        terrainSelection?.let { group ->
            synchronizers += { painter.selectedEntry?.let(group::select) }
        }
        spawnSelection?.let { group ->
            synchronizers += { spawner.selectedEntry?.let(group::select) }
        }
        synchronizers += {
            paintLayers.firstOrNull { it.id == painter.layerId }?.let(layerSelection::select)
        }
        setPanelVisible(settings.panel.visible)
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
        simulationOverlay.sync()
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
            padding = StrataInsets(top = 10f, left = 10f, bottom = 10f, right = 10f)
        ) {
            defaults().fillAvailableX()
            label("STRATA DEBUG").cell { height(28f); left() }
            row(spacing = 6f) {
                defaults().fillAvailableX().uniformX().height(40f)
                DebugPanelTab.entries.forEach { selectableButton(it.label, it, tabs) }
            }
            separator()
            bodyScroll = scrollColumn(spacing = 8f) {
                defaults().fillAvailableX()
                stack {
                    toolsTab = column(spacing = 10f) {
                        defaults().fillAvailableX(); buildTools()
                    }
                    debugTab = column(spacing = 10f) {
                        defaults().fillAvailableX(); buildDebug()
                    }
                }.cell { fillAvailableX() }
            }.cell { grow(); fill(); minHeight(0f) }
        }.cell {
            minWidth(260f)
            prefWidth(Value.percentWidth(0.34f, ui.root))
            maxWidth(460f)
            growY(); fillY(); top(); left()
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
                placementLabel = wrappingLabel("")
            }
            paintControls = column(spacing = 8f) {
                defaults().fillAvailableX()
                label("Terrain")
                responsiveGrid(130f, maximumColumns = 2) {
                    painter.entries.forEach { entry ->
                        val name = entry.type.toString().toDisplayName()
                        selectableButton(name, entry, checkNotNull(terrainSelection))
                            .previewOnHover(entry, DebugContentKind.TERRAIN, name, entry.selectionTexture)
                    }
                }.cell { fillAvailableX() }
                label("Layer")
                responsiveGrid(130f, maximumColumns = 2) {
                    paintLayers.forEach { selectableButton(it.label, it, layerSelection) }
                }.cell { fillAvailableX() }
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
                inspectorLabel = wrappingLabel("Click an entity, object, or tile")
                button("Clear selection") { inspector.clear() }.cell { height(38f) }
            }
            pathControls = column(spacing = 8f) {
                defaults().fillAvailableX()
                label("Pathfinding")
                pathLabel = wrappingLabel("Click a start tile, then a goal tile")
                button("Clear path") { pathfinding.clear() }.cell { height(38f) }
            }
        }.cell { fillAvailableX() }
    }

    private fun StrataColumn.buildDebug() {
        label("Presets")
        responsiveGrid(105f, 38f, maximumColumns = 3) {
            DebugPreset.entries.forEach { preset ->
                button(preset.name.toDisplayName()) {
                    settings.applyPreset(preset); syncControls()
                }
            }
        }.cell { fillAvailableX() }
        separator()
        simpleToggle("Performance overlay", { settings.performance.enabled }) { settings.performance.enabled = it }
        simpleToggle("World stats overlay", { settings.worldStats.enabled }) { settings.worldStats.enabled = it }
        simpleToggle("Simulation controls", { settings.simulation.enabled }) { settings.simulation.enabled = it }
        simpleToggle(
            "Render order",
            { settings.renderOrder.enabled && settings.renderOrder.showLabels }
        ) {
            settings.renderOrder.enabled = it
            if (it) settings.renderOrder.showLabels = true
        }
        simpleToggle("Placement diagnostics", { settings.placement.enabled }) { settings.placement.enabled = it }
        featureExpander("Grid", { settings.grid.enabled }, { settings.grid.enabled = it }) { buildGridSettings() }
        featureExpander("Objects", { settings.objects.enabled }, { settings.objects.enabled = it }) { buildObjectSettings() }
        featureExpander("Entities", { settings.entities.enabled }, { settings.entities.enabled = it }) { buildEntitySettings() }
        featureExpander("Picking", { settings.picking.enabled }, { settings.picking.enabled = it }) {
            toggleGrid(
                toggle("Sprite bounds", { settings.picking.showSpriteBounds }) { settings.picking.showSpriteBounds = it },
                toggle("Cursor marker", { settings.picking.showCursorHit }) { settings.picking.showCursorHit = it }
            )
            pickingLabel = wrappingLabel("")
        }
        featureExpander("Culling", { settings.culling.enabled }, { settings.culling.enabled = it }) {
            toggleGrid(
                toggle("Visible area", { settings.culling.showVisibleArea }) { settings.culling.showVisibleArea = it },
                toggle("Object bounds", { settings.culling.showObjectBounds }) { settings.culling.showObjectBounds = it },
                toggle("Entity bounds", { settings.culling.showEntityBounds }) { settings.culling.showEntityBounds = it }
            )
            cullingLabel = wrappingLabel("")
        }
        featureExpander("Pathfinding", { settings.pathfinding.enabled }, { settings.pathfinding.enabled = it }) {
            toggleGrid(
                toggle("Explored nodes", { settings.pathfinding.showExploredNodes }) { settings.pathfinding.showExploredNodes = it },
                toggle("Final path", { settings.pathfinding.showFinalPath }) { settings.pathfinding.showFinalPath = it }
            )
        }
        featureExpander("Camera", { settings.camera.enabled }, { settings.camera.enabled = it }) {
            toggleGrid(
                toggle("Visible area", { settings.camera.showVisibleArea }) { settings.camera.showVisibleArea = it },
                toggle("World bounds", { settings.camera.showWorldBounds }) { settings.camera.showWorldBounds = it },
                toggle("Clamp bounds", { settings.camera.showClampBounds }) { settings.camera.showClampBounds = it }
            )
            cameraLabel = wrappingLabel("")
        }
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
        read: () -> Boolean,
        write: (Boolean) -> Unit,
        configure: StrataColumn.() -> Unit
    ) {
        expander(
            title = title,
            expanded = false,
            spacing = 8f,
            headerContent = {
                settingToggle(read, write).cell { minWidth(64f); height(36f) }
            }
        ) {
            defaults().fillAvailableX()
            configure()
        }.cell { fillAvailableX() }
    }

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

    private fun StrataLayout.wrappingLabel(text: String): Label = label(text).apply {
        setWrap(true)
    }.cell { fillAvailableX() }

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
        if (tab != lastTab || mode != lastMode) {
            hidePreview()
            bodyScroll.scrollY = 0f
            lastTab = tab
            lastMode = mode
        }
        toolsTab.isVisible = tab == DebugPanelTab.TOOLS
        debugTab.isVisible = tab == DebugPanelTab.DEBUG
        buildControls.isVisible = mode == DebugToolMode.BUILD
        paintControls.isVisible = mode == DebugToolMode.PAINT
        spawnControls.isVisible = mode == DebugToolMode.SPAWN
        inspectControls.isVisible = mode == DebugToolMode.INSPECT
        pathControls.isVisible = mode == DebugToolMode.PATHFINDING
    }

    private fun syncDiagnostics() {
        inspectorLabel.setText(formatInspection())
        pathLabel.setText(formatPath())
        val showPlacement = settings.placement.enabled && tools.mode == DebugToolMode.BUILD
        placementLabel.isVisible = showPlacement
        placementLabel.setText(if (showPlacement) formatPlacement() else "")
        syncDiagnosticLabel(pickingLabel, settings.picking.enabled, ::formatPicking)
        syncDiagnosticLabel(cameraLabel, settings.camera.enabled, ::formatCamera)
        syncDiagnosticLabel(cullingLabel, settings.culling.enabled, ::formatCulling)
    }

    private fun syncDiagnosticLabel(
        label: Label,
        visible: Boolean,
        text: () -> String
    ) {
        label.isVisible = visible
        label.setText(if (visible) text() else "")
    }

    private fun formatPlacement(): String {
        val diagnostic = placement?.previewDiagnostics?.firstOrNull()
        return when {
            placement == null -> "Placement unavailable"
            diagnostic == null -> "Hover or drag to preview"
            diagnostic.valid -> "Placement valid"
            else -> "Invalid: ${diagnostic.reason?.name?.toDisplayName() ?: "unknown reason"}"
        }
    }

    private fun formatPicking(): String {
        val x = Gdx.input.x.toFloat()
        val y = Gdx.input.y.toFloat()
        val projected = view.screenToWorld(x, y)
        val snapshot = settings.worldState.picking
        return formatRows(
            "Screen" to "${x.toInt()}, ${y.toInt()}",
            "World" to "${projected.x.format()}, ${projected.y.format()}",
            "Grid" to display(view.pickGrid(x, y)),
            "Tile" to display(view.pickTile(x, y)),
            "Object" to display(snapshot?.objectResult?.picked?.placeable?.javaClass?.simpleName),
            "Entity" to display(snapshot?.entityResult?.picked?.entity?.javaClass?.simpleName),
            "Object alpha" to alphaText(snapshot?.objectResult?.alphaAccepted),
            "Entity alpha" to alphaText(snapshot?.entityResult?.alphaAccepted)
        )
    }

    private fun formatCamera(): String {
        val camera = view.cameraDebugSnapshot()
        return formatRows(
            "Position" to "${camera.x.format()}, ${camera.y.format()}",
            "Zoom" to camera.zoom.format(),
            "Viewport" to "${camera.viewportWidth.format()} x ${camera.viewportHeight.format()}",
            "Visible" to camera.visibleArea.toString(),
            "World" to camera.worldBounds.toString(),
            "Clamp" to camera.clampBounds.toString()
        )
    }

    private fun formatCulling(): String {
        val stats = view.renderStats
        return formatRows(
            "Objects" to "${stats.objectsDrawn} drawn, " +
                "${(stats.objectsChecked - stats.objectsDrawn).coerceAtLeast(0)} culled",
            "Entities" to "${stats.entitiesDrawn} drawn, " +
                "${(stats.entitiesChecked - stats.entitiesDrawn).coerceAtLeast(0)} culled"
        )
    }

    private fun formatInspection(): String = when (val selected = inspector.selection) {
        null -> "Click an entity, object, or tile"
        is DebugInspection.EntityTarget -> {
            val entity = selected.entity
            val visual = entities.resolve(entity, view.animationTime)
            formatRows(
                "Entity" to entity.entity::class.displayName(),
                "Position" to entity.position.toString(),
                "Tile" to entity.currentTile.toString(),
                "Direction" to entity.direction.toString(),
                "Moving" to entity.isMoving.toString(),
                "Waypoints" to entity.remainingWaypoints.size.toString(),
                "Path" to entity.remainingPath.toString(),
                "Animation" to animationText(
                    visual?.state?.toString(), visual?.stateTime, visual?.visual?.sprite
                ),
                "Bounds" to display(view.entitySpriteBounds(entity))
            )
        }
        is DebugInspection.ObjectTarget -> {
            val placed = selected.placedObject
            val visual = objects.resolve(placed, view.animationTime)
            formatRows(
                "Object" to placed.placeable::class.displayName(),
                "Origin" to "(${placed.x}, ${placed.y})",
                "Footprint" to placed.placeable.footprint.toString(),
                "Occupied" to placed.occupiedTiles().toString(),
                "Animation" to animationText(
                    visual?.state?.toString(), visual?.stateTime, visual?.visual?.sprite
                ),
                "Bounds" to display(view.objectSpriteBounds(placed))
            )
        }
        is DebugInspection.TileTarget -> {
            val position = selected.position
            val tile = world.getTile(position)
            val overlays = world.overlayLayerIds.mapNotNull { id ->
                world.getOverlayTile(id, position.x, position.y)?.let { id to terrainFor(it) }
            }
            val tileEntities = world.getEntities().filter { it.currentTile == position }
            formatRows(
                "Tile" to position.toString(),
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

    private fun formatPath(): String {
        val result = pathfinding.result
        return when {
            pathfinding.start != null -> formatRows(
                "Start" to pathfinding.start.toString(),
                "Next" to "Click a goal tile"
            )
            result == null -> "Click a start tile, then a goal tile"
            else -> formatRows(
                "Start" to result.start.toString(),
                "Goal" to result.goal.toString(),
                "Result" to if (result.success) "success" else "no path",
                "Length" to (result.path?.size ?: 0).toString(),
                "Duration" to "${(result.durationNanos / 1_000_000.0).format()} ms",
                "Explored" to result.explored.size.toString(),
                "Path" to result.path.orEmpty().toString()
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

internal fun formatRows(vararg rows: Pair<String, String>): String {
    val width = rows.maxOfOrNull { it.first.length } ?: 0
    return rows.joinToString("\n") { (key, value) -> "${key.padEnd(width)}  $value" }
}

private fun display(value: Any?): String = value?.toString() ?: "—"
private fun Float.format() = String.format(Locale.ROOT, "%.2f", this)
private fun Double.format() = String.format(Locale.ROOT, "%.2f", this)
private fun String.toDisplayName() = replace('_', ' ').replace('-', ' ')
    .lowercase().replaceFirstChar(Char::titlecase)
private fun kotlin.reflect.KClass<*>.displayName() = simpleName?.toDisplayName() ?: toString()
private fun ObjectEntry.displayName() = type.displayName()
