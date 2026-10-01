package com.mefabc24.strata.render.entity

import com.mefabc24.strata.render.sprite.SpriteDefinitionBuilder
import com.mefabc24.strata.render.sprite.SpriteSource
import com.mefabc24.strata.render.sprite.VisualPlayback
import com.mefabc24.strata.render.sprite.VisualStateDefinition
import com.mefabc24.strata.render.sprite.VisualStateId
import com.mefabc24.strata.render.sprite.spriteSource
import com.mefabc24.strata.world.EntityDirection

internal sealed interface EntitySpriteDefinition {
    data class Single(val source: SpriteSource) : EntitySpriteDefinition

    data class Directional(
        val sources: Map<EntityDirection, SpriteSource>
    ) : EntitySpriteDefinition

    fun sources(): List<SpriteSource> = when (this) {
        is Single -> listOf(source)
        is Directional -> sources.values.toList()
    }

    fun prepare(prepareSource: (SpriteSource) -> EntityVisual): PreparedEntitySprites {
        return when (this) {
            is Single -> PreparedEntitySprites.Single(prepareSource(source))
            is Directional -> PreparedEntitySprites.Directional(
                sources.mapValues { (_, source) -> prepareSource(source) }
            )
        }
    }
}

internal sealed interface PreparedEntitySprites {
    fun resolve(direction: EntityDirection): EntityVisual

    fun representativeVisual(
        direction: EntityDirection? = null
    ): EntityVisual

    class Single(val visual: EntityVisual) : PreparedEntitySprites {
        override fun resolve(direction: EntityDirection): EntityVisual = visual
        override fun representativeVisual(
            direction: EntityDirection?
        ): EntityVisual = visual
    }

    class Directional(
        private val visuals: Map<EntityDirection, EntityVisual>
    ) : PreparedEntitySprites {
        override fun resolve(direction: EntityDirection): EntityVisual {
            return checkNotNull(visuals[direction] ?: visuals[direction.baseDirectionFallback()]) {
                "Entity direction $direction is not registered."
            }
        }

        override fun representativeVisual(
            direction: EntityDirection?
        ): EntityVisual {
            if (direction != null) {
                return checkNotNull(
                    visuals[direction] ?: visuals[direction.baseDirectionFallback()]
                ) {
                    "Representative entity direction $direction is not registered."
                }
            }

            return visuals.values.firstOrNull()
                ?: error("A directional entity visual has no registered directions.")
        }
    }

}

/**
 * Defines either one entity sprite, the four legacy diagonal directions, or all
 * eight facing directions. Missing screen-cardinal visuals use the documented
 * [EntityDirection] fallback.
 */
class EntitySpriteDefinitionBuilder internal constructor(
    private val resolvePath: (String) -> String
) : SpriteDefinitionBuilder(resolvePath) {
    private val directions = linkedMapOf<EntityDirection, SpriteSource>()
    private var directionalSheetDefined = false

    /** Defines one direction using any common static or animated sprite source. */
    fun direction(
        direction: EntityDirection,
        configure: SpriteDefinitionBuilder.() -> Unit
    ) {
        check(!directionalSheetDefined) {
            "Individual directions cannot be mixed with a directional sprite sheet."
        }
        require(direction !in directions) {
            "Entity direction $direction is already registered."
        }
        directions[direction] = spriteSource(resolvePath, configure)
    }

    /**
     * Splits one sheet into a normal [com.mefabc24.strata.render.sprite.SpriteFrames]
     * clip per direction. The game supplies the row order explicitly.
     */
    fun directionalSpriteSheet(
        path: String,
        frameWidth: Int,
        frameHeight: Int,
        frameDuration: Float,
        directionRows: Map<EntityDirection, Int>,
        framesPerDirection: Int? = null
    ) {
        check(!hasDefinition && directions.isEmpty() && !directionalSheetDefined) {
            "A sprite definition must contain exactly one sprite source."
        }
        require(path.isNotBlank()) { "Sprite sheet path must not be blank." }
        require(isSupportedDirectionSet(directionRows.keys)) {
            "Directional sprite sheet must define the four base directions or all eight directions."
        }
        require(directionRows.values.all { it >= 0 }) {
            "Directional sprite sheet row indexes must not be negative."
        }
        require(directionRows.values.toSet().size == directionRows.size) {
            "Directional sprite sheet directions must use distinct rows."
        }

        directionalSheetDefined = true
        val resolvedPath = resolvePath(path)
        directionRows.forEach { (direction, row) ->
            directions[direction] = SpriteSource.SpriteSheetRow(
                path = resolvedPath,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                frameDuration = frameDuration,
                row = row,
                framesPerRow = framesPerDirection
            )
        }
    }

    internal fun buildEntity(): EntitySpriteDefinition {
        check(!(hasDefinition && directions.isNotEmpty())) {
            "A sprite definition must contain exactly one sprite source."
        }
        if (directions.isEmpty()) {
            return EntitySpriteDefinition.Single(build())
        }
        require(isSupportedDirectionSet(directions.keys)) {
            "Directional entity visual must define the four base directions or all eight directions."
        }
        return EntitySpriteDefinition.Directional(directions.toMap())
    }
}

private val BASE_ENTITY_DIRECTIONS = setOf(
    EntityDirection.NORTH_EAST,
    EntityDirection.SOUTH_EAST,
    EntityDirection.SOUTH_WEST,
    EntityDirection.NORTH_WEST
)

private fun isSupportedDirectionSet(directions: Set<EntityDirection>): Boolean =
    directions == BASE_ENTITY_DIRECTIONS || directions == EntityDirection.entries.toSet()

private fun EntityDirection.baseDirectionFallback(): EntityDirection = when (this) {
    EntityDirection.NORTH -> EntityDirection.NORTH_EAST
    EntityDirection.EAST -> EntityDirection.SOUTH_EAST
    EntityDirection.SOUTH -> EntityDirection.SOUTH_WEST
    EntityDirection.WEST -> EntityDirection.NORTH_WEST
    else -> this
}

internal data class BuiltEntityStatefulVisual(
    val states: Map<
            VisualStateId,
            VisualStateDefinition<EntitySpriteDefinition>
            >,
    val representativeState: VisualStateId
)

/** Defines game states whose direction is selected separately at runtime. */
class EntityStatefulVisualBuilder internal constructor(
    private val resolvePath: (String) -> String,
    private val defaultPlayback: VisualPlayback
) {
    private val states =
        linkedMapOf<VisualStateId, VisualStateDefinition<EntitySpriteDefinition>>()

    private var representativeState: VisualStateId? = null

    /** Defines one game-owned state and its optional directional visuals. */
    fun state(
        id: VisualStateId,
        playback: VisualPlayback = defaultPlayback,
        configure: EntitySpriteDefinitionBuilder.() -> Unit
    ) {
        require(id !in states) { "Visual state $id is already registered." }
        states[id] = VisualStateDefinition(
            EntitySpriteDefinitionBuilder(resolvePath).apply(configure).buildEntity(),
            playback
        )
    }

    /**
     * Selects the state used for representative visuals such as spawn previews.
     *
     * When omitted, the first registered state is used.
     */
    fun representativeState(id: VisualStateId) {
        representativeState = id
    }

    internal fun build(): BuiltEntityStatefulVisual {
        require(states.isNotEmpty()) {
            "A stateful visual must register at least one visual state."
        }

        val representative = representativeState ?: states.keys.first()

        require(representative in states) {
            "Representative visual state $representative is not registered."
        }

        return BuiltEntityStatefulVisual(
            states = states.toMap(),
            representativeState = representative
        )
    }
}
