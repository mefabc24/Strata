package com.mefabc24.sandbox.registration
import com.mefabc24.sandbox.TerrainType
import com.mefabc24.strata.terrain.TerrainRegistry

internal fun TerrainRegistry.registerSandboxTerrain() {
    registerAtlas(
        TerrainType.GRASS,
        atlas = TERRAIN_ATLAS,
        region = "grass"
    )

    registerAtlas(
        TerrainType.BUSH,
        atlas = TERRAIN_ATLAS,
        region = "bush"
    )

    registerAtlas(
        TerrainType.LOW_GRASS,
        atlas = TERRAIN_ATLAS,
        region = "lowgrass"
    )

    registerAtlas(
        TerrainType.WATER,
        atlas = TERRAIN_ATLAS,
        region = "water4"
    )

    registerAtlas(
        TerrainType.ROCK,
        atlas = TERRAIN_ATLAS,
        region = "rock"
    )

    registerAtlas(
        TerrainType.SAND,
        atlas = TERRAIN_ATLAS,
        region = "sand"
    )

    registerAtlas(
        TerrainType.STONE,
        atlas = TERRAIN_ATLAS,
        region = "stone"
    )

    registerAtlas(
        TerrainType.DIRT,
        atlas = TERRAIN_ATLAS,
        region = "dirt"
    )

    registerAnimatedAtlas(
        TerrainType.BUSH_ANIMATED,
        atlas = TERRAIN_ATLAS,
        region = "bush-animated",
        frameDuration = 0.2f
    )
}

private const val TERRAIN_ATLAS = "atlas/tiles.atlas"