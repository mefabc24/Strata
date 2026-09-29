package com.mefabc24.strata.debug

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.debug.tools.DebugBuildDragController
import com.mefabc24.strata.debug.tools.DebugEntitySpawner
import com.mefabc24.strata.debug.tools.DebugPathfindingTool
import com.mefabc24.strata.debug.tools.DebugTerrainPainter
import com.mefabc24.strata.debug.tools.DebugToolController
import com.mefabc24.strata.debug.tools.DebugMoveTool
import com.mefabc24.strata.debug.ui.DebugPanel
import com.mefabc24.strata.debug.ui.DebugNotificationOverlay
import com.mefabc24.strata.debug.ui.DebugEventBusOverlay
import com.mefabc24.strata.iso.EntityPickingMode
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputProcessor
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.event.EventBus

/** Runtime owner for built-in debug tools, UI, and input processors. */
internal class DebugRuntime(
    private val settings: DebugSettings,
    private val ui: StrataUi,
    private val skin: Skin,
    world: World,
    private val view: IsoWorldView,
    terrain: TerrainRegistry,
    objects: ObjectRegistry,
    entities: EntityRegistry,
    private val placement: PlacementController?,
    simulation: SimulationController,
    events: EventBus,
    terrainFor: (Tile) -> TerrainId
) {
    private val eventMonitor = DebugEventMonitor(events).apply {
        enabled = settings.eventBus.enabled
        paused = settings.eventBus.paused
    }
    private val notificationOverlay = DebugNotificationOverlay(ui, settings.notifications)
    private val eventBusOverlay = DebugEventBusOverlay(ui, eventMonitor, settings.eventBus)
    private val inspector = DebugInspector(settings.worldState)
    private val pathfinding = DebugPathfindingTool(world, settings.worldState) { position ->
        settings.pathTraversal?.invoke(world, position) ?: true
    }
    private val painter = DebugTerrainPainter(world, terrain.paintableEntries)
    private val buildDrag = placement?.let(::DebugBuildDragController)
    private val move = DebugMoveTool(
        world,
        settings.worldState,
        placement?.previewStyle ?: com.mefabc24.strata.render.preview.PlacementPreviewStyle.DEFAULT
    )
    private var freeCameraToolActive = false
    private var appliedCameraRestrictionsDisabled: Boolean? = null
    private val tools = DebugToolController(
        painter = painter,
        placement = placement,
        buildDrag = buildDrag,
        inspector = inspector,
        pathfinding = pathfinding,
        setFreeCamera = { enabled ->
            freeCameraToolActive = enabled
            syncCameraRestrictions()
        },
        move = move
    )
    private val spawner = DebugEntitySpawner(
        world = world,
        entries = entities.spawnableEntries,
        isActive = { tools.mode == DebugToolMode.SPAWN },
        onSpawned = { settings.entitySpawnedCallback?.invoke(it) }
    )

    private val panel = DebugPanel(
        ui = ui,
        settings = settings,
        tools = tools,
        painter = painter,
        spawner = spawner,
        buildEntries = objects.constructibleEntries,
        placement = placement,
        inspector = inspector,
        pathfinding = pathfinding,
        simulation = simulation,
        eventMonitor = eventMonitor,
        world = world,
        view = view,
        terrainFor = terrainFor,
        objects = objects,
        entities = entities
    )

    private val toggleProcessor = object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            if (keycode != settings.panel.toggleKey) return false
            panel.setPanelVisible(!settings.panel.visible)
            return true
        }
    }

    val uiInputProcessor: InputProcessor = InputMultiplexer(
        ui.inputProcessor,
        toggleProcessor
    )

    val worldInputProcessor: InputProcessor = WorldInputProcessor(
        bindings = bindings(world, view),
        pickTile = view::pickTile,
        pickGrid = view::pickGrid,
        pickObject = view::pickObject,
        pickEntity = view::pickEntity
    )

    private fun syncCameraRestrictions() {
        val disabled = cameraRestrictionsDisabled(
            freeCameraToolActive = freeCameraToolActive,
            persistentOverride = settings.camera.disableRestrictions
        )

        if (appliedCameraRestrictionsDisabled == disabled) return

        appliedCameraRestrictionsDisabled = disabled
        view.setDebugCameraRestrictionsDisabled(disabled)
    }

    private fun bindings(world: World, view: IsoWorldView): List<WorldInputBinding> = listOf(
        WorldInputBinding.Pointer(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            { settings.picking.enabled }
        ) { screenX, screenY ->
            settings.worldState.pickingSelection.selectFromClick(
                view.pickingDebugSnapshot(screenX, screenY).picked
            )
            false
        },
        WorldInputBinding.Entity(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            EntityPickingMode.SPRITE_ALPHA,
            { tools.mode == DebugToolMode.INSPECT }
        ) { inspector.selectFrontmost(it, null, null) },
        WorldInputBinding.Pointer(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.MOVE }
        ) { screenX, screenY ->
            move.begin(
                view.pickingDebugSnapshot(screenX, screenY).picked,
                view.pickGrid(screenX, screenY)
            )
        },
        WorldInputBinding.Object(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            ObjectPickingMode.SPRITE_ALPHA,
            { tools.mode == DebugToolMode.INSPECT }
        ) { inspector.selectFrontmost(null, it, null) },
        WorldInputBinding.Tile(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.INSPECT }
        ) { x, y -> inspector.selectFrontmost(null, null, TilePosition(x, y)) },
        WorldInputBinding.Tile(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.SPAWN }
        ) { x, y ->
            val spawned = spawner.spawn(TilePosition(x, y))
            if (spawned != null) {
                settings.notify(
                    DebugActionMessages.spawned(
                        spawned.entity::class.simpleName ?: "Entity",
                        TilePosition(x, y)
                    ),
                    DebugNotificationSeverity.SUCCESS
                )
                true
            } else {
                settings.notify("Entity spawn failed", DebugNotificationSeverity.WARNING)
                false
            }
        },
        WorldInputBinding.Tile(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.PAINT }
        ) { x, y -> painter.beginPaint(x, y) },
        WorldInputBinding.Grid(
            WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.PAINT }
        ) { x, y -> painter.dragPaint(x, y) },
        WorldInputBinding.NoPicking(
            WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.PAINT }
        ) { painter.endPaint() },
        WorldInputBinding.Tile(
            WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
            { tools.mode == DebugToolMode.PAINT && painter.activeOverlayLayerId != null }
        ) { x, y -> painter.beginErase(x, y) },
        WorldInputBinding.Grid(
            WorldInputTrigger.MouseDrag(Input.Buttons.RIGHT),
            { tools.mode == DebugToolMode.PAINT && painter.activeOverlayLayerId != null }
        ) { x, y -> painter.dragErase(x, y) },
        WorldInputBinding.NoPicking(
            WorldInputTrigger.MouseUp(Input.Buttons.RIGHT),
            { tools.mode == DebugToolMode.PAINT }
        ) { painter.endErase() },
        WorldInputBinding.Tile(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.BUILD }
        ) { x, y -> buildDrag?.begin(TilePosition(x, y)) ?: false },
        WorldInputBinding.Grid(
            WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.BUILD }
        ) { x, y -> buildDrag?.dragTo(TilePosition(x, y)) ?: false },
        WorldInputBinding.Grid(
            WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.BUILD }
        ) { x, y ->
            val drag = buildDrag ?: return@Grid false
            if (!drag.active) false else {
                val diagnostic = placement?.currentDiagnostic
                val placed = drag.finish(TilePosition(x, y))
                if (placed.isNotEmpty()) {
                    settings.objectsPlacedCallback?.invoke(placed)
                    settings.notify(
                        DebugActionMessages.placedObjects(placed.size),
                        DebugNotificationSeverity.SUCCESS
                    )
                } else {
                    settings.notify(
                        DebugActionMessages.placementRejected(diagnostic?.reason),
                        DebugNotificationSeverity.WARNING
                    )
                }
                true
            }
        },
        WorldInputBinding.Grid(
            WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.MOVE }
        ) { x, y -> move.dragTo(TilePosition(x, y)) },
        WorldInputBinding.Grid(
            WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.MOVE }
        ) { x, y ->
            val outcome = move.finish(TilePosition(x, y)) ?: return@Grid false
            if (outcome.success) {
                settings.notify(
                    "${outcome.type.name.lowercase().replaceFirstChar(Char::titlecase)} moved to (${x}, ${y})",
                    DebugNotificationSeverity.SUCCESS
                )
            } else {
                settings.notify(
                    "${outcome.type.name.lowercase().replaceFirstChar(Char::titlecase)} move rejected: " +
                        moveRejectionText(outcome.rejection),
                    DebugNotificationSeverity.WARNING
                )
            }
            true
        },
        WorldInputBinding.Object(
            WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
            ObjectPickingMode.SPRITE_OR_FOOTPRINT,
            { tools.mode == DebugToolMode.BUILD }
        ) {
            val removed = world.remove(it)
            if (removed) {
                settings.notify("Object removed", DebugNotificationSeverity.SUCCESS)
            }
            removed
        },
        WorldInputBinding.Tile(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.PATHFINDING }
        ) { x, y ->
            val handled = pathfinding.click(TilePosition(x, y))
            pathfinding.result?.let { result ->
                if (result.success) {
                    settings.notify(
                        DebugActionMessages.pathFound(result.path?.size ?: 0),
                        DebugNotificationSeverity.SUCCESS
                    )
                } else {
                    settings.notify("Pathfinding failed", DebugNotificationSeverity.WARNING)
                }
            }
            handled
        }
    )

    fun update(delta: Float) {
        syncCameraRestrictions()
        eventMonitor.enabled = settings.eventBus.enabled
        eventMonitor.paused = settings.eventBus.paused
        eventBusOverlay.sync()
        settings.notifications.update(delta)
        notificationOverlay.sync()

        settings.worldState.pickingSelection.syncEnabled(settings.picking.enabled)

        if (settings.picking.enabled) {
            val screenX = Gdx.input.x.toFloat()
            val screenY = Gdx.input.y.toFloat()
            settings.worldState.cursorWorld = view.screenToWorld(screenX, screenY)
            val picking = view.pickingDebugSnapshot(screenX, screenY)
            settings.worldState.picking = picking
            settings.worldState.pickingSelection.refresh(
                view::refreshPickedTarget
            )
        } else {
            settings.worldState.cursorWorld = null
            settings.worldState.picking = null
        }
        panel.update(delta)
        ui.update(delta)
    }
    fun render() = ui.render()
    fun resize(width: Int, height: Int) {
        ui.resize(width, height)
        panel.resized()
    }

    fun dispose() {
        try {
            tools.select(DebugToolMode.NONE)
        } finally {
            try {
                ui.dispose()
            } finally {
                try {
                    eventMonitor.dispose()
                } finally {
                    skin.dispose()
                }
            }
        }
    }
}

private fun moveRejectionText(failure: com.mefabc24.strata.world.WorldPlacementFailure?): String =
    when (failure) {
        com.mefabc24.strata.world.WorldPlacementFailure.FOOTPRINT_OUTSIDE_WORLD -> "outside world"
        com.mefabc24.strata.world.WorldPlacementFailure.OCCUPIED_TILE -> "occupied tile"
        null -> "move could not be committed"
    }


internal fun cameraRestrictionsDisabled(
    freeCameraToolActive: Boolean,
    persistentOverride: Boolean
): Boolean = freeCameraToolActive || persistentOverride
