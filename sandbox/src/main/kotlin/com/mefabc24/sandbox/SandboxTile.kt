package com.mefabc24.sandbox

import com.mefabc24.strata.world.Tile

enum class TerrainType {
    GRASS,
    WATER,
    SAND
}

data class SandboxTile(
    val terrain: TerrainType,
) : Tile