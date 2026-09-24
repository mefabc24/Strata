package com.mefabc24.strata.world

/**
 * Provides terrain modification operations for a world.
 *
 * Elevation modifications preserve object footprint consistency.
 * Area modifications are applied atomically.
 */
class TerrainManipulator internal constructor(
    private val world: World
) {

    /**
     * Sets the elevation of a single terrain tile.
     */
    fun setHeight(
        x: Int,
        y: Int,
        level: Int
    ): Boolean {
        require(level >= 0) {
            "Terrain height must not be negative."
        }

        validatePosition(x, y)

        return applyChanges(
            mapOf(
                TilePosition(x, y) to level
            )
        )
    }

    /**
     * Sets the elevation of a single terrain tile.
     */
    fun setHeight(
        position: TilePosition,
        level: Int
    ): Boolean {
        return setHeight(
            x = position.x,
            y = position.y,
            level = level
        )
    }

    /**
     * Sets the elevation of a rectangular terrain area.
     */
    fun setHeight(
        xRange: IntRange,
        yRange: IntRange,
        level: Int
    ): Boolean {
        require(level >= 0) {
            "Terrain height must not be negative."
        }

        val positions = positionsIn(
            xRange = xRange,
            yRange = yRange
        )

        return applyChanges(
            positions.associateWith { level }
        )
    }

    /**
     * Raises a single terrain tile.
     */
    fun raise(
        x: Int,
        y: Int,
        amount: Int = 1
    ): Boolean {
        require(amount > 0) {
            "Raise amount must be positive."
        }

        validatePosition(x, y)

        val current = requireNotNull(
            world.getHeight(x, y)
        )

        return applyChanges(
            mapOf(
                TilePosition(x, y) to current + amount
            )
        )
    }

    /**
     * Raises a single terrain tile.
     */
    fun raise(
        position: TilePosition,
        amount: Int = 1
    ): Boolean {
        return raise(
            x = position.x,
            y = position.y,
            amount = amount
        )
    }

    /**
     * Raises a rectangular terrain area.
     */
    fun raise(
        xRange: IntRange,
        yRange: IntRange,
        amount: Int = 1
    ): Boolean {
        require(amount > 0) {
            "Raise amount must be positive."
        }

        val positions = positionsIn(
            xRange = xRange,
            yRange = yRange
        )

        val changes = positions.associateWith { position ->
            requireNotNull(
                world.getHeight(
                    position.x,
                    position.y
                )
            ) + amount
        }

        return applyChanges(changes)
    }

    /**
     * Lowers a single terrain tile.
     */
    fun lower(
        x: Int,
        y: Int,
        amount: Int = 1
    ): Boolean {
        require(amount > 0) {
            "Lower amount must be positive."
        }

        validatePosition(x, y)

        val current = requireNotNull(
            world.getHeight(x, y)
        )

        val target = current - amount

        if (target < 0) {
            return false
        }

        return applyChanges(
            mapOf(
                TilePosition(x, y) to target
            )
        )
    }

    /**
     * Lowers a single terrain tile.
     */
    fun lower(
        position: TilePosition,
        amount: Int = 1
    ): Boolean {
        return lower(
            x = position.x,
            y = position.y,
            amount = amount
        )
    }

    /**
     * Lowers a rectangular terrain area.
     */
    fun lower(
        xRange: IntRange,
        yRange: IntRange,
        amount: Int = 1
    ): Boolean {
        require(amount > 0) {
            "Lower amount must be positive."
        }

        val positions = positionsIn(
            xRange = xRange,
            yRange = yRange
        )

        val changes = mutableMapOf<TilePosition, Int>()

        for (position in positions) {
            val target = requireNotNull(
                world.getHeight(
                    position.x,
                    position.y
                )
            ) - amount

            if (target < 0) {
                return false
            }

            changes[position] = target
        }

        return applyChanges(changes)
    }

    /**
     * Replaces the ground tile at the given position.
     */
    fun setTile(
        x: Int,
        y: Int,
        tile: Tile
    ) {
        validatePosition(x, y)

        world.setTile(
            x = x,
            y = y,
            tile = tile
        )
    }

    /**
     * Replaces the ground tile at the given position.
     */
    fun setTile(
        position: TilePosition,
        tile: Tile
    ) {
        setTile(
            x = position.x,
            y = position.y,
            tile = tile
        )
    }

    /**
     * Fills a rectangular area with the same ground tile.
     */
    fun fill(
        xRange: IntRange,
        yRange: IntRange,
        tile: Tile
    ) {
        fill(
            xRange = xRange,
            yRange = yRange
        ) { _, _ ->
            tile
        }
    }

    /**
     * Fills a rectangular area using a tile factory.
     */
    fun fill(
        xRange: IntRange,
        yRange: IntRange,
        createTile: (x: Int, y: Int) -> Tile
    ) {
        val positions = positionsIn(
            xRange = xRange,
            yRange = yRange
        )

        // Create all tiles before modifying the world.
        val tiles = positions.associateWith { position ->
            createTile(
                position.x,
                position.y
            )
        }

        for ((position, tile) in tiles) {
            world.setTile(
                x = position.x,
                y = position.y,
                tile = tile
            )
        }
    }

    private fun applyChanges(
        changes: Map<TilePosition, Int>
    ): Boolean {
        val effectiveChanges = changes.filter { (position, level) ->
            world.getHeight(
                position.x,
                position.y
            ) != level
        }

        if (effectiveChanges.isEmpty()) {
            return true
        }

        val affectedObjects = effectiveChanges.keys
            .mapNotNull { position ->
                world.getObjectAt(
                    position.x,
                    position.y
                )
            }
            .toSet()

        for (placedObject in affectedObjects) {
            var footprintHeight: Int? = null

            for (position in placedObject.occupiedTiles()) {
                val height = effectiveChanges[position]
                    ?: world.getHeight(
                        position.x,
                        position.y
                    )
                    ?: return false

                if (footprintHeight == null) {
                    footprintHeight = height
                } else if (height != footprintHeight) {
                    return false
                }
            }
        }

        world.applyHeightChanges(effectiveChanges)

        return true
    }

    private fun positionsIn(
        xRange: IntRange,
        yRange: IntRange
    ): List<TilePosition> {
        require(!xRange.isEmpty() && !yRange.isEmpty()) {
            "Terrain area must not be empty."
        }

        require(
            xRange.first >= 0 &&
                    xRange.last < world.width &&
                    yRange.first >= 0 &&
                    yRange.last < world.height
        ) {
            "Terrain area is outside the world."
        }

        return buildList {
            for (y in yRange) {
                for (x in xRange) {
                    add(
                        TilePosition(
                            x = x,
                            y = y
                        )
                    )
                }
            }
        }
    }

    private fun validatePosition(
        x: Int,
        y: Int
    ) {
        require(
            x in 0 until world.width &&
                    y in 0 until world.height
        ) {
            "Tile position ($x, $y) is outside the world."
        }
    }
}