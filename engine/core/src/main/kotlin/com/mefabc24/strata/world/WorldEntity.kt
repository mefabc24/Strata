package com.mefabc24.strata.world

/**
 * Engine-owned spatial state for one game-owned [Entity] instance.
 *
 * World entities do not occupy tiles and are independent from [PlacedObject].
 */
class WorldEntity internal constructor(
    val entity: Entity,
    position: EntityPosition
) {
    /** Current continuous position on the logical tile plane. */
    var position: EntityPosition = position

    /** Intentionally teleports this entity to [position]. */
    fun teleport(position: EntityPosition) {
        this.position = position
    }

    /** Tile currently containing the entity's logical position. */
    val currentTile: TilePosition
        get() = position.tile
}
