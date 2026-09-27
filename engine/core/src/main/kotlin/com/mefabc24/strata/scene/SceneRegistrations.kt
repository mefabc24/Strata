package com.mefabc24.strata.scene

import com.mefabc24.strata.audio.SoundRegistry
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.terrain.TerrainRegistry

/**
 * Groups scene-owned content registrations during scene setup.
 */
class SceneRegistrations internal constructor(
    private val terrainRegistry: TerrainRegistry,
    private val objectRegistry: ObjectRegistry,
    private val entityRegistry: EntityRegistry,
    private val soundRegistry: SoundRegistry
) {

    /** Configures terrain registrations. */
    fun terrain(
        configure: TerrainRegistry.() -> Unit
    ) {
        terrainRegistry.apply(configure)
    }

    /** Configures object registrations. */
    fun objects(
        configure: ObjectRegistry.() -> Unit
    ) {
        objectRegistry.apply(configure)
    }

    /** Configures entity registrations. */
    fun entities(
        configure: EntityRegistry.() -> Unit
    ) {
        entityRegistry.apply(configure)
    }

    /** Configures sound registrations. */
    fun sounds(
        configure: SoundRegistry.() -> Unit
    ) {
        soundRegistry.apply(configure)
    }
}