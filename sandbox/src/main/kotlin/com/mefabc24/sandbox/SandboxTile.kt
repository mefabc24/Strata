package com.mefabc24.sandbox

import com.mefabc24.strata.world.Tile

enum class TerrainType {
    GRASS,
    WATER,
    BUSH,
    SAND,
    STONE,
    DIRT,
    LOW_GRASS,
    ROCK,
    BUSH_ANIMATED
}

data class SandboxTile(
    val terrain: TerrainType,
) : Tile