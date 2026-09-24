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

    /**
     * Terrain elevation for each world coordinate.
     *
     * All tiles start at elevation zero.
     */
    private val heights = Array(height) {
        IntArray(width)
    }

    /**
     * Highest terrain level currently present in the world.
     */
    var maxHeight: Int = 0
        private set

    /**
     * Changes whenever terrain elevation is modified.
     */
    var heightVersion: Long = 0L
        private set

    /**
     * Provides safe terrain elevation modifications.
     */
    val terrain = TerrainManipulator(this)

    /**
     * Additional terrain layers in rendering order.
     *
     * Null represents an empty overlay cell.
     */
    private val overlayLayers = linkedMapOf<String, Array<Array<Tile?>>>()

    /**
     * Returns overlay identifiers in their rendering order.
     */
    val overlayLayerIds: List<String>
        get() = overlayLayers.keys.toList()

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

        val elevation = getHeight(
            placedObject.x,
            placedObject.y
        ) ?: return false

        return placedObject.occupiedTiles().all { (x, y) ->
            getTile(x, y) != null &&
                    getObjectAt(x, y) == null &&
                    getHeight(x, y) == elevation
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

    /**
     * Returns the terrain height at the given position.
     *
     * Returns null when the position is outside the world.
     */
    fun getHeight(x: Int, y: Int): Int? {
        return heights.getOrNull(y)?.getOrNull(x)
    }

    /**
     * Sets the terrain height at the given position.
     *
     * Height zero represents the base terrain level.
     */
    internal fun setHeight(
        x: Int,
        y: Int,
        level: Int
    ) {
        require(x in 0 until width && y in 0 until height) {
            "Tile position ($x, $y) is outside the world."
        }

        require(level >= 0) {
            "Terrain height must not be negative."
        }

        applyHeightChanges(
            mapOf((x to y) to level)
        )
    }

    /**
     * Applies a validated terrain elevation change as one operation.
     */
    internal fun applyHeightChanges(
        changes: Map<Pair<Int, Int>, Int>
    ) {
        if (changes.isEmpty()) return

        var changed = false
        var requiresMaxHeightRefresh = false
        var highestNewLevel = maxHeight

        for ((position, level) in changes) {
            val (x, y) = position

            require(x in 0 until width && y in 0 until height)
            require(level >= 0)

            val previousLevel = heights[y][x]

            if (previousLevel == level) {
                continue
            }

            if (
                previousLevel == maxHeight &&
                level < previousLevel
            ) {
                requiresMaxHeightRefresh = true
            }

            heights[y][x] = level

            highestNewLevel = maxOf(
                highestNewLevel,
                level
            )

            changed = true
        }

        if (!changed) return

        maxHeight = if (requiresMaxHeightRefresh) {
            heights.maxOf { row ->
                row.maxOrNull() ?: 0
            }
        } else {
            highestNewLevel
        }

        heightVersion++
    }

    /**
     * Adds an empty terrain overlay layer.
     *
     * Layers are rendered in their registration order.
     */
    fun addOverlayLayer(id: String) {
        require(id.isNotBlank()) {
            "Overlay layer ID must not be blank."
        }

        require(id !in overlayLayers) {
            "Overlay layer '$id' already exists."
        }

        overlayLayers[id] = Array(height) {
            arrayOfNulls<Tile>(width)
        }
    }

    /**
     * Returns the tile on an overlay layer, or null if the cell is empty
     * or outside the world.
     */
    fun getOverlayTile(
        layerId: String,
        x: Int,
        y: Int
    ): Tile? {
        val layer = requireOverlayLayer(layerId)

        return layer.getOrNull(y)?.getOrNull(x)
    }

    /**
     * Sets or clears a tile on an overlay layer.
     *
     * Passing null clears the cell.
     */
    fun setOverlayTile(
        layerId: String,
        x: Int,
        y: Int,
        tile: Tile?
    ) {
        require(x in 0 until width && y in 0 until height) {
            "Tile position ($x, $y) is outside the world."
        }

        val layer = requireOverlayLayer(layerId)

        layer[y][x] = tile
    }

    /**
     * Returns a registered overlay layer.
     */
    private fun requireOverlayLayer(id: String): Array<Array<Tile?>> {
        return overlayLayers[id]
            ?: throw IllegalArgumentException(
                "Unknown overlay layer '$id'."
            )
    }
}