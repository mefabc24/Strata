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
            mapOf((x to y) to level)
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

        val positions = positionsIn(xRange, yRange)

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

        val current = requireNotNull(world.getHeight(x, y))

        return applyChanges(
            mapOf((x to y) to current + amount)
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

        val positions = positionsIn(xRange, yRange)

        val changes = positions.associateWith { (x, y) ->
            requireNotNull(world.getHeight(x, y)) + amount
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

        val current = requireNotNull(world.getHeight(x, y))
        val target = current - amount

        if (target < 0) {
            return false
        }

        return applyChanges(
            mapOf((x to y) to target)
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

        val positions = positionsIn(xRange, yRange)

        val changes = mutableMapOf<Pair<Int, Int>, Int>()

        for ((x, y) in positions) {
            val target =
                requireNotNull(world.getHeight(x, y)) - amount

            if (target < 0) {
                return false
            }

            changes[x to y] = target
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

        world.setTile(x, y, tile)
    }

    /**
     * Fills a rectangular area with the same ground tile.
     */
    fun fill(
        xRange: IntRange,
        yRange: IntRange,
        tile: Tile
    ) {
        fill(xRange, yRange) { _, _ ->
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
        val positions = positionsIn(xRange, yRange)

        // Create all tiles before modifying the world.
        val tiles = positions.associateWith { (x, y) ->
            createTile(x, y)
        }

        for ((position, tile) in tiles) {
            world.setTile(
                position.first,
                position.second,
                tile
            )
        }
    }

    private fun applyChanges(
        changes: Map<Pair<Int, Int>, Int>
    ): Boolean {
        val effectiveChanges = changes.filter { (position, level) ->
            world.getHeight(
                position.first,
                position.second
            ) != level
        }

        if (effectiveChanges.isEmpty()) {
            return true
        }

        val affectedObjects = effectiveChanges.keys
            .mapNotNull { (x, y) ->
                world.getObjectAt(x, y)
            }
            .toSet()

        for (placedObject in affectedObjects) {
            var footprintHeight: Int? = null

            for (position in placedObject.occupiedTiles()) {
                val height = effectiveChanges[position]
                    ?: world.getHeight(
                        position.first,
                        position.second
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
    ): List<Pair<Int, Int>> {
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
                    add(x to y)
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