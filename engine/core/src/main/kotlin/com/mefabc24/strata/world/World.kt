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

    fun getTile(x: Int, y: Int): Tile? =
        tiles.getOrNull(y)?.getOrNull(x)

    fun setTile(x: Int, y: Int, tile: Tile) {
        require(x in 0 until width && y in 0 until height)
        tiles[y][x] = tile
    }
}