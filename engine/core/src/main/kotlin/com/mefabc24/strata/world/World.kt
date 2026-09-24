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

    private val occupiedTiles =
        mutableMapOf<TilePosition, PlacedObject>()

    /**
     * Checks whether a placeable can be placed at the given position.
     */
    fun canPlace(
        placeable: Placeable,
        x: Int,
        y: Int
    ): Boolean {
        return canPlaceObject(
            PlacedObject(
                placeable = placeable,
                x = x,
                y = y
            )
        )
    }

    /**
     * Places an object at the given position.
     *
     * Returns the placed object on success, or null otherwise.
     */
    fun place(
        placeable: Placeable,
        x: Int,
        y: Int
    ): PlacedObject? {
        val placedObject = PlacedObject(
            placeable = placeable,
            x = x,
            y = y
        )

        return if (placeObject(placedObject)) {
            placedObject
        } else {
            null
        }
    }

    /**
     * Checks whether an object can be placed without modifying the world.
     */
    internal fun canPlaceObject(
        placedObject: PlacedObject
    ): Boolean {
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
    internal fun placeObject(
        placedObject: PlacedObject
    ): Boolean {
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
    fun getObjectAt(
        x: Int,
        y: Int
    ): PlacedObject? {
        return getObjectAt(TilePosition(x, y))
    }

    fun getObjectAt(
        position: TilePosition
    ): PlacedObject? {
        return occupiedTiles[position]
    }

    fun getTile(position: TilePosition): Tile? {
        return getTile(position.x, position.y)
    }

    fun getHeight(position: TilePosition): Int? {
        return getHeight(position.x, position.y)
    }

    fun canPlace(
        placeable: Placeable,
        position: TilePosition
    ): Boolean {
        return canPlace(
            placeable = placeable,
            x = position.x,
            y = position.y
        )
    }

    fun place(
        placeable: Placeable,
        position: TilePosition
    ): PlacedObject? {
        return place(
            placeable = placeable,
            x = position.x,
            y = position.y
        )
    }

    fun removeAt(
        position: TilePosition
    ): PlacedObject? {
        return removeAt(position.x, position.y)
    }

    /**
     * Returns a read-only live view of all placed objects.
     */
    fun getObjects(): Set<PlacedObject> = objectView

    /**
     * Removes a placed object from the world.
     */
    fun remove(
        placedObject: PlacedObject
    ): Boolean {
        return removeObject(placedObject)
    }

    /**
     * Removes a specific placed object from the world.
     *
     * Returns false if the object is not currently placed.
     */
    internal fun removeObject(
        placedObject: PlacedObject
    ): Boolean {
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
     * Removes the object occupying the given tile.
     */
    fun removeAt(
        x: Int,
        y: Int
    ): PlacedObject? {
        return removeObjectAt(x, y)
    }

    /**
     * Removes the entire object occupying the given tile.
     */
    internal fun removeObjectAt(
        x: Int,
        y: Int
    ): PlacedObject? {
        val placedObject = getObjectAt(x, y) ?: return null

        return if (removeObject(placedObject)) {
            placedObject
        } else {
            null
        }
    }

    fun getTile(x: Int, y: Int): Tile? =
        tiles.getOrNull(y)?.getOrNull(x)

    internal fun setTile(
        x: Int,
        y: Int,
        tile: Tile
    ) {
        require(x in 0 until width && y in 0 until height) {
            "Tile position ($x, $y) is outside the world."
        }

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
            mapOf(
                TilePosition(x, y) to level
            )
        )
    }

    /**
     * Applies a validated terrain elevation change as one operation.
     */
    internal fun applyHeightChanges(
        changes: Map<TilePosition, Int>
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
     * Returns the tile on an overlay layer at the given position.
     */
    fun getOverlayTile(
        layerId: String,
        position: TilePosition
    ): Tile? {
        return getOverlayTile(
            layerId = layerId,
            x = position.x,
            y = position.y
        )
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
     * Sets or clears a tile on an overlay layer at the given position.
     */
    fun setOverlayTile(
        layerId: String,
        position: TilePosition,
        tile: Tile?
    ) {
        setOverlayTile(
            layerId = layerId,
            x = position.x,
            y = position.y,
            tile = tile
        )
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