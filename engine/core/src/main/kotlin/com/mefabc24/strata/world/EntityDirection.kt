package com.mefabc24.strata.world

/**
 * Facing directions as seen on screen after isometric projection.
 *
 * Directional sprite registrations may provide either the four diagonal
 * directions or all eight directions. When a screen-cardinal visual is absent,
 * rendering uses the clockwise adjacent diagonal: north-east for north,
 * south-east for east, south-west for south, and north-west for west.
 */
enum class EntityDirection {
    NORTH_EAST,
    SOUTH_EAST,
    SOUTH_WEST,
    NORTH_WEST,
    NORTH,
    SOUTH,
    EAST,
    WEST
}
