package com.mefabc24.sandbox.registration

import com.mefabc24.sandbox.Wolf
import com.mefabc24.sandbox.WolfState
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.world.EntityDirection

internal fun EntityRegistry.registerSandboxEntities() {
    registerStateful<Wolf>(
        stateFor = { entity ->
            if (entity.isMoving) {
                WolfState.WALK
            } else {
                WolfState.IDLE
            }
        },
        configure = {
            offsetY = -24f
        }
    ) {
        state(WolfState.IDLE) {
            directionalSpriteSheet(
                path = "wolf/wolf-idle.png",
                frameWidth = 64,
                frameHeight = 64,
                frameDuration = 0.2f,
                framesPerDirection = 4,
                directionRows = DIRECTION_ROWS
            )
        }

        state(WolfState.WALK) {
            directionalSpriteSheet(
                path = "wolf/wolf-run.png",
                frameWidth = 64,
                frameHeight = 64,
                frameDuration = 0.1f,
                framesPerDirection = 8,
                directionRows = DIRECTION_ROWS
            )
        }
    }
}

private val DIRECTION_ROWS = mapOf(
    EntityDirection.SOUTH_WEST to 0,
    EntityDirection.SOUTH_EAST to 1,
    EntityDirection.NORTH_WEST to 2,
    EntityDirection.NORTH_EAST to 3
)