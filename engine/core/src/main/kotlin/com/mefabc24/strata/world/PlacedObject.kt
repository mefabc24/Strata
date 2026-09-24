package com.mefabc24.strata.world

class PlacedObject(
    val placeable: Placeable,
    val x: Int,
    val y: Int
) {
    /**
     * Returns the world positions of all tiles occupied by this object.
     */
    fun occupiedTiles(): Set<TilePosition> {
        val origin = placeable.footprint.origin

        return placeable.footprint.offsets.mapTo(mutableSetOf()) { offset ->
            TilePosition(
                x = x + offset.x - origin.x,
                y = y + offset.y - origin.y
            )
        }
    }
}