package com.mefabc24.strata.audio

import com.mefabc24.strata.assets.StrataAssets

/**
 * Describes a registered sound.
 */
data class SoundDefinition(
    val path: String,
    val category: SoundCategoryId
)

/**
 * Registers game-defined sound IDs and their audio settings.
 *
 * Sound assets are queued automatically during scene setup registration.
 * Definitions remain readable after the owning scene closes registration.
 */
class SoundRegistry internal constructor(
    private val queueSound: (String) -> Unit
) {

    constructor(assets: StrataAssets) : this(assets::queueSound)

    private val definitions =
        mutableMapOf<SoundId, SoundDefinition>()

    private var registrationOpen = true

    /**
     * Registers a sound and queues its asset for loading.
     */
    fun register(
        id: SoundId,
        path: String,
        category: SoundCategoryId
    ) {
        checkRegistrationOpen()

        require(id !in definitions) {
            "Sound is already registered: $id"
        }

        require(path.isNotBlank()) {
            "Sound path must not be blank."
        }

        queueSound(path)

        definitions[id] = SoundDefinition(
            path = path,
            category = category
        )
    }

    internal fun freeze() {
        registrationOpen = false
    }

    /**
     * Returns the definition of a registered sound.
     */
    operator fun get(id: SoundId): SoundDefinition {
        return definitions[id]
            ?: error("Sound is not registered: $id")
    }

    private fun checkRegistrationOpen() {
        check(registrationOpen) {
            "Sound registry registration is already closed."
        }
    }
}