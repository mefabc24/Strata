package com.mefabc24.strata.world

import kotlin.math.floor

/**
 * A continuous position on the logical tile plane.
 *
 * Integer coordinates lie on tile boundaries. The center of tile `(x, y)` is
 * therefore `(x + 0.5, y + 0.5)`.
 */
data class EntityPosition(
    val x: Float,
    val y: Float
) {
    init {
        require(x.isFinite() && y.isFinite()) {
            "Entity coordinates must be finite."
        }
    }

    /** The tile containing this position. */
    val tile: TilePosition
        get() = TilePosition(
            x = floor(x).toInt(),
            y = floor(y).toInt()
        )

    companion object {
        /** Returns the logical center of [tile]. */
        fun centerOf(tile: TilePosition): EntityPosition {
            return EntityPosition(
                x = tile.x + 0.5f,
                y = tile.y + 0.5f
            )
        }
    }
}
