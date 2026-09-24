package com.mefabc24.strata.terrain

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.render.TerrainCliffVisuals

/**
 * Describes one terrain registration.
 *
 * The texture is owned by the registry's asset manager. Accessing [texture]
 * requires the registry to have been prepared.
 */
class TerrainEntry<T : Enum<T>> internal constructor(
    val type: T,
    val spritePath: String,
    val leftCliffSpritePath: String?,
    val rightCliffSpritePath: String?
) {
    private var preparedTexture: TextureRegion? = null
    private var preparedCliffs: TerrainCliffVisuals? = null

    /** The loaded texture region used to render this terrain. */
    val texture: TextureRegion
        get() = preparedTexture
            ?: error("Terrain type $type is not prepared.")

    /** Whether [texture] is ready for use. */
    val isPrepared: Boolean
        get() = preparedTexture != null

    /** Prepared optional cliff faces used for additional elevation levels. */
    val cliffs: TerrainCliffVisuals?
        get() {
            check(isPrepared) {
                "Terrain type $type is not prepared."
            }

            return preparedCliffs
        }

    internal fun prepare(
        texture: TextureRegion,
        cliffs: TerrainCliffVisuals?
    ) {
        preparedTexture = texture
        preparedCliffs = cliffs
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
     * Registers a terrain type and queues its surface and optional cliff
     * textures. Registration order is retained by [entries].
     */
    fun register(
        type: T,
        sprite: String = "${type.name.lowercase()}.png",
        configure: TerrainRegistrationSettings.() -> Unit = {}
    ) {
        checkRegistrationOpen()

        require(type !in registrations) {
            "Terrain type $type is already registered."
        }

        require(sprite.isNotBlank()) {
            "Sprite path must not be blank."
        }

        val settings = TerrainRegistrationSettings().apply(configure)
        settings.validate()

        val path = resolvePath(sprite)
        val leftCliffPath = settings.cliffs.left?.let(::resolvePath)
        val rightCliffPath = settings.cliffs.right?.let(::resolvePath)

        queueTexture(path)
        leftCliffPath?.let(queueTexture)
        rightCliffPath?.let(queueTexture)

        registrations[type] = TerrainEntry(
            type = type,
            spritePath = path,
            leftCliffSpritePath = leftCliffPath,
            rightCliffSpritePath = rightCliffPath
        )
    }

    /**
     * Resolves registered sprites after their textures have loaded.
     */
    internal fun prepare() {
        for (entry in registrations.values) {
            if (entry.isPrepared) continue

            entry.prepare(
                texture = regionFor(entry.spritePath),
                cliffs = createCliffs(entry)
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

    /** Returns prepared optional cliff faces for a terrain type. */
    fun cliffs(type: T): TerrainCliffVisuals? {
        val entry = registrations[type]
            ?: error("Terrain type $type is not registered.")

        return entry.cliffs
    }

    private fun createCliffs(
        entry: TerrainEntry<T>
    ): TerrainCliffVisuals? {
        val left = entry.leftCliffSpritePath?.let(regionFor)
        val right = entry.rightCliffSpritePath?.let(regionFor)

        return if (left == null && right == null) {
            null
        } else {
            TerrainCliffVisuals(left = left, right = right)
        }
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
