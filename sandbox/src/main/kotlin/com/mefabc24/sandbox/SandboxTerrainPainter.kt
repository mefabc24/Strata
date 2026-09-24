package com.mefabc24.sandbox

import com.mefabc24.strata.world.World
import kotlin.math.abs

/**
 * Paints ground and overlay tiles using continuous mouse strokes.
 */
class SandboxTerrainPainter(
    private val world: World
) {
    private enum class Stroke {
        PAINT,
        ERASE
    }

    var enabled: Boolean = false
        set(value) {
            if (field != value) {
                cancel()
                field = value
            }
        }

    /**
     * Null selects the ground layer.
     */
    var layerId: String? = null
        set(value) {
            require(value == null || value in world.overlayLayerIds) {
                "Unknown overlay layer '$value'."
            }

            if (field != value) {
                cancel()
                field = value
            }
        }

    var terrain: TerrainType = TerrainType.WATER

    /**
     * Elevation applied while painting the ground layer.
     *
     * Overlay tiles use the existing world elevation at their coordinate.
     */
    var elevation: Int = 0
        set(value) {
            require(value >= 0) {
                "Paint elevation must not be negative."
            }

            if (field != value) {
                cancel()
                field = value
            }
        }

    /** Overlay layer identifiers in world rendering order. */
    val overlayLayerIds: List<String>
        get() = world.overlayLayerIds

    /** Selects the next ground or overlay layer in world rendering order. */
    fun cycleLayer() {
        val layers = listOf<String?>(null) + overlayLayerIds
        val currentIndex = layers.indexOf(layerId)

        layerId = layers[(currentIndex + 1) % layers.size]
    }

    private var activeStroke: Stroke? = null
    private var lastTile: Pair<Int, Int>? = null

    fun beginPaint(x: Int, y: Int): Boolean {
        if (!enabled) return false

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
        if (!enabled || layerId == null) return false

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

    /**
     * Ends the current stroke without modifying the world.
     */
    fun cancel() {
        activeStroke = null
        lastTile = null
    }

    private fun paint(x: Int, y: Int): Boolean {
        val target = x to y
        val tile = SandboxTile(terrain)
        val selectedLayer = layerId

        forEachTileOnLine(lastTile, target) { tileX, tileY ->
            if (selectedLayer == null) {
                val heightChanged = world.terrain.setHeight(
                    x = tileX,
                    y = tileY,
                    level = elevation
                )

                if (heightChanged) {
                    world.terrain.setTile(
                        x = tileX,
                        y = tileY,
                        tile = tile
                    )
                }
            } else {
                world.setOverlayTile(
                    layerId = selectedLayer,
                    x = tileX,
                    y = tileY,
                    tile = tile
                )
            }
        }

        lastTile = target
        return true
    }

    private fun erase(x: Int, y: Int): Boolean {
        val selectedLayer = layerId ?: return false
        val target = x to y

        forEachTileOnLine(lastTile, target) { tileX, tileY ->
            world.setOverlayTile(
                layerId = selectedLayer,
                x = tileX,
                y = tileY,
                tile = null
            )
        }

        lastTile = target
        return true
    }

    private fun forEachTileOnLine(
        from: Pair<Int, Int>?,
        to: Pair<Int, Int>,
        action: (x: Int, y: Int) -> Unit
    ) {
        var x = from?.first ?: to.first
        var y = from?.second ?: to.second

        val dx = abs(to.first - x)
        val dy = abs(to.second - y)

        val stepX = if (x < to.first) 1 else -1
        val stepY = if (y < to.second) 1 else -1

        var error = dx - dy

        while (true) {
            // Skip the previous tile when continuing a stroke.
            if (from == null || x != from.first || y != from.second) {
                action(x, y)
            }

            if (x == to.first && y == to.second) {
                break
            }

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
