package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.terrain.TerrainEntry
import com.mefabc24.strata.world.World
import kotlin.math.abs

enum class DebugPaintTarget { GROUND, OVERLAY }

/** Paints registered terrain on ground and overlay layers with continuous strokes. */
class DebugTerrainPainter(
    private val world: World,
    entries: List<TerrainEntry>
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
    private var lastTile: Pair<Int, Int>? = null

    fun beginPaint(x: Int, y: Int): Boolean {
        if (!enabled || selectedEntry == null) return false
        cancel()
        activeStroke = Stroke.PAINT
        return paint(x, y)
    }

    fun dragPaint(x: Int, y: Int): Boolean {
        if (!enabled || activeStroke != Stroke.PAINT) return false
        return paint(x, y)
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
        return erase(x, y)
    }

    fun dragErase(x: Int, y: Int): Boolean {
        if (!enabled || activeStroke != Stroke.ERASE) return false
        return erase(x, y)
    }

    fun endErase(): Boolean {
        if (activeStroke != Stroke.ERASE) return false
        cancel()
        return true
    }

    fun cancel() {
        activeStroke = null
        lastTile = null
    }

    private fun paint(x: Int, y: Int): Boolean {
        val entry = selectedEntry ?: return false
        val target = x to y
        val selectedLayer = activeOverlayLayerId
        forEachTileOnLine(lastTile, target) { tileX, tileY ->
            if (world.getTile(tileX, tileY) == null) return@forEachTileOnLine
            val tile = entry.createTile()
            if (selectedLayer == null) {
                world.terrain.setTile(tileX, tileY, tile)
            } else {
                world.setOverlayTile(selectedLayer, tileX, tileY, tile)
            }
        }
        lastTile = target
        return true
    }

    private fun erase(x: Int, y: Int): Boolean {
        val selectedLayer = activeOverlayLayerId ?: return false
        val target = x to y
        forEachTileOnLine(lastTile, target) { tileX, tileY ->
            if (world.getTile(tileX, tileY) != null) {
                world.setOverlayTile(selectedLayer, tileX, tileY, null)
            }
        }
        lastTile = target
        return true
    }

    private fun forEachTileOnLine(
        from: Pair<Int, Int>?,
        to: Pair<Int, Int>,
        action: (Int, Int) -> Unit
    ) {
        var x = from?.first ?: to.first
        var y = from?.second ?: to.second
        val dx = abs(to.first - x)
        val dy = abs(to.second - y)
        val stepX = if (x < to.first) 1 else -1
        val stepY = if (y < to.second) 1 else -1
        var error = dx - dy
        while (true) {
            if (from == null || x != from.first || y != from.second) action(x, y)
            if (x == to.first && y == to.second) break
            val doubleError = 2 * error
            if (doubleError > -dy) {
                error -= dy
                x += stepX
            }
            if (doubleError < dx) {
                error += dx
                y += stepY
            }
        }
    }
}
