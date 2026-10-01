@file:Suppress("unused")

package com.mefabc24.strata.world

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Engine-owned spatial state for one game-owned [Entity] instance.
 *
 * World entities do not occupy tiles and are independent from [PlacedObject].
 */
class WorldEntity internal constructor(
    val entity: Entity,
    position: EntityPosition,
    initialDirection: EntityDirection = EntityDirection.SOUTH_EAST
) {
    private var movement: Movement? = null

    /**
     * Direction this entity is currently facing.
     *
     * Movement updates the direction automatically. When movement stops,
     * the last direction is retained.
     */
    var direction: EntityDirection = initialDirection
        private set

    /**
     * Current continuous position on the logical tile plane.
     *
     * Engine movement updates this value. Use [teleport] for an intentional
     * discontinuous relocation.
     */
    var position: EntityPosition = position
        private set

    /**
     * Immediately relocates this entity to [position] and cancels its route.
     *
     * Teleportation is an intentional discontinuity, so movement does not
     * continue toward waypoints selected for the previous position.
     */
    fun teleport(position: EntityPosition) {
        this.position = position
        cancelMovement()
    }

    /** Tile currently containing the entity's logical position. */
    val currentTile: TilePosition
        get() = position.tile

    /**
     * Changes the facing direction without moving the entity.
     *
     * Active movement may update the direction again on the next movement update.
     */
    fun face(direction: EntityDirection) {
        this.direction = direction
    }

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

    /** Remaining route tiles, returned as a snapshot. */
    val remainingPath: List<TilePosition>
        get() = movement?.let { state ->
            state.waypoints.subList(
                state.waypointIndex,
                state.waypoints.size
            ).map(EntityPosition::tile)
        }.orEmpty()

    /**
     * Replaces the current route with tile-center waypoints from [path].
     * [speed] is measured in logical tiles per second.
     * An empty path clears the active route.
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
        updateDirectionToCurrentWaypoint()
    }

    /** Stops route following at the current continuous position. */
    fun cancelMovement() {
        movement = null
    }

    internal fun updateMovement(delta: Float) {
        require(delta.isFinite() && delta >= 0f) {
            "Entity movement delta must be finite and non-negative."
        }

        if (delta == 0f) return

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

            direction = directionFor(dx, dy)

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

    private fun updateDirectionToCurrentWaypoint() {
        val state = movement ?: return
        val target = state.waypoints[state.waypointIndex]

        val dx = target.x - position.x
        val dy = target.y - position.y

        if (
            dx * dx + dy * dy <=
            POSITION_EPSILON * POSITION_EPSILON
        ) {
            return
        }

        direction = directionFor(dx, dy)
    }

    private fun directionFor(
        dx: Float,
        dy: Float
    ): EntityDirection {
        val screenX = dx - dy
        val screenDown = dx + dy
        val angle = atan2(screenDown, screenX)
        val sector = Math.floorMod(
            floor((angle + PI.toFloat() / 8f) / (PI.toFloat() / 4f)).toInt(),
            EntityDirection.entries.size
        )

        return when (sector) {
            0 -> EntityDirection.EAST
            1 -> EntityDirection.SOUTH_EAST
            2 -> EntityDirection.SOUTH
            3 -> EntityDirection.SOUTH_WEST
            4 -> EntityDirection.WEST
            5 -> EntityDirection.NORTH_WEST
            6 -> EntityDirection.NORTH
            else -> EntityDirection.NORTH_EAST
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
