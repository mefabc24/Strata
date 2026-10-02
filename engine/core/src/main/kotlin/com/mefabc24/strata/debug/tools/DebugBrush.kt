package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.world.TilePosition
import kotlin.math.abs

/** Shared square-brush geometry clipped to finite world bounds. */
object DebugBrush {
    fun tiles(
        center: TilePosition,
        size: Int,
        worldWidth: Int,
        worldHeight: Int
    ): List<TilePosition> {
        require(size > 0 && size % 2 == 1) { "Debug brush size must be a positive odd number." }
        val radius = size / 2
        return buildList {
            for (y in center.y - radius..center.y + radius) {
                for (x in center.x - radius..center.x + radius) {
                    if (x in 0 until worldWidth && y in 0 until worldHeight) {
                        add(TilePosition(x, y))
                    }
                }
            }
        }
    }
}

/** Visits each affected tile once while interpolating between sampled cursor positions. */
class DebugBrushStroke {
    private var lastCenter: TilePosition? = null
    private val visited = mutableSetOf<TilePosition>()

    val active: Boolean get() = lastCenter != null

    fun begin(
        center: TilePosition,
        size: Int,
        worldWidth: Int,
        worldHeight: Int,
        action: (TilePosition) -> Unit
    ) {
        cancel()
        visit(center, size, worldWidth, worldHeight, action)
    }

    fun drag(
        center: TilePosition,
        size: Int,
        worldWidth: Int,
        worldHeight: Int,
        action: (TilePosition) -> Unit
    ): Boolean {
        if (!active) return false
        visit(center, size, worldWidth, worldHeight, action)
        return true
    }

    fun cancel() {
        lastCenter = null
        visited.clear()
    }

    private fun visit(
        center: TilePosition,
        size: Int,
        worldWidth: Int,
        worldHeight: Int,
        action: (TilePosition) -> Unit
    ) {
        forEachCenterOnLine(lastCenter, center) { sampledCenter ->
            DebugBrush.tiles(sampledCenter, size, worldWidth, worldHeight).forEach { tile ->
                if (visited.add(tile)) action(tile)
            }
        }
        lastCenter = center
    }
}

internal inline fun forEachCenterOnLine(
    from: TilePosition?,
    to: TilePosition,
    action: (TilePosition) -> Unit
) {
    var x = from?.x ?: to.x
    var y = from?.y ?: to.y
    val dx = abs(to.x - x)
    val dy = abs(to.y - y)
    val stepX = if (x < to.x) 1 else -1
    val stepY = if (y < to.y) 1 else -1
    var error = dx - dy
    while (true) {
        if (from == null || x != from.x || y != from.y) action(TilePosition(x, y))
        if (x == to.x && y == to.y) break
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
