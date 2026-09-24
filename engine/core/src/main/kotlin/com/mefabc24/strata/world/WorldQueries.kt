package com.mefabc24.strata.world

/**
 * Returns whether a tile position lies inside this world.
 */
fun World.contains(position: TilePosition): Boolean {
    return position.x in 0 until width &&
            position.y in 0 until height
}

/**
 * Returns the valid neighboring tile positions.
 *
 * By default only edge-connected neighbors are returned.
 */
fun World.neighbors(
    position: TilePosition,
    includeDiagonals: Boolean = false
): List<TilePosition> {
    require(contains(position)) {
        "Tile position $position is outside the world."
    }

    val offsets = if (includeDiagonals) {
        listOf(
            TileOffset(-1, -1),
            TileOffset(0, -1),
            TileOffset(1, -1),
            TileOffset(-1, 0),
            TileOffset(1, 0),
            TileOffset(-1, 1),
            TileOffset(0, 1),
            TileOffset(1, 1)
        )
    } else {
        listOf(
            TileOffset(0, -1),
            TileOffset(-1, 0),
            TileOffset(1, 0),
            TileOffset(0, 1)
        )
    }

    return offsets.mapNotNull { offset ->
        val neighbor = TilePosition(
            x = position.x + offset.x,
            y = position.y + offset.y
        )

        neighbor.takeIf(::contains)
    }
}

/**
 * Returns all valid tile positions inside the requested rectangle.
 *
 * Parts of the requested area outside the world are ignored.
 */
fun World.positionsIn(
    xRange: IntRange,
    yRange: IntRange
): List<TilePosition> {
    val world = this

    return buildList {
        for (y in yRange) {
            for (x in xRange) {
                val position = TilePosition(x, y)

                if (world.contains(position)) {
                    add(position)
                }
            }
        }
    }
}

/**
 * Returns whether an object occupies the given tile.
 */
fun World.isOccupied(
    position: TilePosition
): Boolean {
    return getObjectAt(position) != null
}

/**
 * Returns all unique objects occupying the requested rectangle.
 */
fun World.objectsIn(
    xRange: IntRange,
    yRange: IntRange
): Set<PlacedObject> {
    return positionsIn(xRange, yRange)
        .mapNotNull(::getObjectAt)
        .toSet()
}