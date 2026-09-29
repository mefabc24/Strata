package com.mefabc24.strata.debug

import com.mefabc24.strata.world.WorldEntity
import java.util.Collections
import java.util.IdentityHashMap

/** Identity-based debug state for selectively suspending engine movement. */
internal class DebugEntityFreezeState {
    private val frozen = Collections.newSetFromMap(
        IdentityHashMap<WorldEntity, Boolean>()
    )
    private val playback = IdentityHashMap<WorldEntity, PlaybackState>()

    fun isFrozen(entity: WorldEntity): Boolean = entity in frozen

    fun setFrozen(entity: WorldEntity, frozen: Boolean): Boolean {
        return if (frozen) this.frozen.add(entity) else this.frozen.remove(entity)
    }

    fun clear(): Int {
        val count = frozen.size
        frozen.clear()
        return count
    }

    fun retain(activeEntities: Set<WorldEntity>) {
        frozen.removeIf { entity -> activeEntities.none { it === entity } }
        playback.keys.removeIf { entity -> activeEntities.none { it === entity } }
    }

    fun isAnimationFrozen(entity: WorldEntity, freezeAnimation: Boolean): Boolean =
        freezeAnimation && isFrozen(entity)

    fun <T> resolveAnimation(
        entity: WorldEntity,
        animationTime: Float,
        freezeAnimation: Boolean,
        resolve: (Float) -> T
    ): T {
        val state = playback.getOrPut(entity, ::PlaybackState)
        if (isAnimationFrozen(entity, freezeAnimation)) {
            if (state.pausedAt == null) {
                state.pausedAt = animationTime
                state.cached = Cached(resolve(animationTime - state.pausedDuration))
            }
            @Suppress("UNCHECKED_CAST")
            return checkNotNull(state.cached).value as T
        }

        state.pausedAt?.let { pausedAt ->
            state.pausedDuration += (animationTime - pausedAt).coerceAtLeast(0f)
            state.pausedAt = null
            state.cached = null
        }
        return resolve((animationTime - state.pausedDuration).coerceAtLeast(0f))
    }

    private class PlaybackState(
        var pausedAt: Float? = null,
        var pausedDuration: Float = 0f,
        var cached: Cached? = null
    )

    private data class Cached(val value: Any?)
}
