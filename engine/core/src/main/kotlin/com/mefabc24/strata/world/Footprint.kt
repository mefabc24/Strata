package com.mefabc24.strata.world

/**
 * An offset relative to a footprint's coordinate system.
 */
data class TileOffset(
    val x: Int,
    val y: Int
)

/**
 * Defines which corner of a rectangular footprint is used
 * as its placement origin.
 */
enum class FootprintOrigin {
    NORTH,
    SOUTH,
    WEST,
    EAST
}

/**
 * Defines the tiles occupied by an object and its placement origin.
 */
class Footprint private constructor(
    val offsets: Set<TileOffset>,
    val origin: TileOffset,
) {
    companion object {

        /**
         * Creates a rectangular footprint with a configurable origin.
         */
        fun rectangle(
            width: Int,
            height: Int,
            origin: FootprintOrigin = FootprintOrigin.NORTH
        ): Footprint {
            require(width > 0 && height > 0)

            val offsets = buildSet {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        add(TileOffset(x, y))
                    }
                }
            }

            val originOffset = when (origin) {
                FootprintOrigin.NORTH -> TileOffset(0, 0)
                FootprintOrigin.EAST -> TileOffset(width - 1, 0)
                FootprintOrigin.SOUTH -> TileOffset(width - 1, height - 1)
                FootprintOrigin.WEST -> TileOffset(0, height - 1)
            }

            return Footprint(offsets, originOffset)
        }

        /**
         * Creates a square footprint with a configurable origin.
         */
        fun square(
            size: Int,
            origin: FootprintOrigin = FootprintOrigin.NORTH
        ): Footprint {
            return rectangle(size, size, origin)
        }

        /**
         * Creates a custom footprint with an explicitly defined origin.
         *
         * The origin must be one of the occupied tiles.
         */
        fun custom(
            vararg offsets: TileOffset,
            origin: TileOffset = TileOffset(0, 0)
        ): Footprint {
            val occupied = offsets.toSet()

            require(occupied.isNotEmpty()) {
                "A footprint must occupy at least one tile."
            }

            require(origin in occupied) {
                "The footprint origin must be an occupied tile."
            }

            return Footprint(occupied, origin)
        }
    }
}