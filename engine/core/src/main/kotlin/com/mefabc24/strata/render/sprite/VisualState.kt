package com.mefabc24.strata.render.sprite

import java.util.IdentityHashMap

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
 * Runtime state resolution never loads assets.
 */
open class StatefulSpriteBuilder internal constructor(
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

    internal open fun build(): Map<VisualStateId, VisualStateDefinition<SpriteSource>> {
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
        val stateFor: (C) -> VisualStateId
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
                stateFor = stateFor
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
        private val stateFor: (C) -> VisualStateId
    ) : PreparedVisualDefinition<C, V> {
        private val activeStates = IdentityHashMap<C, ActiveState>()

        override fun resolve(context: C, animationTime: Float): ResolvedVisual<V> {
            validateAnimationTime(animationTime)
            val state = stateFor(context)
            val definition = states[state]
                ?: error("Resolved visual state $state is not registered.")
            val previous = activeStates[context]
            val active = if (previous?.state == state) {
                previous
            } else {
                ActiveState(state, animationTime).also {
                    activeStates[context] = it
                }
            }
            val stateTime = when (definition.playback) {
                VisualPlayback.LOCAL -> (animationTime - active.startedAt).coerceAtLeast(0f)
                VisualPlayback.SYNCHRONIZED -> animationTime
            }
            return ResolvedVisual(definition.source, stateTime, state)
        }
    }

    fun resolve(context: C, animationTime: Float): ResolvedVisual<V>
}

private data class ActiveState(
    val state: VisualStateId,
    val startedAt: Float
)

private fun validateAnimationTime(animationTime: Float) {
    require(animationTime.isFinite() && animationTime >= 0f) {
        "Animation time must be finite and non-negative."
    }
}
