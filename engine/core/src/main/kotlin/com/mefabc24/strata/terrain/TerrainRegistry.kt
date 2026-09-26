package com.mefabc24.strata.terrain

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.render.sprite.SpriteFrames
import com.mefabc24.strata.render.sprite.SpriteSource

/**
 * Describes one terrain registration.
 *
 * The sprite is owned by the registry's asset manager. Accessing prepared
 * visual data requires the registry to have been prepared.
 */
class TerrainEntry<T : Enum<T>> internal constructor(
    val type: T,
    internal val source: SpriteSource
) {
    val spritePath: String = source.assetPaths.first()

    private var preparedSprite: SpriteFrames? = null

    /** The static texture or first animation frame. */
    val texture: TextureRegion
        get() = sprite.frameAtIndex(0)

    /** Prepared static or animated sprite frames. */
    val sprite: SpriteFrames
        get() = preparedSprite
            ?: error("Terrain type $type is not prepared.")

    val isPrepared: Boolean
        get() = preparedSprite != null

    fun frameAt(stateTime: Float): TextureRegion {
        return sprite.frameAt(stateTime)
    }

    internal fun prepare(sprite: SpriteFrames) {
        preparedSprite = sprite
    }
}

/**
 * Maps terrain types to their sprites in registration order.
 *
 * Registration is available only during scene setup. Runtime reads remain
 * available after the owning scene closes registration and prepares entries.
 */
class TerrainRegistry<T : Enum<T>> internal constructor(
    directory: String,
    private val queueTexture: (String) -> Unit,
    private val regionFor: (String) -> TextureRegion,
    private val queueAtlas: (String) -> Unit = {},
    private val atlasFor: (String) -> TextureAtlas = {
        error("Atlas resolver is not configured.")
    }
) {
    constructor(
        directory: String,
        assets: StrataAssets
    ) : this(
        directory = directory,
        queueTexture = assets::queueTexture,
        regionFor = assets::region,
        queueAtlas = assets::queueAtlas,
        atlasFor = assets::atlas
    )

    private val baseDirectory = directory.trimEnd('/')
    private val registrations = linkedMapOf<T, TerrainEntry<T>>()
    private var registrationOpen = true

    /** A snapshot of registered terrain entries in registration order. */
    val entries: List<TerrainEntry<T>>
        get() = registrations.values.toList()

    /** Registers a static terrain sprite. */
    fun register(
        type: T,
        sprite: String = "${type.name.lowercase()}.png"
    ) {
        checkRegistrationOpen()
        require(sprite.isNotBlank()) {
            "Sprite path must not be blank."
        }

        registerSource(
            type = type,
            source = SpriteSource.Static(resolvePath(sprite))
        )
    }

    /** Registers a looping terrain animation from ordered image files. */
    fun registerAnimated(
        type: T,
        frames: List<String>,
        frameDuration: Float
    ) {
        checkRegistrationOpen()
        require(frames.isNotEmpty()) {
            "An animation must contain at least one frame path."
        }
        require(frames.all { it.isNotBlank() }) {
            "Animation frame paths must not be blank."
        }

        registerSource(
            type = type,
            source = SpriteSource.AnimatedFiles(
                paths = frames.map(::resolvePath),
                frameDuration = frameDuration
            )
        )
    }

    /** Registers a looping terrain animation from a tight spritesheet. */
    fun registerAnimated(
        type: T,
        spriteSheet: String,
        frameWidth: Int,
        frameHeight: Int,
        frameDuration: Float,
        frameCount: Int? = null
    ) {
        checkRegistrationOpen()
        require(spriteSheet.isNotBlank()) {
            "Sprite sheet path must not be blank."
        }

        registerSource(
            type = type,
            source = SpriteSource.SpriteSheet(
                path = resolvePath(spriteSheet),
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                frameDuration = frameDuration,
                frameCount = frameCount
            )
        )
    }

    /** Resolves registered sprites after their textures have loaded. */
    internal fun prepare() {
        for (entry in registrations.values) {
            if (entry.isPrepared) continue

            entry.prepare(entry.source.prepare(regionFor, atlasFor))
        }
    }

    internal fun freeze() {
        registrationOpen = false
    }

    /**
     * Returns the maximum terrain sprite height in world units.
     *
     * An empty registry returns infinity to disable height-based culling.
     */
    fun maxSpriteHeight(tileWidth: Float): Float {
        require(tileWidth > 0f && tileWidth.isFinite()) {
            "Tile width must be finite and positive."
        }

        check(registrations.values.all { it.isPrepared }) {
            "Terrain sprites must be prepared before calculating their height."
        }

        return registrations.values.maxOfOrNull { entry ->
            val texture = entry.texture

            require(texture.regionWidth > 0) {
                "Terrain sprite width must be positive."
            }

            tileWidth * texture.regionHeight / texture.regionWidth
        } ?: Float.POSITIVE_INFINITY
    }

    /** Returns the static texture or first animation frame. */
    operator fun get(type: T): TextureRegion {
        return entry(type).texture
    }

    /** Resolves a terrain frame for the shared world-view animation time. */
    fun frameAt(type: T, stateTime: Float): TextureRegion {
        return entry(type).frameAt(stateTime)
    }

    private fun registerSource(
        type: T,
        source: SpriteSource
    ) {
        require(type !in registrations) {
            "Terrain type $type is already registered."
        }

        source.queue(queueTexture, queueAtlas)
        registrations[type] = TerrainEntry(type, source)
    }

    private fun entry(type: T): TerrainEntry<T> {
        return registrations[type]
            ?: error("Terrain type $type is not registered.")
    }

    private fun resolvePath(sprite: String): String {
        return if (baseDirectory.isEmpty()) {
            sprite
        } else {
            "$baseDirectory/$sprite"
        }
    }

    private fun checkRegistrationOpen() {
        check(registrationOpen) {
            "Terrain registry registration is already closed."
        }
    }
}
