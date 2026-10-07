package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.`object`.ObjectEntry
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition

/** Tile-selection shape for the generic Debug Build tool. */
enum class DebugBuildShape { RECTANGLE, PATH }

/** Keeps debug interactions synchronized with one explicit tool mode. */
class DebugToolController(
    private val painter: DebugTerrainPainter,
    private val placement: PlacementController?,
    private val buildDrag: DebugBuildDragController?,
    private val inspector: DebugInspector,
    private val pathfinding: DebugPathfindingTool,
    private val setFreeCamera: (Boolean) -> Unit = {},
    private val move: DebugMoveTool? = null,
    private val remover: DebugWorldRemover? = null
) {
    private var savedPlacementEnabled: Boolean? = null
    private var savedPlacementFactory: (() -> com.mefabc24.strata.world.Placeable)? = null
    private var debugPlacementFactory: (() -> com.mefabc24.strata.world.Placeable)? = null

    var mode: DebugToolMode = DebugToolMode.NONE
        private set

    val buildAvailable: Boolean get() = placement != null && buildDrag != null
    val buildDragging: Boolean get() = buildDrag?.active == true
    /** Number of distinct selected origins, including origins whose preview is hidden. */
    val buildPreviewCount: Int get() = if (buildShape == DebugBuildShape.PATH) {
        placement?.path?.positions?.distinct()?.size ?: 0
    } else buildDrag?.previewCount ?: 0

    /** Build gesture shape. Changing it cancels unfinished rectangle and path gestures. */
    var buildShape: DebugBuildShape = DebugBuildShape.RECTANGLE
        set(value) {
            if (field == value) return
            cancelBuild()
            field = value
        }

    /** Whether the Build tool has a path with a selected first point. */
    val buildPathActive: Boolean get() = placement?.path?.active == true

    /** Starts a rectangle, or starts/extends a path with a confirmed waypoint. */
    fun beginBuild(position: TilePosition): Boolean {
        if (mode != DebugToolMode.BUILD) return false
        return if (buildShape == DebugBuildShape.PATH) {
            val path = placement?.path ?: return false
            if (path.active) path.addWaypoint(position) else {
                buildDrag?.cancel()
                path.begin(position)
            }
        } else buildDrag?.begin(position) ?: false
    }

    /** Updates the active Build gesture when a pointer is dragged. */
    fun dragBuild(position: TilePosition): Boolean {
        if (mode != DebugToolMode.BUILD) return false
        return if (buildShape == DebugBuildShape.PATH) {
            placement?.path?.update(position) ?: false
        } else buildDrag?.dragTo(position) ?: false
    }

    /** Refreshes the live path while hovering, including current-world validation. */
    fun updateBuild(position: TilePosition?) {
        if (mode == DebugToolMode.BUILD && buildShape == DebugBuildShape.PATH) {
            placement?.path?.update(position)
        }
    }

    /** Finishes the active gesture using its current endpoint or an explicit [end]. */
    fun finishBuild(end: TilePosition? = null): List<PlacedObject> {
        if (mode != DebugToolMode.BUILD) return emptyList()
        return if (buildShape == DebugBuildShape.PATH) {
            placement?.path?.finish(end).orEmpty()
        } else if (end != null) buildDrag?.finish(end).orEmpty() else emptyList()
    }

    /** Cancels an unfinished path without changing Build mode or selection. */
    fun cancelBuildPath(): Boolean = placement?.path?.cancel() ?: false

    /** Cancels all unfinished Build gestures and their previews. */
    fun cancelBuild() {
        buildDrag?.cancel()
        cancelBuildPath()
    }

    /** Selects a constructible object and cancels any unfinished Build gesture. */
    fun selectBuildEntry(entry: ObjectEntry?) {
        require(entry == null || entry.isConstructible) {
            "A debug Build entry must have a registered factory."
        }
        cancelBuild()
        debugPlacementFactory = entry?.let { selected -> selected::create }
        if (mode == DebugToolMode.BUILD) {
            placement?.selectedFactory = debugPlacementFactory
        }
    }

    fun select(mode: DebugToolMode) {
        require(mode != DebugToolMode.BUILD || buildAvailable) {
            "The Build debug tool requires a configured PlacementController."
        }
        if (this.mode == mode) return
        leave(this.mode)
        this.mode = mode
        enter(mode)
    }

    fun clearSelection() {
        inspector.clear()
        pathfinding.clear()
    }

    private fun leave(mode: DebugToolMode) {
        if (mode == DebugToolMode.INSPECT) {
            inspector.setHighlightVisible(false)
        }
        when (mode) {
            DebugToolMode.BUILD -> {
                cancelBuild()
                placement?.let { controller ->
                    controller.selectedFactory = savedPlacementFactory
                    controller.enabled = savedPlacementEnabled ?: controller.enabled
                }
                savedPlacementEnabled = null
                savedPlacementFactory = null
            }
            DebugToolMode.PAINT -> painter.cancel()
            DebugToolMode.MOVE -> move?.cancel()
            DebugToolMode.DELETE -> remover?.cancel()
            DebugToolMode.PATHFINDING -> pathfinding.clear()
            DebugToolMode.FREE_CAMERA -> setFreeCamera(false)
            else -> Unit
        }
        painter.enabled = false
    }

    private fun enter(mode: DebugToolMode) {
        if (mode == DebugToolMode.INSPECT) {
            inspector.setHighlightVisible(true)
        }
        if (mode == DebugToolMode.BUILD) {
            placement?.let { controller ->
                savedPlacementEnabled = controller.enabled
                savedPlacementFactory = controller.selectedFactory
                controller.selectedFactory = debugPlacementFactory
                controller.enabled = true
            }
        }
        if (mode == DebugToolMode.FREE_CAMERA) {
            setFreeCamera(true)
        }
        painter.enabled = mode == DebugToolMode.PAINT
    }
}
