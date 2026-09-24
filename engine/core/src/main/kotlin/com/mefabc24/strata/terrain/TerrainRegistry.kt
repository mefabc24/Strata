package com.mefabc24.strata.terrain

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.assets.StrataAssets

/**
 * Describes one terrain registration.
 *
 * The texture is owned by the registry's asset manager. Accessing [texture]
 * requires the registry to have been prepared.
 */
class TerrainEntry<T : Enum<T>> internal constructor(
    val type: T,
    val spritePath: String
) {
    private var preparedTexture: TextureRegion? = null

    /** The loaded texture region used to render this terrain. */
    val texture: TextureRegion
        get() = preparedTexture
            ?: error("Terrain type $type is not prepared.")

    /** Whether [texture] is ready for use. */
    val isPrepared: Boolean
        get() = preparedTexture != null

    internal fun prepare(texture: TextureRegion) {
        preparedTexture = texture
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
    private val regionFor: (String) -> TextureRegion
) {
    constructor(
        directory: String,
        assets: StrataAssets
    ) : this(
        directory = directory,
        queueTexture = assets::queueTexture,
        regionFor = assets::region
    )

    private val baseDirectory = directory.trimEnd('/')

    private val registrations = linkedMapOf<T, TerrainEntry<T>>()
    private var registrationOpen = true

    /**
     * A snapshot of registered terrain entries in registration order.
     *
     * Mutating the returned list cannot change this registry.
     */
    val entries: List<TerrainEntry<T>>
        get() = registrations.values.toList()

    /**
     * Registers a terrain type and queues its texture.
     */
    fun register(
        type: T,
        sprite: String = "${type.name.lowercase()}.png"
    ) {
        checkRegistrationOpen()

        require(type !in registrations) {
            "Terrain type $type is already registered."
        }

        require(sprite.isNotBlank()) {
            "Sprite path must not be blank."
        }

        val path = if (baseDirectory.isEmpty()) {
            sprite
        } else {
            "$baseDirectory/$sprite"
        }

        queueTexture(path)

        registrations[type] = TerrainEntry(
            type = type,
            spritePath = path
        )
    }

    /**
     * Resolves registered sprites after their textures have loaded.
     */
    internal fun prepare() {
        for (entry in registrations.values) {
            if (entry.isPrepared) continue

            entry.prepare(
                regionFor(entry.spritePath)
            )
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
            val region = entry.texture

            require(region.regionWidth > 0) {
                "Terrain sprite width must be positive."
            }

            tileWidth * region.regionHeight / region.regionWidth
        } ?: Float.POSITIVE_INFINITY
    }

    /**
     * Returns the prepared sprite for a terrain type.
     */
    operator fun get(type: T): TextureRegion {
        return registrations[type]?.texture
            ?: error("Terrain type $type is not registered.")
    }

    private fun checkRegistrationOpen() {
        check(registrationOpen) {
            "Terrain registry registration is already closed."
        }
    }
}
