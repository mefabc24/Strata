package com.mefabc24.sandbox

import com.mefabc24.strata.pathfinding.findPath
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity
import kotlin.math.abs
import kotlin.random.Random

/** Gives Sandbox entities independent idle-and-roam behavior. */
class SandboxRoamingController(
    private val world: World,
    private val random: Random = Random.Default,
    private val pathFor: (
        start: TilePosition,
        goal: TilePosition
    ) -> List<TilePosition>? = { start, goal ->
        world.findPath(start, goal)
    },
    private val isHeld: (WorldEntity) -> Boolean = { false }
) {
    private val entities =
        linkedMapOf<WorldEntity, RoamingBehavior>()

    fun control(entity: WorldEntity) {
        require(entity in world.getEntities()) {
            "A controlled entity must belong to the controller's world."
        }

        entities.putIfAbsent(
            entity,
            RoamingBehavior(
                idleRemaining = nextIdleDuration()
            )
        )
    }

    fun update(delta: Float) {
        require(delta.isFinite() && delta >= 0f) {
            "Roaming behavior delta must be finite and non-negative."
        }

        if (delta == 0f) return

        val activeEntities = world.getEntities()
        val iterator = entities.iterator()

        while (iterator.hasNext()) {
            val (entity, behavior) = iterator.next()

            if (entity !in activeEntities) {
                iterator.remove()
                continue
            }
            if (isHeld(entity)) continue

            when {
                entity.isMoving -> {
                    behavior.wasMoving = true
                }

                behavior.wasMoving -> {
                    behavior.wasMoving = false
                    behavior.idleRemaining = nextIdleDuration()
                }

                behavior.idleRemaining > delta -> {
                    behavior.idleRemaining -= delta
                }

                startRoaming(entity) -> {
                    behavior.idleRemaining = 0f
                    behavior.wasMoving = true
                }

                else -> {
                    behavior.idleRemaining = nextIdleDuration()
                }
            }
        }
    }

    private fun startRoaming(
        entity: WorldEntity
    ): Boolean {
        val start = entity.currentTile

        repeat(DESTINATION_ATTEMPTS) {
            val destination = TilePosition(
                x = start.x +
                        random.nextInt(
                            -MAX_DISTANCE,
                            MAX_DISTANCE + 1
                        ),
                y = start.y +
                        random.nextInt(
                            -MAX_DISTANCE,
                            MAX_DISTANCE + 1
                        )
            )

            val distance =
                abs(destination.x - start.x) +
                        abs(destination.y - start.y)

            if (distance < MIN_DISTANCE) {
                return@repeat
            }

            if (world.getTile(destination) == null) {
                return@repeat
            }

            val path =
                pathFor(start, destination)
                    ?: return@repeat

            if (path.size < 2) {
                return@repeat
            }

            entity.followPath(
                path = path,
                speed = MOVEMENT_SPEED
            )

            return true
        }

        return false
    }

    private fun nextIdleDuration(): Float {
        return MIN_IDLE_SECONDS +
                random.nextFloat() *
                (MAX_IDLE_SECONDS - MIN_IDLE_SECONDS)
    }

    private data class RoamingBehavior(
        var idleRemaining: Float,
        var wasMoving: Boolean = false
    )

    private companion object {
        const val MIN_IDLE_SECONDS = 1.5f
        const val MAX_IDLE_SECONDS = 4f
        const val MIN_DISTANCE = 3
        const val MAX_DISTANCE = 8
        const val DESTINATION_ATTEMPTS = 8
        const val MOVEMENT_SPEED = 2f
    }
}
