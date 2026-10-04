package com.mefabc24.strata.world

/**
 * An inclusive rectangular area of grid positions in row-major order.
 */
class TileArea private constructor(
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int
) : Iterable<TilePosition> {

    val positions: List<TilePosition> = buildList {
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                add(TilePosition(x, y))
            }
        }
    }

    override fun iterator(): Iterator<TilePosition> = positions.iterator()

    companion object {

        /** Creates the inclusive rectangle spanning two grid positions. */
        fun between(
            start: TilePosition,
            end: TilePosition
        ): TileArea {
            return TileArea(
                minX = minOf(start.x, end.x),
                minY = minOf(start.y, end.y),
                maxX = maxOf(start.x, end.x),
                maxY = maxOf(start.y, end.y)
            )
        }
    }
}
