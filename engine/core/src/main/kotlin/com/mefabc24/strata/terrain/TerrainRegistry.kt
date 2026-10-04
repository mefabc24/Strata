package com.mefabc24.strata.terrain

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.render.sprite.PreparedVisualDefinition
import com.mefabc24.strata.render.sprite.SpriteDefinitionBuilder
import com.mefabc24.strata.render.sprite.SpriteFrames
import com.mefabc24.strata.render.sprite.SpriteSource
import com.mefabc24.strata.render.sprite.StatefulSpriteBuilder
import com.mefabc24.strata.render.sprite.VisualDefinition
import com.mefabc24.strata.render.sprite.VisualPlayback
import com.mefabc24.strata.render.sprite.VisualStateId
import com.mefabc24.strata.render.sprite.spriteSource
import com.mefabc24.strata.world.Tile

/** Describes one terrain registration and its prepared visual data. */
class TerrainEntry internal constructor(
    val type: TerrainId,
    internal val definition: VisualDefinition<Tile, SpriteSource>,
    private val factory: (() -> Tile)?
) {
    internal val sources: List<SpriteSource> = definition.sources()

    private var preparedDefinition:
        PreparedVisualDefinition<Tile, SpriteFrames>? = null
    private var preparedSprites: List<SpriteFrames> = emptyList()

    /** The static texture or first animation frame of a single visual. */
    val texture: TextureRegion
        get() = sprite.frameAtIndex(0)

    /** Stable first registered frame for menus and debug selection previews. */
    val selectionTexture: TextureRegion
        get() = preparedSprites.firstOrNull()?.frameAtIndex(0)
            ?: error("Terrain type $type is not prepared.")

    /** Prepared frames for a single-visual terrain registration. */
    val sprite: SpriteFrames
        get() {
            check(definition is VisualDefinition.Single) {
                "Terrain type $type is stateful; resolve it with a runtime tile."
            }
            return preparedSprites.singleOrNull()
                ?: error("Terrain type $type is not prepared.")
        }

    val isPrepared: Boolean
        get() = preparedDefinition != null

    /** Whether this terrain can be created by engine tooling. */
    val isPaintable: Boolean
        get() = factory != null

    /** Creates a tile through the explicitly registered factory. */
    fun createTile(): Tile {
        val create = factory
            ?: error("Terrain type $type has no registered tile factory.")
        return checkNotNull(create()) {
            "The tile factory for $type returned null."
        }
    }

    internal fun resolve(tile: Tile, animationTime: Float): TextureRegion {
        val resolved = preparedDefinition?.resolve(tile, animationTime)
            ?: error("Terrain type $type is not prepared.")
        return resolved.visual.frameAt(resolved.stateTime)
    }

    internal fun resolveSingle(animationTime: Float): TextureRegion {
        check(definition is VisualDefinition.Single) {
            "Terrain type $type is stateful; a runtime tile is required."
        }
        return sprite.frameAt(animationTime)
    }

    internal fun prepare(
        prepared: PreparedVisualDefinition<Tile, SpriteFrames>,
        sprites: List<SpriteFrames>
    ) {
        preparedDefinition = prepared
        preparedSprites = sprites
    }

    internal fun maxSpriteHeight(tileWidth: Float): Float {
        check(isPrepared) { "Terrain type $type is not prepared." }
        return preparedSprites.maxOf { sprite ->
            (0 until sprite.frameCount).maxOf { index ->
                val texture = sprite.frameAtIndex(index)
                require(texture.regionWidth > 0) {
                    "Terrain sprite width must be positive."
                }
                tileWidth * texture.regionHeight / texture.regionWidth
            }
        }
    }
}

/** Scene-owned terrain visual registration in declaration order. */
class TerrainRegistry internal constructor(
    directory: String,
    private val queueTexture: (String) -> Unit,
    private val regionFor: (String) -> TextureRegion,
    private val queueAtlas: (String) -> Unit = {},
    private val atlasFor: (String) -> TextureAtlas = {
        error("Atlas resolver is not configured.")
    }
) {
    constructor(directory: String, assets: StrataAssets) : this(
        directory,
        assets::queueTexture,
        assets::region,
        assets::queueAtlas,
        assets::atlas
    )

    private val baseDirectory = directory.trimEnd('/')
    private val registrations = linkedMapOf<TerrainId, TerrainEntry>()
    private var registrationOpen = true

    val entries: List<TerrainEntry>
        get() = registrations.values.toList()

    /** A registration-order snapshot containing entries with tile factories. */
    val paintableEntries: List<TerrainEntry>
        get() = registrations.values.filter(TerrainEntry::isPaintable)

    fun register(
        type: TerrainId,
        sprite: String,
        factory: (() -> Tile)? = null
    ) {
        registerVisual(type, factory) { sprite(sprite) }
    }

    fun registerAtlas(
        type: TerrainId,
        atlas: String,
        region: String,
        factory: (() -> Tile)? = null
    ) {
        registerVisual(type, factory) { atlas(atlas, region) }
    }

    fun registerAnimated(
        type: TerrainId,
        frames: List<String>,
        frameDuration: Float,
        factory: (() -> Tile)? = null
    ) {
        registerVisual(type, factory) { animated(frames, frameDuration) }
    }

    fun registerAnimatedAtlas(
        type: TerrainId,
        atlas: String,
        region: String,
        frameDuration: Float,
        factory: (() -> Tile)? = null
    ) {
        registerVisual(type, factory) {
            animatedAtlas(atlas, region, frameDuration)
        }
    }

    fun registerAnimated(
        type: TerrainId,
        spriteSheet: String,
        frameWidth: Int,
        frameHeight: Int,
        frameDuration: Float,
        frameCount: Int? = null,
        factory: (() -> Tile)? = null
    ) {
        registerVisual(type, factory) {
            spriteSheet(
                spriteSheet,
                frameWidth,
                frameHeight,
                frameDuration,
                frameCount
            )
        }
    }

    /** Registers one static or animated sprite through the common source DSL. */
    fun registerVisual(
        type: TerrainId,
        factory: (() -> Tile)? = null,
        visual: SpriteDefinitionBuilder.() -> Unit
    ) {
        registerDefinition(
            type,
            VisualDefinition.Single(spriteSource(::resolvePath, visual)),
            factory
        )
    }

    /**
     * Registers game-defined terrain states resolved from each runtime tile.
     * Terrain states use synchronized playback unless a state overrides it.
     */
    fun registerStateful(
        type: TerrainId,
        stateFor: (Tile) -> VisualStateId,
        factory: (() -> Tile)? = null,
        configure: StatefulSpriteBuilder.() -> Unit
    ) {
        checkRegistrationOpen()
        val states = StatefulSpriteBuilder(
            ::resolvePath,
            VisualPlayback.SYNCHRONIZED
        ).apply(configure).build()
        registerDefinition(
            type,
            VisualDefinition.Stateful(states, stateFor),
            factory
        )
    }

    internal fun prepare() {
        registrations.values.forEach { entry ->
            if (entry.isPrepared) return@forEach
            val preparedBySource = entry.sources.associateWith { source ->
                source.prepare(regionFor, atlasFor)
            }
            entry.prepare(
                prepared = entry.definition.prepare(preparedBySource::getValue),
                sprites = entry.sources.map(preparedBySource::getValue)
            )
        }
    }

    internal fun freeze() {
        registrationOpen = false
    }

    fun maxSpriteHeight(tileWidth: Float): Float {
        require(tileWidth > 0f && tileWidth.isFinite()) {
            "Tile width must be finite and positive."
        }
        check(registrations.values.all(TerrainEntry::isPrepared)) {
            "Terrain sprites must be prepared before calculating their height."
        }
        return registrations.values.maxOfOrNull {
            it.maxSpriteHeight(tileWidth)
        } ?: Float.POSITIVE_INFINITY
    }

    operator fun get(type: TerrainId): TextureRegion = entry(type).texture

    /** Resolves a frame for a single-visual terrain registration. */
    fun frameAt(type: TerrainId, stateTime: Float): TextureRegion {
        return entry(type).resolveSingle(stateTime)
    }

    /** Resolves the active terrain state and its current frame. */
    fun frameAt(
        type: TerrainId,
        tile: Tile,
        animationTime: Float
    ): TextureRegion {
        return entry(type).resolve(tile, animationTime)
    }

    private fun registerDefinition(
        type: TerrainId,
        definition: VisualDefinition<Tile, SpriteSource>,
        factory: (() -> Tile)?
    ) {
        checkRegistrationOpen()
        require(type !in registrations) {
            "Terrain type $type is already registered."
        }
        definition.sources().forEach { it.queue(queueTexture, queueAtlas) }
        registrations[type] = TerrainEntry(type, definition, factory)
    }

    private fun entry(type: TerrainId): TerrainEntry {
        return registrations[type]
            ?: error("Terrain type $type is not registered.")
    }

    private fun resolvePath(path: String): String {
        return if (baseDirectory.isEmpty()) path else "$baseDirectory/$path"
    }

    private fun checkRegistrationOpen() {
        check(registrationOpen) {
            "Terrain registry registration is already closed."
        }
    }
}
