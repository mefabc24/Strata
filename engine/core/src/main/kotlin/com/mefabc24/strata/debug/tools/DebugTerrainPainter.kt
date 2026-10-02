package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.terrain.TerrainEntry
import com.mefabc24.strata.debug.DebugPaintToolSettings
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

enum class DebugPaintTarget { GROUND, OVERLAY }

/** Paints registered terrain on ground and overlay layers with continuous strokes. */
class DebugTerrainPainter(
    private val world: World,
    entries: List<TerrainEntry>,
    private val settings: DebugPaintToolSettings = DebugPaintToolSettings()
) {
    private enum class Stroke { PAINT, ERASE }

    val entries: List<TerrainEntry> = entries.toList().also { values ->
        require(values.all(TerrainEntry::isPaintable)) {
            "Debug terrain entries must have registered tile factories."
        }
    }

    var enabled: Boolean = false
        set(value) {
            if (field != value) cancel()
            field = value
        }

    var selectedEntry: TerrainEntry? = this.entries.firstOrNull()
        set(value) {
            require(value == null || value in entries) {
                "Selected terrain is not in the paintable registry entries."
            }
            if (field !== value) cancel()
            field = value
        }

    val overlayLayerIds: List<String> get() = world.overlayLayerIds

    var target: DebugPaintTarget = DebugPaintTarget.GROUND
        set(value) {
            require(value != DebugPaintTarget.OVERLAY || overlayLayerIds.isNotEmpty()) {
                "Overlay painting requires a registered overlay layer."
            }
            if (field != value) cancel()
            field = value
        }

    var selectedOverlayLayerId: String? = overlayLayerIds.firstOrNull()
        set(value) {
            require(value == null || value in overlayLayerIds) {
                "Unknown overlay layer '$value'."
            }
            if (field != value) cancel()
            field = value
        }

    val activeOverlayLayerId: String?
        get() = selectedOverlayLayerId.takeIf { target == DebugPaintTarget.OVERLAY }

    private var activeStroke: Stroke? = null
    private val brushStroke = DebugBrushStroke()

    fun beginPaint(x: Int, y: Int): Boolean {
        if (!enabled || selectedEntry == null) return false
        cancel()
        activeStroke = Stroke.PAINT
        paintBegin(TilePosition(x, y))
        return true
    }

    fun dragPaint(x: Int, y: Int): Boolean {
        if (!enabled || activeStroke != Stroke.PAINT) return false
        return paintDrag(TilePosition(x, y))
    }

    fun endPaint(): Boolean {
        if (activeStroke != Stroke.PAINT) return false
        cancel()
        return true
    }

    fun beginErase(x: Int, y: Int): Boolean {
        if (!enabled || activeOverlayLayerId == null) return false
        cancel()
        activeStroke = Stroke.ERASE
        eraseBegin(TilePosition(x, y))
        return true
    }

    fun dragErase(x: Int, y: Int): Boolean {
        if (!enabled || activeStroke != Stroke.ERASE) return false
        return eraseDrag(TilePosition(x, y))
    }

    fun endErase(): Boolean {
        if (activeStroke != Stroke.ERASE) return false
        cancel()
        return true
    }

    fun cancel() {
        activeStroke = null
        brushStroke.cancel()
    }

    private fun paintBegin(center: TilePosition) {
        val entry = selectedEntry ?: return
        val selectedLayer = activeOverlayLayerId
        brushStroke.begin(center, settings.brushSize, world.width, world.height) { position ->
            val tile = entry.createTile()
            if (selectedLayer == null) {
                world.terrain.setTile(position.x, position.y, tile)
            } else {
                world.setOverlayTile(selectedLayer, position, tile)
            }
        }
    }

    private fun paintDrag(center: TilePosition): Boolean {
        val entry = selectedEntry ?: return false
        val selectedLayer = activeOverlayLayerId
        return brushStroke.drag(center, settings.brushSize, world.width, world.height) { position ->
            val tile = entry.createTile()
            if (selectedLayer == null) world.terrain.setTile(position.x, position.y, tile)
            else world.setOverlayTile(selectedLayer, position, tile)
        }
    }

    private fun eraseBegin(center: TilePosition) {
        val selectedLayer = activeOverlayLayerId ?: return
        brushStroke.begin(center, settings.brushSize, world.width, world.height) { position ->
            world.setOverlayTile(selectedLayer, position, null)
        }
    }

    private fun eraseDrag(center: TilePosition): Boolean {
        val selectedLayer = activeOverlayLayerId ?: return false
        return brushStroke.drag(center, settings.brushSize, world.width, world.height) { position ->
            world.setOverlayTile(selectedLayer, position, null)
        }
    }
}
