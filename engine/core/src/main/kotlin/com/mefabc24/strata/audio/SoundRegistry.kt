package com.mefabc24.strata.audio

import com.mefabc24.strata.assets.StrataAssets

/**
 * Describes a registered sound.
 */
data class SoundDefinition<C : Enum<C>>(
    val path: String,
    val category: C
)

/**
 * Registers game-defined sound IDs and their audio settings.
 *
 * Sound assets are queued automatically during registration.
 */
class SoundRegistry<C : Enum<C>>(
    private val assets: StrataAssets
) {

    private val definitions = mutableMapOf<SoundId, SoundDefinition<C>>()

    /**
     * Registers a sound and queues its asset for loading.
     */
    fun register(
        id: SoundId,
        path: String,
        category: C
    ) {
        require(id !in definitions) {
            "Sound is already registered: $id"
        }

        require(path.isNotBlank()) {
            "Sound path must not be blank."
        }

        assets.queueSound(path)

        definitions[id] = SoundDefinition(
            path = path,
            category = category
        )
    }

    /**
     * Returns the definition of a registered sound.
     */
    operator fun get(id: SoundId): SoundDefinition<C> {
        return definitions[id]
            ?: error("Sound is not registered: $id")
    }
}