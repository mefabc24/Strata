package com.mefabc24.sandbox

import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.terrain.TerrainId

enum class TerrainType : TerrainId {
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