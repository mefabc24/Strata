package com.mefabc24.sandbox

import com.mefabc24.strata.world.TilePosition

/** Demo event published after the Sandbox debug Spawn tool creates an entity. */
data class SandboxDebugEntitySpawned(
    val entityType: String,
    val position: TilePosition
)

/** Demo event published after the Sandbox debug Build tool places an object. */
data class SandboxDebugObjectPlaced(
    val objectType: String,
    val position: TilePosition
)
