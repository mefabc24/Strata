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

    /** Provides ground terrain modification operations. */
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

    private val entities = linkedSetOf<WorldEntity>()

    /**
     * Changes whenever an object is placed or removed.
     */
    var objectVersion: Long = 0L
        private set

    /** Changes whenever an entity is added or removed. */
    var entityVersion: Long = 0L
        private set

    private val objectView: Set<PlacedObject> =
        Collections.unmodifiableSet(objects)

    private val entityView: Set<WorldEntity> =
        Collections.unmodifiableSet(entities)

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

        return placedObject.occupiedTiles().all { (x, y) ->
            getTile(x, y) != null &&
                    getObjectAt(x, y) == null
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
     * Adds an independent entity instance without occupying a world tile.
     */
    fun addEntity(
        entity: Entity,
        position: EntityPosition
    ): WorldEntity {
        val worldEntity = WorldEntity(
            entity = entity,
            position = position
        )

        entities += worldEntity
        entityVersion++
        return worldEntity
    }

    /** Returns a read-only live view of active world entities. */
    fun getEntities(): Set<WorldEntity> = entityView

    /** Removes an active world entity. */
    fun removeEntity(entity: WorldEntity): Boolean {
        if (!entities.remove(entity)) return false

        entityVersion++
        return true
    }

    /** Advances active entity routes using the supplied frame delta. */
    internal fun updateEntities(delta: Float) {
        entities.forEach { it.updateMovement(delta) }
    }

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
