package com.mefabc24.strata.render.sprite

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/**
 * Marker for a game-defined visual state identifier.
 *
 * Strata compares identifiers to select registered visuals but never
 * interprets their meaning. Games commonly implement this with an enum.
 */
interface VisualStateId

/** Determines which clock drives a visual state's animation. */
enum class VisualPlayback {
    /** Restarts at frame zero when a runtime object enters the state. */
    LOCAL,

    /** Uses the scene animation clock so runtime objects remain synchronized. */
    SYNCHRONIZED
}

internal data class VisualStateDefinition<S : Any>(
    val source: S,
    val playback: VisualPlayback
)

/**
 * Builds game-defined states whose assets are all registered during setup.
 * Runtime state resolution never loads assets and fails when a resolver
 * returns an identifier that was not registered.
 */
class StatefulSpriteBuilder internal constructor(
    private val resolvePath: (String) -> String,
    private val defaultPlayback: VisualPlayback
) {
    private val states = linkedMapOf<VisualStateId, VisualStateDefinition<SpriteSource>>()

    /** Defines the sprite used by [id]. */
    fun state(
        id: VisualStateId,
        playback: VisualPlayback = defaultPlayback,
        configure: SpriteDefinitionBuilder.() -> Unit
    ) {
        require(id !in states) { "Visual state $id is already registered." }
        states[id] = VisualStateDefinition(
            source = spriteSource(resolvePath, configure),
            playback = playback
        )
    }

    internal fun build(): Map<VisualStateId, VisualStateDefinition<SpriteSource>> {
        require(states.isNotEmpty()) {
            "A stateful visual must register at least one visual state."
        }
        return states.toMap()
    }
}

internal sealed interface VisualDefinition<C : Any, S : Any> {
    data class Single<C : Any, S : Any>(
        val source: S
    ) : VisualDefinition<C, S>

    data class Stateful<C : Any, S : Any>(
        val states: Map<VisualStateId, VisualStateDefinition<S>>,
        val stateFor: (C) -> VisualStateId,
        val playbackIdentityFor: (C) -> Any = { context -> context }
    ) : VisualDefinition<C, S>

    fun sources(): List<S> = when (this) {
        is Single -> listOf(source)
        is Stateful -> states.values.map(VisualStateDefinition<S>::source)
    }

    fun <V : Any> prepare(
        prepareSource: (S) -> V
    ): PreparedVisualDefinition<C, V> {
        return when (this) {
            is Single -> PreparedVisualDefinition.Single(prepareSource(source))
            is Stateful -> PreparedVisualDefinition.Stateful(
                states = states.mapValues { (_, definition) ->
                    VisualStateDefinition(
                        source = prepareSource(definition.source),
                        playback = definition.playback
                    )
                },
                stateFor = stateFor,
                playbackIdentityFor = playbackIdentityFor
            )
        }
    }
}

internal data class ResolvedVisual<V : Any>(
    val visual: V,
    val stateTime: Float,
    val state: VisualStateId?
)

internal sealed interface PreparedVisualDefinition<C : Any, V : Any> {
    class Single<C : Any, V : Any>(
        private val visual: V
    ) : PreparedVisualDefinition<C, V> {
        override fun resolve(context: C, animationTime: Float): ResolvedVisual<V> {
            validateAnimationTime(animationTime)
            return ResolvedVisual(visual, animationTime, null)
        }
    }

    class Stateful<C : Any, V : Any>(
        private val states: Map<VisualStateId, VisualStateDefinition<V>>,
        private val stateFor: (C) -> VisualStateId,
        private val playbackIdentityFor: (C) -> Any
    ) : PreparedVisualDefinition<C, V> {
        private val activeStates = if (
            states.values.any { it.playback == VisualPlayback.LOCAL }
        ) {
            WeakIdentityMap<ActiveState>()
        } else {
            null
        }

        internal val trackedIdentityCount: Int
            get() = activeStates?.size ?: 0

        override fun resolve(context: C, animationTime: Float): ResolvedVisual<V> {
            validateAnimationTime(animationTime)
            val state = stateFor(context)
            val definition = states[state]
                ?: error("Resolved visual state $state is not registered.")
            val active = activeStates?.let { trackedStates ->
                val identity = playbackIdentityFor(context)
                val previous = trackedStates[identity]
                if (previous?.state == state) {
                    previous
                } else {
                    ActiveState(
                        state = state,
                        localStartedAt = animationTime.takeIf {
                            definition.playback == VisualPlayback.LOCAL
                        }
                    ).also {
                        trackedStates[identity] = it
                    }
                }
            }
            val stateTime = when (definition.playback) {
                VisualPlayback.LOCAL -> {
                    checkNotNull(active) {
                        "Local visual playback requires runtime state tracking."
                    }
                    val startedAt = checkNotNull(active.localStartedAt) {
                        "Local visual playback has no start time."
                    }
                    (animationTime - startedAt).coerceAtLeast(0f)
                }
                VisualPlayback.SYNCHRONIZED -> animationTime
            }
            return ResolvedVisual(definition.source, stateTime, state)
        }
    }

    fun resolve(context: C, animationTime: Float): ResolvedVisual<V>
}

private data class ActiveState(
    val state: VisualStateId,
    val localStartedAt: Float?
)

internal class WeakIdentityMap<V : Any> {
    private val queue = ReferenceQueue<Any>()
    private val buckets = mutableMapOf<Int, MutableList<Entry<V>>>()

    internal val size: Int
        get() {
            removeCollectedKeys()
            return buckets.values.sumOf { bucket -> bucket.size }
        }

    operator fun get(key: Any): V? {
        removeCollectedKeys()
        val identityHash = System.identityHashCode(key)
        return buckets[identityHash]
            ?.firstOrNull { entry -> entry.get() === key }
            ?.value
    }

    operator fun set(key: Any, value: V) {
        removeCollectedKeys()
        val identityHash = System.identityHashCode(key)
        val bucket = buckets.getOrPut(identityHash, ::mutableListOf)
        val existing = bucket.firstOrNull { entry -> entry.get() === key }
        if (existing != null) {
            existing.value = value
        } else {
            bucket += Entry(key, queue, identityHash, value)
        }
    }

    private fun removeCollectedKeys() {
        while (true) {
            @Suppress("UNCHECKED_CAST")
            val entry = queue.poll() as Entry<V>? ?: return
            val bucket = buckets[entry.identityHash] ?: continue
            bucket.remove(entry)
            if (bucket.isEmpty()) {
                buckets.remove(entry.identityHash)
            }
        }
    }

    private class Entry<V : Any>(
        key: Any,
        queue: ReferenceQueue<Any>,
        val identityHash: Int,
        var value: V
    ) : WeakReference<Any>(key, queue)
}

private fun validateAnimationTime(animationTime: Float) {
    require(animationTime.isFinite() && animationTime >= 0f) {
        "Animation time must be finite and non-negative."
    }
}
