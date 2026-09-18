package com.mefabc24.strata.world

import java.util.Collections

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

    private val objects = linkedSetOf<PlacedObject>()

    /**
     * Changes whenever an object is placed or removed.
     */
    var objectVersion: Long = 0L
        private set

    private val objectView: Set<PlacedObject> =
        Collections.unmodifiableSet(objects)

    private val occupiedTiles = mutableMapOf<Pair<Int, Int>, PlacedObject>()

    /**
     * Checks whether an object can be placed without modifying the world.
     */
    fun canPlaceObject(placedObject: PlacedObject): Boolean {
        if (placedObject in objects) return false

        return placedObject.occupiedTiles().all { (x, y) ->
            getTile(x, y) != null && getObjectAt(x, y) == null
        }
    }

    /**
     * Places an object if its footprint is inside the world and unoccupied.
     */
    fun placeObject(placedObject: PlacedObject): Boolean {
        if (!canPlaceObject(placedObject)) return false

        for (position in placedObject.occupiedTiles()) {
            occupiedTiles[position] = placedObject
        }

        objects.add(placedObject)
        objectVersion++

        return true
    }

    /**
     * Returns the object occupying the given tile, if any.
     */
    fun getObjectAt(x: Int, y: Int): PlacedObject? {
        return occupiedTiles[x to y]
    }

    /**
     * Returns a read-only live view of all placed objects.
     */
    fun getObjects(): Set<PlacedObject> = objectView

    /**
     * Removes a specific placed object from the world.
     *
     * Returns false if the object is not currently placed.
     */
    fun removeObject(placedObject: PlacedObject): Boolean {
        if (placedObject !in objects) return false

        val positions = placedObject.occupiedTiles()

        if (positions.any { occupiedTiles[it] !== placedObject }) {
            return false
        }

        for (position in positions) {
            occupiedTiles.remove(position)
        }

        objects.remove(placedObject)
        objectVersion++

        return true
    }

    /**
     * Removes the entire object occupying the given tile.
     */
    fun removeObjectAt(x: Int, y: Int): PlacedObject? {
        val placedObject = getObjectAt(x, y) ?: return null

        return if (removeObject(placedObject)) {
            placedObject
        } else {
            null
        }
    }

    fun getTile(x: Int, y: Int): Tile? =
        tiles.getOrNull(y)?.getOrNull(x)

    fun setTile(x: Int, y: Int, tile: Tile) {
        require(x in 0 until width && y in 0 until height)
        tiles[y][x] = tile
    }
}