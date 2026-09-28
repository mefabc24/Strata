package com.mefabc24.strata

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Disposable
import com.mefabc24.strata.audio.SoundRegistry
import com.mefabc24.strata.audio.StrataAudio
import com.mefabc24.strata.input.StrataInput
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.lighting.Lighting
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.scene.DebugSettings
import com.mefabc24.strata.scene.StrataScene
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.StrataUiTheme
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World

/**
 * Main facade for configuring and operating a Strata runtime.
 *
 * Strata owns one scene runtime while keeping game-owned world state outside
 * the engine configuration.
 */
@Suppress("unused")
class Strata : Disposable {

    /** Process-wide engine settings. */
    val engineSettings = EngineSettings()

    /** Runtime control over simulation pause and time scaling. */
    val simulation = SimulationController()

    private var sceneSpec: SceneSpec? = null
    private var activeScene: StrataScene? = null

    private var configured = false
    private var disposed = false

    /** Active scene runtime. */
    val scene: StrataScene
        get() = activeScene
            ?: error("Strata has not been created yet.")

    val terrain: TerrainRegistry
        get() = scene.terrain

    val objects: ObjectRegistry
        get() = scene.objects

    val entities: EntityRegistry
        get() = scene.entities

    val sounds: SoundRegistry
        get() = scene.sounds

    val audio: StrataAudio
        get() = scene.audio

    val debug: DebugSettings
        get() = scene.debug

    /** Runtime ambient and point lighting for the active scene. */
    val lighting: Lighting
        get() = scene.lighting

    val world: World
        get() = scene.world

    val view: IsoWorldView
        get() = scene.view

    val placement: PlacementController
        get() = scene.placement

    val input: StrataInput
        get() = scene.input

    val ui: StrataUi
        get() = scene.ui

    /**
     * Configures this Strata runtime.
     *
     * Configuration is intentionally one-shot and must complete before
     * [create] is called.
     */
    fun configure(
        configure: StrataConfiguration.() -> Unit
    ): Strata {
        check(!configured) {
            "Strata has already been configured."
        }

        check(activeScene == null) {
            "Strata cannot be configured after creation."
        }

        check(!disposed) {
            "Strata has already been disposed."
        }

        val configuration =
            StrataConfiguration(
                engineSettings = engineSettings
            ).apply(configure)

        sceneSpec = checkNotNull(configuration.sceneSpec) {
            "Strata configuration must define a scene."
        }

        configured = true

        return this
    }

    /** Creates the configured scene runtime. */
    fun create() {
        check(!disposed) {
            "Strata has already been disposed."
        }

        check(configured) {
            "Strata must be configured before creation."
        }

        check(activeScene == null) {
            "Strata has already been created."
        }

        val spec = checkNotNull(sceneSpec)

        activeScene = StrataScene(
            terrainDirectory = spec.terrainDirectory,
            objectDirectory = spec.objectDirectory,
            entityDirectory = spec.entityDirectory,
            configure = spec.configure
        )
    }

    /**
     * Attaches game-owned world state to the active scene.
     */
    fun attachWorld(
        world: World,
        terrainFor: (Tile) -> TerrainId
    ) {
        scene.attachWorld(
            world = world,
            terrainFor = terrainFor
        )
    }

    /**
     * Creates and attaches the scene UI.
     */
    fun createUi(
        skin: Skin,
        theme: StrataUiTheme = StrataUiTheme(),
        configure: StrataUi.() -> Unit = {}
    ): StrataUi {
        return scene.createUi(
            skin = skin,
            theme = theme,
            configure = configure
        )
    }

    /**
     * Updates real-time and simulation systems and returns the simulation delta.
     */
    fun update(realDelta: Float): Float {
        val simulationDelta = simulation.simulationDelta(realDelta)
        scene.update(
            realDelta = realDelta,
            simulationDelta = simulationDelta
        )
        return simulationDelta
    }

    fun render() {
        scene.render()
    }

    fun resize(
        width: Int,
        height: Int
    ) {
        activeScene?.resize(width, height)
    }

    override fun dispose() {
        if (disposed) return

        disposed = true

        try {
            activeScene?.dispose()
        } finally {
            activeScene = null
        }
    }
}

/**
 * One-shot configuration DSL for [Strata].
 */
class StrataConfiguration internal constructor(
    private val engineSettings: EngineSettings
) {

    internal var sceneSpec: SceneSpec? = null
        private set

    /** Configures process-wide engine settings. */
    fun engine(
        configure: EngineSettings.() -> Unit
    ) {
        engineSettings.apply(configure)
    }

    /**
     * Defines the scene owned by this Strata runtime.
     */
    fun scene(
        terrainDirectory: String,
        objectDirectory: String,
        entityDirectory: String = objectDirectory,
        configure: StrataScene.() -> Unit
    ) {
        check(sceneSpec == null) {
            "A Strata scene has already been configured."
        }

        sceneSpec = SceneSpec(
            terrainDirectory = terrainDirectory,
            objectDirectory = objectDirectory,
            entityDirectory = entityDirectory,
            configure = configure
        )
    }
}

internal data class SceneSpec(
    val terrainDirectory: String,
    val objectDirectory: String,
    val entityDirectory: String,
    val configure: StrataScene.() -> Unit
)
