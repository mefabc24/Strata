package com.mefabc24.sandbox.registration
import com.mefabc24.sandbox.TerrainType
import com.mefabc24.sandbox.SandboxTile
import com.mefabc24.strata.terrain.TerrainRegistry

internal fun TerrainRegistry.registerSandboxTerrain() {
    registerAtlas(
        TerrainType.GRASS,
        atlas = TERRAIN_ATLAS,
        region = "grass",
        factory = { SandboxTile(TerrainType.GRASS) }
    )

    registerAtlas(
        TerrainType.BUSH,
        atlas = TERRAIN_ATLAS,
        region = "bush",
        factory = { SandboxTile(TerrainType.BUSH) }
    )

    registerAtlas(
        TerrainType.LOW_GRASS,
        atlas = TERRAIN_ATLAS,
        region = "lowgrass",
        factory = { SandboxTile(TerrainType.LOW_GRASS) }
    )

    registerAtlas(
        TerrainType.WATER,
        atlas = TERRAIN_ATLAS,
        region = "water4",
        factory = { SandboxTile(TerrainType.WATER) }
    )

    registerAtlas(
        TerrainType.ROCK,
        atlas = TERRAIN_ATLAS,
        region = "rock",
        factory = { SandboxTile(TerrainType.ROCK) }
    )

    registerAtlas(
        TerrainType.SAND,
        atlas = TERRAIN_ATLAS,
        region = "sand",
        factory = { SandboxTile(TerrainType.SAND) }
    )

    registerAtlas(
        TerrainType.STONE,
        atlas = TERRAIN_ATLAS,
        region = "stone",
        factory = { SandboxTile(TerrainType.STONE) }
    )

    registerAtlas(
        TerrainType.DIRT,
        atlas = TERRAIN_ATLAS,
        region = "dirt",
        factory = { SandboxTile(TerrainType.DIRT) }
    )

    registerAnimatedAtlas(
        TerrainType.BUSH_ANIMATED,
        atlas = TERRAIN_ATLAS,
        region = "bush-animated",
        frameDuration = 0.2f,
        factory = { SandboxTile(TerrainType.BUSH_ANIMATED) }
    )
}

private const val TERRAIN_ATLAS = "atlas/tiles.atlas"
