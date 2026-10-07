package com.mefabc24.strata.placement

import com.mefabc24.strata.world.TilePosition
import kotlin.math.abs

/**
 * Selects an ordered, connected chain of logical tiles, independently of objects
 * and world bounds. Results must include both endpoints and have no consecutive
 * duplicates. Callers copy results; implementations retain ownership of them.
 * A resolver may apply routing rules, but selection itself does not validate placement.
 */
fun interface TilePathResolver {
    /** Resolves a segment from [start] to [end], including both endpoints. */
    fun resolve(start: TilePosition, end: TilePosition): List<TilePosition>
}

/**
 * Rasterizes a straight logical grid line using eight-connected Bresenham steps.
 * This is direct selection, not obstacle-avoiding pathfinding: bounds and occupancy
 * do not affect the result. Reversing endpoints reverses exactly the same tiles.
 * Each call returns a fresh read-only list with no repeated tiles.
 */
object DirectTilePathResolver : TilePathResolver {
    override fun resolve(start: TilePosition, end: TilePosition): List<TilePosition> {
        val reversed = start.x > end.x || (start.x == end.x && start.y > end.y)
        val from = if (reversed) end else start
        val to = if (reversed) start else end
        var x = from.x.toLong()
        var y = from.y.toLong()
        val dx = abs(to.x.toLong() - x)
        val dy = abs(to.y.toLong() - y)
        require(maxOf(dx, dy) < Int.MAX_VALUE) { "Path is too large for a JVM List." }
        val stepX = if (x < to.x) 1L else -1L
        val stepY = if (y < to.y) 1L else -1L
        var error = dx - dy
        val tiles = buildList {
            while (true) {
                add(TilePosition(x.toInt(), y.toInt()))
                if (x == to.x.toLong() && y == to.y.toLong()) break
                val twiceError = error * 2
                if (twiceError > -dy) {
                    error -= dy
                    x += stepX
                }
                if (twiceError < dx) {
                    error += dx
                    y += stepY
                }
            }
        }
        return if (reversed) tiles.asReversed().toList() else tiles
    }
}
