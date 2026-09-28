package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.debug.DebugGridExtent
import com.mefabc24.strata.debug.DebugGridRenderLayer
import com.mefabc24.strata.debug.DebugPreset
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.debug.tools.DebugEntitySpawner
import com.mefabc24.strata.debug.tools.DebugPathfindingTool
import com.mefabc24.strata.debug.tools.DebugTerrainPainter
import com.mefabc24.strata.debug.tools.DebugToolController
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.`object`.ObjectEntry
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.ui.StrataColumn
import com.mefabc24.strata.ui.StrataLayout
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.cell
import com.mefabc24.strata.ui.fillAvailableX
import com.mefabc24.strata.ui.hugX
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import java.util.Locale

private enum class DebugPanelTab(val label: String) { TOOLS("Tools"), DEBUG("Debug") }

private sealed interface PaintLayer {
    val id: String?
    data object Ground : PaintLayer { override val id: String? = null }
    data class Overlay(override val id: String) : PaintLayer
    val label: String get() = id?.toDisplayName() ?: "Ground"
}

/** Builds and synchronizes the engine-owned developer panel. */
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
    private val performance = DebugPerformanceOverlay(
        ui, { view.renderStats }, { settings.performance.enabled }
    )
    private val simulationOverlay = DebugSimulationOverlay(ui, simulation)

    private val tabs = ui.selectionGroup(
        DebugPanelTab.entries,
        DebugPanelTab.TOOLS
    ) { syncVisibility() }

    private val modes = buildList {
        add(DebugToolMode.NONE)
        add(DebugToolMode.INSPECT)
        if (tools.buildAvailable && buildEntries.isNotEmpty()) add(DebugToolMode.BUILD)
        if (painter.entries.isNotEmpty()) add(DebugToolMode.PAINT)
        if (spawner.entries.isNotEmpty()) add(DebugToolMode.SPAWN)
        add(DebugToolMode.PATHFINDING)
    }
    private val modeSelection = ui.selectionGroup(modes, DebugToolMode.NONE) {
        tools.select(it)
        syncVisibility()
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
    private val paintLayers = listOf(PaintLayer.Ground) +
        world.overlayLayerIds.map(PaintLayer::Overlay)
    private val layerSelection = ui.selectionGroup(paintLayers, paintLayers.first()) {
        painter.layerId = it.id
    }

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
    private lateinit var worldStatsLabel: Label
    private lateinit var placementLabel: Label

    init {
        buildSelection?.selected?.let(tools::selectBuildEntry)
        terrainSelection?.selected?.let { painter.selectedEntry = it }
        spawnSelection?.selected?.let { spawner.selectedEntry = it }
        buildUi()
        setPanelVisible(settings.panel.visible)
        syncVisibility()
    }

    fun setPanelVisible(visible: Boolean) {
        settings.panel.visible = visible
        ui.root.isVisible = visible
    }

    fun update(delta: Float) {
        performance.update(delta)
        simulationOverlay.setVisible(settings.simulation.enabled)
        simulationOverlay.sync()
        syncVisibility()
        syncDiagnostics()
    }

    private fun buildUi() {
        ui.root.pad(16f)
        ui.panel(spacing = 0f) {
            defaults().fillAvailableX()
            expander("STRATA DEBUG", expanded = false, spacing = 6f) {
                defaults().fillAvailableX()
                row(spacing = 4f) {
                    defaults().fillAvailableX().uniformX().height(34f)
                    DebugPanelTab.entries.forEach { tab ->
                        selectableButton(tab.label, tab, tabs)
                    }
                }
                separator()
                stack {
                    toolsTab = column(spacing = 6f) {
                        defaults().fillAvailableX()
                        buildTools()
                    }
                    debugTab = column(spacing = 6f) {
                        defaults().fillAvailableX()
                        buildDebug()
                    }
                }.cell { fillAvailableX() }
            }.cell { fillAvailableX() }
        }.cell { minWidth(310f); hugX(); top(); left() }
    }

    private fun StrataColumn.buildTools() {
        label("Mode")
        grid(columns = 3, spacing = 4f, alignment = Align.center) {
            defaults().fillAvailableX().uniformX().height(32f)
            modes.forEach { mode ->
                selectableButton(mode.displayName, mode, modeSelection)
            }
        }
        if (!tools.buildAvailable || buildEntries.isEmpty()) {
            label(
                if (!tools.buildAvailable) {
                    "Build unavailable: no PlacementController"
                } else {
                    "Build unavailable: no constructible objects"
                }
            )
        }
        stack {
            buildControls = column(spacing = 4f) {
                defaults().fillAvailableX()
                label("Build object")
                buildEntries.forEach { entry ->
                    selectableButton(entry.displayName(), entry, checkNotNull(buildSelection))
                }
                placementLabel = label("")
            }
            paintControls = column(spacing = 4f) {
                defaults().fillAvailableX()
                label("Terrain")
                painter.entries.forEach { entry ->
                    selectableButton(entry.type.toString().toDisplayName(), entry, checkNotNull(terrainSelection))
                }
                label("Layer")
                paintLayers.forEach { layer ->
                    selectableButton(layer.label, layer, layerSelection)
                }
            }
            spawnControls = column(spacing = 4f) {
                defaults().fillAvailableX()
                label("Spawn entity")
                spawner.entries.forEach { entry ->
                    selectableButton(entry.type.displayName(), entry, checkNotNull(spawnSelection))
                }
            }
            inspectControls = column(spacing = 4f) {
                defaults().fillAvailableX()
                label("Inspector")
                inspectorLabel = wrappingLabel("Click an entity, object, or tile")
                button("Clear selection") { inspector.clear() }
            }
            pathControls = column(spacing = 4f) {
                defaults().fillAvailableX()
                label("Pathfinding")
                pathLabel = wrappingLabel("Click a start tile, then a goal tile")
                button("Clear path") { pathfinding.clear() }
            }
        }.cell { fillAvailableX() }
    }

    private fun StrataColumn.buildDebug() {
        expander("Presets", expanded = true) {
            grid(columns = 2, spacing = 4f) {
                defaults().fillAvailableX().uniformX().height(30f)
                DebugPreset.entries.forEach { preset ->
                    button(preset.name.toDisplayName()) { settings.applyPreset(preset) }
                }
            }
        }
        expander("Performance", expanded = false) {
            toggleButton("Enabled", settings.performance.enabled) {
                settings.performance.enabled = it
            }
        }
        expander("Simulation", expanded = false) {
            toggleButton("Show controls", settings.simulation.enabled) {
                settings.simulation.enabled = it
            }
        }
        expander("Grid", expanded = false) { buildGridSettings() }
        expander("Objects", expanded = false) { buildObjectSettings() }
        expander("Entities", expanded = false) { buildEntitySettings() }
        expander("Picking", expanded = false) {
            toggleButton("Enabled", settings.picking.enabled) { settings.picking.enabled = it }
            toggleButton("Picked sprite bounds", settings.picking.showSpriteBounds) {
                settings.picking.showSpriteBounds = it
            }
            toggleButton("Cursor hit marker", settings.picking.showCursorHit) {
                settings.picking.showCursorHit = it
            }
            pickingLabel = wrappingLabel("")
        }
        expander("Render Order", expanded = false) {
            toggleButton("Enabled", settings.renderOrder.enabled) { settings.renderOrder.enabled = it }
            toggleButton("Labels", settings.renderOrder.showLabels) { settings.renderOrder.showLabels = it }
        }
        expander("Culling", expanded = false) {
            toggleButton("Enabled", settings.culling.enabled) { settings.culling.enabled = it }
            toggleButton("Visible area", settings.culling.showVisibleArea) { settings.culling.showVisibleArea = it }
            toggleButton("Object bounds", settings.culling.showObjectBounds) { settings.culling.showObjectBounds = it }
            toggleButton("Entity bounds", settings.culling.showEntityBounds) { settings.culling.showEntityBounds = it }
            cullingLabel = wrappingLabel("")
        }
        expander("Pathfinding", expanded = false) {
            toggleButton("Diagnostics enabled", settings.pathfinding.enabled) {
                settings.pathfinding.enabled = it
            }
            toggleButton("Explored nodes", settings.pathfinding.showExploredNodes) {
                settings.pathfinding.showExploredNodes = it
            }
            toggleButton("Final path", settings.pathfinding.showFinalPath) {
                settings.pathfinding.showFinalPath = it
            }
        }
        expander("Placement", expanded = false) {
            toggleButton("Diagnostics enabled", settings.placement.enabled) {
                settings.placement.enabled = it
            }
            label("Build previews use the same checks as final placement.")
        }
        expander("Camera", expanded = false) {
            toggleButton("Enabled", settings.camera.enabled) { settings.camera.enabled = it }
            toggleButton("Visible area", settings.camera.showVisibleArea) { settings.camera.showVisibleArea = it }
            toggleButton("World bounds", settings.camera.showWorldBounds) { settings.camera.showWorldBounds = it }
            toggleButton("Clamp bounds", settings.camera.showClampBounds) { settings.camera.showClampBounds = it }
            cameraLabel = wrappingLabel("")
        }
        expander("World Stats", expanded = false) {
            toggleButton("Enabled", settings.worldStats.enabled) { settings.worldStats.enabled = it }
            worldStatsLabel = wrappingLabel("")
        }
    }

    private fun StrataColumn.buildGridSettings() {
        toggleButton("Enabled", settings.grid.enabled) { settings.grid.enabled = it }
        val layers = ui.selectionGroup(DebugGridRenderLayer.entries, settings.grid.renderLayer) {
            settings.grid.renderLayer = it
        }
        row(spacing = 4f) {
            defaults().fillAvailableX().uniformX()
            selectableButton("Below objects", DebugGridRenderLayer.BELOW_OBJECTS, layers)
            selectableButton("Above objects", DebugGridRenderLayer.ABOVE_OBJECTS, layers)
        }
        val extents = ui.selectionGroup(DebugGridExtent.entries, settings.grid.extent) {
            settings.grid.extent = it
        }
        row(spacing = 4f) {
            defaults().fillAvailableX().uniformX()
            selectableButton("World", DebugGridExtent.WORLD, extents)
            selectableButton("Visible", DebugGridExtent.VISIBLE, extents)
        }
        numericControl("Line width", settings.grid.lineWidth, 0.25f, 0.25f..8f) {
            settings.grid.lineWidth = it
        }
        numericControl("Grid alpha", settings.grid.color.a, 0.05f, 0f..1f) { alpha ->
            settings.grid.color = settings.grid.color.apply { a = alpha }
        }
        numericControl("Hover alpha", settings.grid.hoverColor.a, 0.05f, 0f..1f) { alpha ->
            settings.grid.hoverColor = settings.grid.hoverColor.apply { a = alpha }
        }
        toggleButton("Background", settings.grid.backgroundColor != null) {
            settings.grid.backgroundColor = if (it) Color(1f, 1f, 1f, 0.2f) else null
        }
        toggleButton("Hover background", settings.grid.hoverBackgroundColor != null) {
            settings.grid.hoverBackgroundColor = if (it) Color(1f, 0f, 0f, 0.5f) else null
        }
        numericControl("Background alpha", settings.grid.backgroundColor?.a ?: 0.2f, 0.05f, 0f..1f) { alpha ->
            settings.grid.backgroundColor?.let { settings.grid.backgroundColor = it.apply { a = alpha } }
        }
        numericControl("Hover background alpha", settings.grid.hoverBackgroundColor?.a ?: 0.5f, 0.05f, 0f..1f) { alpha ->
            settings.grid.hoverBackgroundColor?.let { settings.grid.hoverBackgroundColor = it.apply { a = alpha } }
        }
    }

    private fun StrataColumn.buildObjectSettings() {
        toggleButton("Enabled", settings.objects.enabled) { settings.objects.enabled = it }
        toggleButton("Occupied tiles", settings.objects.showOccupiedTiles) { settings.objects.showOccupiedTiles = it }
        toggleButton("Origin tile", settings.objects.showOriginTile) { settings.objects.showOriginTile = it }
        toggleButton("Sprite bounds", settings.objects.showSpriteBounds) { settings.objects.showSpriteBounds = it }
        toggleButton("Tile fill", settings.objects.occupiedTileFillColor != null) {
            settings.objects.occupiedTileFillColor = if (it) Color(0.2f, 0.65f, 1f, 0.18f) else null
        }
        numericControl("Line width", settings.objects.lineWidth, 0.25f, 0.25f..8f) { settings.objects.lineWidth = it }
        numericControl("Fill alpha", settings.objects.occupiedTileFillColor?.a ?: 0.18f, 0.05f, 0f..1f) { alpha ->
            settings.objects.occupiedTileFillColor?.let { settings.objects.occupiedTileFillColor = it.apply { a = alpha } }
        }
    }

    private fun StrataColumn.buildEntitySettings() {
        toggleButton("Enabled", settings.entities.enabled) { settings.entities.enabled = it }
        toggleButton("Current tile", settings.entities.showCurrentTile) { settings.entities.showCurrentTile = it }
        toggleButton("Exact position", settings.entities.showPosition) { settings.entities.showPosition = it }
        toggleButton("Path", settings.entities.showPath) { settings.entities.showPath = it }
        toggleButton("Direction", settings.entities.showDirection) { settings.entities.showDirection = it }
        toggleButton("Sprite bounds", settings.entities.showSpriteBounds) { settings.entities.showSpriteBounds = it }
        toggleButton("Tile fill", settings.entities.currentTileFillColor != null) {
            settings.entities.currentTileFillColor = if (it) Color(0.3f, 1f, 0.3f, 0.16f) else null
        }
        numericControl("Line width", settings.entities.lineWidth, 0.25f, 0.25f..8f) { settings.entities.lineWidth = it }
        numericControl("Fill alpha", settings.entities.currentTileFillColor?.a ?: 0.16f, 0.05f, 0f..1f) { alpha ->
            settings.entities.currentTileFillColor?.let { settings.entities.currentTileFillColor = it.apply { a = alpha } }
        }
    }

    private fun StrataLayout.numericControl(
        label: String,
        initial: Float,
        step: Float,
        range: ClosedFloatingPointRange<Float>,
        changed: (Float) -> Unit
    ) {
        var value = initial.coerceIn(range.start, range.endInclusive)
        lateinit var valueLabel: Label
        row(spacing = 4f) {
            label(label).cell { growX(); left() }
            button("-") {
                value = (value - step).coerceIn(range.start, range.endInclusive)
                changed(value); valueLabel.setText(value.format())
            }
            valueLabel = label(value.format())
            button("+") {
                value = (value + step).coerceIn(range.start, range.endInclusive)
                changed(value); valueLabel.setText(value.format())
            }
        }
    }

    private fun StrataLayout.wrappingLabel(text: String): Label = label(text).apply {
        setWrap(true)
    }.cell { fillAvailableX() }

    private fun syncVisibility() {
        if (!::toolsTab.isInitialized) return
        val toolsVisible = tabs.selected == DebugPanelTab.TOOLS
        toolsTab.isVisible = toolsVisible
        debugTab.isVisible = !toolsVisible
        buildControls.isVisible = tools.mode == DebugToolMode.BUILD
        paintControls.isVisible = tools.mode == DebugToolMode.PAINT
        spawnControls.isVisible = tools.mode == DebugToolMode.SPAWN
        inspectControls.isVisible = tools.mode == DebugToolMode.INSPECT
        pathControls.isVisible = tools.mode == DebugToolMode.PATHFINDING
    }

    private fun syncDiagnostics() {
        if (::inspectorLabel.isInitialized) inspectorLabel.setText(formatInspection())
        if (::pathLabel.isInitialized) pathLabel.setText(formatPath())
        if (::placementLabel.isInitialized) {
            val diagnostic = placement?.previewDiagnostics?.firstOrNull()
            placementLabel.setText(
                when {
                    placement == null -> "Placement unavailable"
                    diagnostic == null -> "Hover or drag to preview"
                    diagnostic.valid -> "Placement valid"
                    else -> "Invalid: ${diagnostic.reason?.name?.toDisplayName()}"
                }
            )
        }
        if (::pickingLabel.isInitialized && settings.picking.enabled) {
            val x = Gdx.input.x.toFloat()
            val y = Gdx.input.y.toFloat()
            val projected = view.screenToWorld(x, y)
            pickingLabel.setText(
                "Screen: ${x.toInt()}, ${y.toInt()}\n" +
                    "World: ${projected.x.format()}, ${projected.y.format()}\n" +
                    "Grid: ${view.pickGrid(x, y)}\n" +
                    "Tile: ${view.pickTile(x, y)}\n" +
                    "Object: ${view.pickObject(x, y)?.placeable?.javaClass?.simpleName}\n" +
                    "Entity: ${view.pickEntity(x, y)?.entity?.javaClass?.simpleName}\n" +
                    "Object alpha: ${alphaText(settings.worldState.picking?.objectResult?.alphaAccepted)}\n" +
                    "Entity alpha: ${alphaText(settings.worldState.picking?.entityResult?.alphaAccepted)}"
            )
        }
        if (::cameraLabel.isInitialized && settings.camera.enabled) {
            val camera = view.cameraDebugSnapshot()
            cameraLabel.setText(
                "Position: ${camera.x.format()}, ${camera.y.format()}\n" +
                    "Zoom: ${camera.zoom.format()}\n" +
                    "Viewport: ${camera.viewportWidth.format()} x ${camera.viewportHeight.format()}\n" +
                    "Visible: ${camera.visibleArea}\nWorld: ${camera.worldBounds}\nClamp: ${camera.clampBounds}"
            )
        }
        if (::cullingLabel.isInitialized && settings.culling.enabled) {
            val stats = view.renderStats
            cullingLabel.setText(
                "Objects: ${stats.objectsDrawn} drawn, " +
                    "${(stats.objectsChecked - stats.objectsDrawn).coerceAtLeast(0)} culled\n" +
                    "Entities: ${stats.entitiesDrawn} drawn, " +
                    "${(stats.entitiesChecked - stats.entitiesDrawn).coerceAtLeast(0)} culled"
            )
        }
        if (::worldStatsLabel.isInitialized && settings.worldStats.enabled) {
            val moving = world.getEntities().count { it.isMoving }
            val paths = world.getEntities().count { it.remainingPath.isNotEmpty() }
            val stats = view.renderStats
            worldStatsLabel.setText(
                "World: ${world.width} x ${world.height}\n" +
                    "Ground tiles: ${world.groundTileCount}\n" +
                    "Overlay tiles: ${world.overlayTileCount}\n" +
                    "Overlay layers: ${world.overlayLayerIds.size}\n" +
                    "Objects: ${world.placedObjectCount} (${stats.objectsDrawn} drawn)\n" +
                    "Entities: ${world.entityCount} (${stats.entitiesDrawn} drawn)\n" +
                    "Moving: $moving, active paths: $paths\n" +
                    "Placement previews: ${placement?.previews?.size ?: 0}"
            )
        }
    }

    private fun formatInspection(): String = when (val selected = inspector.selection) {
        null -> "Click an entity, object, or tile"
        is DebugInspection.EntityTarget -> {
            val entity = selected.entity
            val visual = entities.resolve(entity, view.animationTime)
            val sprite = visual?.visual?.sprite
            "Entity: ${entity.entity::class.displayName()}\n" +
                "Position: ${entity.position}\nTile: ${entity.currentTile}\n" +
                "Direction: ${entity.direction}\nMoving: ${entity.isMoving}\n" +
                "Waypoints: ${entity.remainingWaypoints.size}\nPath: ${entity.remainingPath}\n" +
                animationText(visual?.state?.toString(), visual?.stateTime, sprite) +
                "\nBounds: ${view.entitySpriteBounds(entity)}"
        }
        is DebugInspection.ObjectTarget -> {
            val placed = selected.placedObject
            val visual = objects.resolve(placed, view.animationTime)
            val sprite = visual?.visual?.sprite
            "Object: ${placed.placeable::class.displayName()}\n" +
                "Origin: (${placed.x}, ${placed.y})\n" +
                "Footprint: ${placed.placeable.footprint}\n" +
                "Occupied: ${placed.occupiedTiles()}\n" +
                animationText(visual?.state?.toString(), visual?.stateTime, sprite) +
                "\nBounds: ${view.objectSpriteBounds(placed)}"
        }
        is DebugInspection.TileTarget -> {
            val position = selected.position
            val tile = world.getTile(position)
            val overlays = world.overlayLayerIds.mapNotNull { id ->
                world.getOverlayTile(id, position.x, position.y)?.let { id to terrainFor(it) }
            }
            val tileEntities = world.getEntities().filter { it.currentTile == position }
            "Tile: $position\nTerrain: ${tile?.let(terrainFor)}\n" +
                "Overlays: $overlays\nObject: ${world.getObjectAt(position)?.placeable?.javaClass?.simpleName}\n" +
                "Entities: ${tileEntities.map { it.entity::class.simpleName }}"
        }
    }

    private fun formatPath(): String {
        val result = pathfinding.result
        return when {
            pathfinding.start != null -> "Start: ${pathfinding.start}\nClick a goal tile"
            result == null -> "Click a start tile, then a goal tile"
            else -> "Start: ${result.start}\nGoal: ${result.goal}\n" +
                "Result: ${if (result.success) "success" else "no path"}\n" +
                "Length: ${result.path?.size ?: 0}\n" +
                "Duration: ${result.durationNanos / 1_000_000.0} ms\n" +
                "Explored: ${result.explored.size}\nPath: ${result.path.orEmpty()}"
        }
    }

    private fun animationText(
        state: String?,
        stateTime: Float?,
        sprite: com.mefabc24.strata.render.sprite.SpriteFrames?
    ): String {
        if (sprite == null || stateTime == null) return "Visual: unavailable"
        return "State: ${state ?: "default"}\nState time: ${stateTime.format()}\n" +
            "Frame: ${sprite.frameIndexAt(stateTime) + 1}/${sprite.frameCount}\n" +
            "Frame duration: ${sprite.frameDuration?.format() ?: "static"}"
    }

    private fun alphaText(value: Boolean?): String = when (value) {
        true -> "accepted"
        false -> "rejected"
        null -> "not available"
    }
}

private fun Float.format() = String.format(Locale.ROOT, "%.2f", this)
private fun String.toDisplayName() = replace('_', ' ').replace('-', ' ')
    .lowercase().replaceFirstChar(Char::titlecase)
private fun kotlin.reflect.KClass<*>.displayName() = simpleName?.toDisplayName() ?: toString()
private fun ObjectEntry.displayName() = type.displayName()
