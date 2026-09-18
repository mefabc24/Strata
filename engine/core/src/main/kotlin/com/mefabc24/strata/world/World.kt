package com.mefabc24.strata.world

class World(
    val width: Int,
    val height: Int,
    createTile: (x: Int, y: Int) -> Tile
) {
    init {
        require(width > 0 && height > 0)
    }

    private val tiles = Array(height) { y ->
        Array(width) { x -> createTile(x, y) }
    }

    private val occupiedTiles = mutableMapOf<Pair<Int, Int>, PlacedObject>()

    /**
     * Places an object if all required tiles are inside the world and unoccupied.
     */
    fun placeObject(placedObject: PlacedObject): Boolean {
        val positions = placedObject.occupiedTiles()

        val canPlace = positions.all { (x, y) ->
            getTile(x, y) != null &&
                    (x to y) !in occupiedTiles
        }

        if (!canPlace) return false

        for (position in positions) {
            occupiedTiles[position] = placedObject
        }

        return true
    }

    /**
     * Returns the object occupying the given tile, if any.
     */
    fun getObjectAt(x: Int, y: Int): PlacedObject? {
        return occupiedTiles[x to y]
    }

    /**
     * Returns all placed objects without duplicates.
     */
    fun getObjects(): Set<PlacedObject> {
        return occupiedTiles.values.toSet()
    }

    /**
     * Removes the entire object occupying the given tile.
     */
    fun removeObjectAt(x: Int, y: Int): PlacedObject? {
        val placedObject = getObjectAt(x, y) ?: return null

        for (position in placedObject.occupiedTiles()) {
            occupiedTiles.remove(position)
        }

        return placedObject
    }

    fun getTile(x: Int, y: Int): Tile? =
        tiles.getOrNull(y)?.getOrNull(x)

    fun setTile(x: Int, y: Int, tile: Tile) {
        require(x in 0 until width && y in 0 until height)
        tiles[y][x] = tile
    }
}