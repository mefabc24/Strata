package com.mefabc24.strata.debug

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.debug.tools.DebugBuildDragController
import com.mefabc24.strata.debug.tools.DebugEntitySpawner
import com.mefabc24.strata.debug.tools.DebugPathfindingTool
import com.mefabc24.strata.debug.tools.DebugTerrainPainter
import com.mefabc24.strata.debug.tools.DebugToolController
import com.mefabc24.strata.debug.ui.DebugPanel
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
    placement: PlacementController?,
    simulation: SimulationController,
    terrainFor: (Tile) -> TerrainId
) {
    private val inspector = DebugInspector(settings.worldState)
    private val pathfinding = DebugPathfindingTool(world, settings.worldState) { position ->
        settings.pathTraversal?.invoke(world, position) ?: true
    }
    private val painter = DebugTerrainPainter(world, terrain.paintableEntries)
    private val buildDrag = placement?.let(::DebugBuildDragController)
    private val tools = DebugToolController(
        painter = painter,
        placement = placement,
        buildDrag = buildDrag,
        inspector = inspector,
        pathfinding = pathfinding
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

    private fun bindings(world: World, view: IsoWorldView): List<WorldInputBinding> = listOf(
        WorldInputBinding.Entity(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            EntityPickingMode.SPRITE_ALPHA,
            { tools.mode == DebugToolMode.INSPECT }
        ) { inspector.selectFrontmost(it, null, null) },
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
        ) { x, y -> spawner.spawn(TilePosition(x, y)) != null },
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
            { tools.mode == DebugToolMode.PAINT && painter.layerId != null }
        ) { x, y -> painter.beginErase(x, y) },
        WorldInputBinding.Grid(
            WorldInputTrigger.MouseDrag(Input.Buttons.RIGHT),
            { tools.mode == DebugToolMode.PAINT && painter.layerId != null }
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
                val placed = drag.finish(TilePosition(x, y))
                if (placed.isNotEmpty()) settings.objectsPlacedCallback?.invoke(placed)
                true
            }
        },
        WorldInputBinding.Object(
            WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
            ObjectPickingMode.SPRITE_OR_FOOTPRINT,
            { tools.mode == DebugToolMode.BUILD }
        ) { world.remove(it); true },
        WorldInputBinding.Tile(
            WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            { tools.mode == DebugToolMode.PATHFINDING }
        ) { x, y -> pathfinding.click(TilePosition(x, y)) }
    )

    fun update(delta: Float) {
        if (settings.picking.enabled) {
            val screenX = com.badlogic.gdx.Gdx.input.x.toFloat()
            val screenY = com.badlogic.gdx.Gdx.input.y.toFloat()
            settings.worldState.cursorWorld = view.screenToWorld(screenX, screenY)
            val picking = view.pickingDebugSnapshot(screenX, screenY)
            settings.worldState.picking = picking
            settings.worldState.pickedObject = picking.objectResult.picked
            settings.worldState.pickedEntity = picking.entityResult.picked
        } else {
            settings.worldState.cursorWorld = null
            settings.worldState.pickedObject = null
            settings.worldState.pickedEntity = null
            settings.worldState.picking = null
        }
        panel.update(delta)
        ui.update(delta)
    }
    fun render() = ui.render()
    fun resize(width: Int, height: Int) = ui.resize(width, height)

    fun dispose() {
        try {
            tools.select(DebugToolMode.NONE)
        } finally {
            try {
                ui.dispose()
            } finally {
                skin.dispose()
            }
        }
    }
}
