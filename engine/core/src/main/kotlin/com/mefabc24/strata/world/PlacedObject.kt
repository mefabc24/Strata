package com.mefabc24.strata.world

class PlacedObject(
    val placeable: Placeable,
    val x: Int,
    val y: Int
) {
    /**
     * Returns the world coordinates of all tiles occupied by this object.
     */
    fun occupiedTiles(): Set<Pair<Int, Int>> {
        val origin = placeable.footprint.origin

        return placeable.footprint.offsets.mapTo(mutableSetOf()) { offset ->
            (x + offset.x - origin.x) to
                    (y + offset.y - origin.y)
        }
    }
}