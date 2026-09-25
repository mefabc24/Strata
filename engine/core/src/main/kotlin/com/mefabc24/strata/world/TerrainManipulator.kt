package com.mefabc24.strata.world

/** Provides ground terrain modification operations for a world. */
class TerrainManipulator internal constructor(
    private val world: World
) {

    /** Replaces the ground tile at the given position. */
    fun setTile(
        x: Int,
        y: Int,
        tile: Tile
    ) {
        validatePosition(x, y)
        world.setTile(x, y, tile)
    }

    /** Replaces the ground tile at the given position. */
    fun setTile(
        position: TilePosition,
        tile: Tile
    ) {
        setTile(position.x, position.y, tile)
    }

    /** Fills a rectangular area with the same ground tile. */
    fun fill(
        xRange: IntRange,
        yRange: IntRange,
        tile: Tile
    ) {
        fill(xRange, yRange) { _, _ -> tile }
    }

    /** Fills a rectangular area using a tile factory. */
    fun fill(
        xRange: IntRange,
        yRange: IntRange,
        createTile: (x: Int, y: Int) -> Tile
    ) {
        val positions = positionsIn(xRange, yRange)

        // Create every replacement before modifying the world.
        val tiles = positions.associateWith { position ->
            createTile(position.x, position.y)
        }

        for ((position, tile) in tiles) {
            world.setTile(position.x, position.y, tile)
        }
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
                    add(TilePosition(x, y))
                }
            }
        }
    }

    private fun validatePosition(x: Int, y: Int) {
        require(x in 0 until world.width && y in 0 until world.height) {
            "Tile position ($x, $y) is outside the world."
        }
    }
}
