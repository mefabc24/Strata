package com.mefabc24.strata.world

/**
 * Provides safe terrain elevation modifications for a world.
 *
 * Area modifications are atomic. If the resulting terrain would make
 * an occupied object footprint span multiple elevations, no changes
 * are applied.
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