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

    class Single(val visual: EntityVisual) : PreparedEntitySprites {
        override fun resolve(direction: EntityDirection): EntityVisual = visual
    }

    class Directional(
        private val visuals: Map<EntityDirection, EntityVisual>
    ) : PreparedEntitySprites {
        override fun resolve(direction: EntityDirection): EntityVisual {
            return checkNotNull(visuals[direction]) {
                "Entity direction $direction is not registered."
            }
        }
    }

}

/** Defines either one entity sprite or a sprite for every facing direction. */
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
        require(directionRows.keys == EntityDirection.entries.toSet()) {
            val missing = EntityDirection.entries.filterNot(directionRows::containsKey)
            "Directional sprite sheet must define every entity direction; missing $missing."
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
        require(directions.keys == EntityDirection.entries.toSet()) {
            val missing = EntityDirection.entries.filterNot(directions::containsKey)
            "Directional entity visual must define every direction; missing $missing."
        }
        return EntitySpriteDefinition.Directional(directions.toMap())
    }
}

/** Defines game states whose direction is selected separately at runtime. */
class EntityStatefulVisualBuilder internal constructor(
    private val resolvePath: (String) -> String,
    private val defaultPlayback: VisualPlayback
) {
    private val states =
        linkedMapOf<VisualStateId, VisualStateDefinition<EntitySpriteDefinition>>()

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

    internal fun build(): Map<VisualStateId, VisualStateDefinition<EntitySpriteDefinition>> {
        require(states.isNotEmpty()) {
            "A stateful visual must register at least one visual state."
        }
        return states.toMap()
    }
}
