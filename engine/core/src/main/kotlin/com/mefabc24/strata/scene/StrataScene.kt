package com.mefabc24.strata.scene

import com.badlogic.gdx.utils.Disposable
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.audio.SoundRegistry
import com.mefabc24.strata.audio.StrataAudio
import com.mefabc24.strata.camera.CameraSettings
import com.mefabc24.strata.input.ControlsSettings
import com.mefabc24.strata.input.StrataInput
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.lighting.Lighting
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.placement.PlacementSettings
import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.strata.render.entity.EntityRegistry
import com.mefabc24.strata.render.RenderingSettings
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.StrataUiTheme
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.debug.DebugRuntime
import com.mefabc24.strata.debug.ui.DebugPanelSkin
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.event.EventBus

internal fun interface StrataUiFactory {
    fun create(
        skin: Skin,
        theme: StrataUiTheme
    ): StrataUi
}

/**
 * Coordinates assets and the optional world and UI layers of a scene.
 *
 * The scene owns its assets, audio, attached world view, and attached UI. The
 * game creates an optional [World] and attaches it; the world itself currently
 * has no disposal lifecycle. A scene can contain either runtime layer
 * independently or both together. Setup DSL values are frozen after the
 * constructor configuration block completes and are applied when their
 * runtime component is created.
 */
class StrataScene private constructor(
    terrainDirectory: String,
    objectDirectory: String,
    entityDirectory: String,
    private val simulation: SimulationController,
    configure: StrataScene.() -> Unit,
    private val uiFactory: StrataUiFactory,
    private val worldViewFactory: SceneWorldViewFactory
) : Disposable {

    constructor(
        terrainDirectory: String,
        objectDirectory: String,
        entityDirectory: String = objectDirectory,
        simulation: SimulationController = SimulationController(),
        configure: StrataScene.() -> Unit
    ) : this(
        terrainDirectory,
        objectDirectory,
        entityDirectory,
        simulation,
        configure,
        StrataUiFactory { skin, theme ->
            StrataUi(
                skin = skin,
                theme = theme
            )
        },
        DefaultSceneWorldViewFactory
    )

    internal constructor(
        terrainDirectory: String,
        objectDirectory: String,
        entityDirectory: String = objectDirectory,
        simulation: SimulationController = SimulationController(),
        uiFactory: StrataUiFactory,
        worldViewFactory: SceneWorldViewFactory = DefaultSceneWorldViewFactory,
        configure: StrataScene.() -> Unit
    ) : this(
        terrainDirectory,
        objectDirectory,
        entityDirectory,
        simulation,
        configure,
        uiFactory,
        worldViewFactory
    )

    val assets = StrataAssets()

    val terrain = TerrainRegistry(
        directory = terrainDirectory,
        assets = assets
    )

    val objects = ObjectRegistry(
        directory = objectDirectory,
        assets = assets
    )

    val entities = EntityRegistry(
        directory = entityDirectory,
        assets = assets
    )

    val sounds = SoundRegistry(assets)

    private val sceneRegistrations = SceneRegistrations(
        terrainRegistry = terrain,
        objectRegistry = objects,
        entityRegistry = entities,
        soundRegistry = sounds
    )

    val audio = StrataAudio(
        assets = assets,
        sounds = sounds
    )

    /**
     * Provides access to scene debugging facilities.
     */
    val debug = DebugSettings()

    /** Scene-owned synchronous event bus for game and diagnostic events. */
    val events = EventBus()

    /** Scene-owned runtime ambient and point lighting. */
    val lighting = Lighting()

    private val cameraSettings = CameraSettings()
    private val renderingSettings = RenderingSettings()
    private val controlsSettings = ControlsSettings()
    private var placementSettings: PlacementSettings? = null

    private val cameraSnapshot: CameraSettings
    private val renderingSnapshot: RenderingSettings
    private val controlsSnapshot: ControlsSettings

    private var configurationOpen = true

    private var attachedWorld: World? = null
    private var attachedView: SceneWorldView? = null
    private var attachedPlacement: PlacementController? = null
    private var attachedUi: StrataUi? = null
    private var attachedDebug: DebugRuntime? = null

    private val sceneInput = StrataInput()
    private var inputInstalled = false

    private var disposed = false

    /**
     * Returns the attached world view.
     */
    val view: IsoWorldView
        get() = attachedView?.publicView
            ?: error("No world view is attached to this scene.")

    /**
     * Returns the game-created world attached to this scene.
     */
    val world: World
        get() = attachedWorld
            ?: error("No world is attached to this scene.")

    /**
     * Returns the scene-owned placement controller.
     *
     * Placement exists only when it was configured during scene setup and a
     * world has subsequently been attached.
     */
    val placement: PlacementController
        get() = attachedPlacement
            ?: error("No placement controller is attached to this scene.")

    /**
     * Runtime input routing for the optional UI and world layers.
     */
    val input: StrataInput
        get() = sceneInput

    /**
     * Returns the attached UI layer.
     */
    val ui: StrataUi
        get() = attachedUi
            ?: error("No UI is attached to this scene.")

    init {
        try {
            configure(this)
            configurationOpen = false

            terrain.freeze()
            objects.freeze()
            entities.freeze()
            sounds.freeze()

            cameraSnapshot = cameraSettings.copy()
            renderingSnapshot = renderingSettings.copy()
            controlsSnapshot = controlsSettings.copy()
            placementSettings = placementSettings?.copy()

            cameraSnapshot.validate()
            renderingSnapshot.validate()

            assets.finishLoading()

            terrain.prepare()
            objects.prepare()
            entities.prepare()
        } catch (failure: Throwable) {
            try {
                dispose()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }

            throw failure
        }
    }

    /**
     * Configures scene-owned content registrations.
     */
    fun registrations(
        configure: SceneRegistrations.() -> Unit
    ) {
        checkConfigurationOpen()
        sceneRegistrations.apply(configure)
    }

    /** Configures runtime audio during scene setup. */
    fun audio(configure: StrataAudio.() -> Unit) {
        checkConfigurationOpen()
        audio.apply(configure)
    }

    /** Configures debugging facilities during scene setup. */
    fun debug(configure: DebugSettings.() -> Unit) {
        checkConfigurationOpen()
        debug.apply(configure)
    }

    /** Configures scene lighting while retaining runtime mutability. */
    fun lighting(configure: Lighting.() -> Unit) {
        checkConfigurationOpen()
        lighting.apply(configure)
    }

    /**
     * Configures the camera snapshot applied when a world is attached.
     */
    fun camera(configure: CameraSettings.() -> Unit) {
        checkConfigurationOpen()
        cameraSettings.apply(configure)
    }

    /**
     * Configures the rendering snapshot applied when a world is attached.
     */
    fun rendering(configure: RenderingSettings.() -> Unit) {
        checkConfigurationOpen()
        renderingSettings.apply(configure)
    }

    /**
     * Configures the controls snapshot applied when a world is attached.
     */
    fun controls(configure: ControlsSettings.() -> Unit) {
        checkConfigurationOpen()
        controlsSettings.apply(configure)
    }

    /**
     * Enables and configures scene-owned object placement.
     *
     * The controller is created when a world is attached.
     */
    fun placement(configure: PlacementSettings.() -> Unit = {}) {
        checkConfigurationOpen()

        val settings = placementSettings
            ?: PlacementSettings().also {
                placementSettings = it
            }

        settings.apply(configure)
    }

    /**
     * Attaches a game-created world and creates its isometric view.
     *
     * The frozen camera, rendering, control, and optional placement settings
     * are applied to the new runtime components. Terrain and object visuals
     * are resolved through this scene's registries. A world can be attached
     * only once.
     */
    fun attachWorld(
        world: World,
        terrainFor: (Tile) -> TerrainId
    ) {
        checkActive()
        checkConfigurationComplete()

        check(attachedView == null) {
            "A world is already attached to this scene."
        }

        val renderingSnapshot = this.renderingSnapshot.copy().apply {
            validate()

            if (maxTerrainSpriteHeight == null) {
                maxTerrainSpriteHeight = terrain.maxSpriteHeight(tileGeometry.width)
            }
        }

        val view = worldViewFactory.create(
            SceneWorldViewSpec(
                world = world,
                textureFor = { tile, animationTime ->
                    terrain.frameAt(terrainFor(tile), tile, animationTime)
                },
                objectVisualFor = objects::get,
                entityVisualFor = entities::get,
                resolvedObjectVisualFor = objects::resolve,
                resolvedEntityVisualFor = entities::resolve,
                resolvedRepresentativeEntityVisualFor = entities::resolveRepresentative,
                cameraSettings = cameraSnapshot.copy(),
                controlsSettings = controlsSnapshot.copy(),
                renderingSettings = renderingSnapshot,
                lighting = lighting,
                debugGridSettings = debug.grid,
                debugObjectSettings = debug.objects,
                debugEntitySettings = debug.entities,
                debugSettings = debug
            )
        )

        val placement = placementSettings?.createController(world)

        attachView(
            world = world,
            view = view,
            placement = placement
        )

        if (debug.panel.enabled) {
            attachDebugRuntime(
                world = world,
                view = checkNotNull(view.publicView) {
                    "The built-in debug panel requires an IsoWorldView."
                },
                terrainFor = terrainFor
            )
        }
    }

    private fun attachDebugRuntime(
        world: World,
        view: IsoWorldView,
        terrainFor: (Tile) -> TerrainId
    ) {
        check(attachedDebug == null) { "A debug runtime is already attached." }
        val skin = DebugPanelSkin.create()
        val ui = try {
            uiFactory.create(skin, DebugPanelSkin.theme())
        } catch (failure: Throwable) {
            skin.dispose()
            throw failure
        }
        val runtime = try {
            DebugRuntime(
                settings = debug,
                ui = ui,
                skin = skin,
                world = world,
                view = view,
                terrain = terrain,
                objects = objects,
                entities = entities,
                placement = attachedPlacement,
                simulation = simulation,
                events = events,
                terrainFor = terrainFor
            )
        } catch (failure: Throwable) {
            try { ui.dispose() } finally { skin.dispose() }
            throw failure
        }

        try {
            sceneInput.addDebugUiProcessor(runtime.uiInputProcessor)
            sceneInput.setDebugWorldProcessor(runtime.worldInputProcessor)
            installInputIfNeeded()
        } catch (failure: Throwable) {
            sceneInput.removeDebugUiProcessor(runtime.uiInputProcessor)
            sceneInput.removeDebugWorldProcessor(runtime.worldInputProcessor)
            runtime.dispose()
            throw failure
        }
        attachedDebug = runtime
    }

    /**
     * Creates and attaches a UI layer.
     *
     * The scene owns the UI and its stage. The supplied skin and all resources
     * in it remain owned by the caller. [theme] maps semantic UI roles to
     * styles in that skin.
     */
    fun createUi(
        skin: Skin,
        theme: StrataUiTheme = StrataUiTheme(),
        configure: StrataUi.() -> Unit = {}
    ): StrataUi {
        checkActive()
        checkConfigurationComplete()

        check(attachedUi == null) {
            "A UI layer is already attached to this scene."
        }

        val ui = uiFactory.create(
            skin = skin,
            theme = theme
        )

        try {
            ui.configure()

            sceneInput.addUiProcessor(
                ui.inputProcessor
            )

            try {
                installInputIfNeeded()
            } catch (failure: Throwable) {
                sceneInput.removeUiProcessor(ui.inputProcessor)
                throw failure
            }
        } catch (failure: Throwable) {
            try {
                ui.dispose()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }

            throw failure
        }

        attachedUi = ui

        return ui
    }

    /**
     * Attaches a world view and installs its input processor.
     *
     * A scene can own one world view.
     */
    private fun attachView(
        world: World,
        view: SceneWorldView,
        placement: PlacementController?
    ) {
        checkActive()

        check(attachedView == null) {
            "A world view is already attached to this scene."
        }

        var processorAttached = false

        try {
            sceneInput.setWorldProcessor(view.inputProcessor)
            processorAttached = true
            installInputIfNeeded()
        } catch (failure: Throwable) {
            if (processorAttached) {
                sceneInput.removeWorldProcessor(view.inputProcessor)
            }

            try {
                view.dispose()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }

            throw failure
        }

        attachedWorld = world
        attachedView = view
        attachedPlacement = placement
    }

    private fun installInputIfNeeded() {
        if (inputInstalled) return

        sceneInput.install()
        inputInstalled = true
    }

    /**
     * Updates simulation systems with [simulationDelta] and real-time systems
     * with [realDelta].
     */
    fun update(
        realDelta: Float,
        simulationDelta: Float = realDelta
    ) {
        checkActive()
        checkConfigurationComplete()

        require(realDelta.isFinite() && realDelta >= 0f) {
            "Real frame delta must be finite and non-negative."
        }
        require(simulationDelta.isFinite() && simulationDelta >= 0f) {
            "Simulation delta must be finite and non-negative."
        }

        attachedWorld?.let { world ->
            debug.entityFreezeState.retain(world.getEntities())
            world.updateEntities(simulationDelta) { entity ->
                !debug.isEntityFrozen(entity)
            }
        }

        attachedView?.let { view ->
            view.update(
                realDelta = realDelta,
                simulationDelta = simulationDelta
            )
            attachedPlacement?.update(view.hoveredTile)
        }

        attachedUi?.update(realDelta)
        attachedDebug?.update(realDelta)
    }

    /**
     * Renders the optional world first, followed by the optional UI.
     */
    fun render() {
        checkActive()
        checkConfigurationComplete()

        attachedView?.let { view ->
            view.render(attachedPlacement?.previews.orEmpty())

            debug.performance.record(
                stats = view.renderStats,
                delta = Gdx.graphics.deltaTime
            )
        }

        attachedUi?.render()
        attachedDebug?.render()
    }

    /** Resizes every attached layer. */
    fun resize(
        width: Int,
        height: Int
    ) {
        checkActive()
        checkConfigurationComplete()

        attachedView?.resize(
            width,
            height
        )

        attachedUi?.resize(
            width,
            height
        )

        attachedDebug?.resize(width, height)
    }

    private fun checkActive() {
        check(!disposed) {
            "StrataScene has already been disposed."
        }
    }

    private fun checkConfigurationOpen() {
        check(configurationOpen) {
            "Scene setup configuration is already complete."
        }
    }

    private fun checkConfigurationComplete() {
        check(!configurationOpen) {
            "Scene runtime operations are unavailable during setup."
        }
    }

    /**
     * Releases resources in reverse ownership order.
     */
    override fun dispose() {
        if (disposed) return

        disposed = true

        try {
            sceneInput.uninstall()
        } finally {
            try {
                attachedDebug?.dispose()
            } finally {
                try {
                    attachedUi?.dispose()
                } finally {
                    try {
                        attachedView?.dispose()
                    } finally {
                        try {
                            audio.dispose()
                        } finally {
                            assets.dispose()
                        }
                    }
                }
            }
        }
    }
}
