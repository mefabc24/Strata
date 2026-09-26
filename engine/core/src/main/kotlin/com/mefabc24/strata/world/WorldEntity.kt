package com.mefabc24.strata.world

import kotlin.math.sqrt

/**
 * Engine-owned spatial state for one game-owned [Entity] instance.
 *
 * World entities do not occupy tiles and are independent from [PlacedObject].
 */
class WorldEntity internal constructor(
    val entity: Entity,
    position: EntityPosition
) {
    private var movement: Movement? = null

    /** Current continuous position on the logical tile plane. */
    var position: EntityPosition = position

    /** Intentionally teleports this entity to [position]. */
    fun teleport(position: EntityPosition) {
        this.position = position
    }

    /** Tile currently containing the entity's logical position. */
    val currentTile: TilePosition
        get() = position.tile

    /** Whether this entity currently has a route to follow. */
    val isMoving: Boolean
        get() = movement != null

    /** Active movement speed in logical tiles per second. */
    val movementSpeed: Float?
        get() = movement?.speed

    /** Remaining route centers, returned as a snapshot. */
    val remainingWaypoints: List<EntityPosition>
        get() = movement?.let { state ->
            state.waypoints.subList(
                state.waypointIndex,
                state.waypoints.size
            ).toList()
        }.orEmpty()

    /**
     * Replaces the current route with tile-center waypoints from [path].
     * [speed] is measured in logical tiles per second.
     */
    fun followPath(
        path: List<TilePosition>,
        speed: Float
    ) {
        require(speed.isFinite() && speed > 0f) {
            "Entity movement speed must be finite and positive."
        }

        if (path.isEmpty()) {
            movement = null
            return
        }

        movement = Movement(
            waypoints = path.map(EntityPosition::centerOf),
            speed = speed
        )
        skipReachedWaypoints()
    }

    /** Stops route following at the current continuous position. */
    fun cancelMovement() {
        movement = null
    }

    internal fun updateMovement(delta: Float) {
        require(delta.isFinite() && delta >= 0f) {
            "Entity movement delta must be finite and non-negative."
        }

        var remainingDistance = (movement?.speed ?: return) * delta

        while (true) {
            val state = movement ?: return
            val target = state.waypoints[state.waypointIndex]
            val dx = target.x - position.x
            val dy = target.y - position.y
            val distance = sqrt(dx * dx + dy * dy)

            if (distance <= POSITION_EPSILON) {
                position = target
                advanceWaypoint(state)
                continue
            }

            if (remainingDistance <= 0f) return

            if (remainingDistance >= distance) {
                position = target
                remainingDistance -= distance
                advanceWaypoint(state)
            } else {
                val fraction = remainingDistance / distance
                position = EntityPosition(
                    x = position.x + dx * fraction,
                    y = position.y + dy * fraction
                )
                return
            }
        }
    }

    private fun skipReachedWaypoints() {
        while (true) {
            val state = movement ?: return
            val target = state.waypoints[state.waypointIndex]
            val dx = target.x - position.x
            val dy = target.y - position.y

            if (dx * dx + dy * dy > POSITION_EPSILON * POSITION_EPSILON) {
                return
            }

            position = target
            advanceWaypoint(state)
        }
    }

    private fun advanceWaypoint(state: Movement) {
        state.waypointIndex++
        if (state.waypointIndex >= state.waypoints.size) {
            movement = null
        }
    }

    private data class Movement(
        val waypoints: List<EntityPosition>,
        val speed: Float,
        var waypointIndex: Int = 0
    )

    private companion object {
        const val POSITION_EPSILON = 0.00001f
    }
}
