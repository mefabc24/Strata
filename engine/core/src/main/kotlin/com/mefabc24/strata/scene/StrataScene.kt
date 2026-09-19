package com.mefabc24.strata.scene

import com.badlogic.gdx.utils.Disposable
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.audio.SoundRegistry
import com.mefabc24.strata.audio.StrataAudio
import com.mefabc24.strata.input.StrataInput
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.render.ObjectRegistry
import com.mefabc24.strata.render.PlacementPreview
import com.mefabc24.strata.terrain.TerrainRegistry
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import com.badlogic.gdx.Gdx

/**
 * Coordinates asset loading and the lifecycle of a world view.
 *
 * The scene owns its assets, audio, and attached world view.
 */
class StrataScene<T : Enum<T>, C : Enum<C>>(
    terrainDirectory: String,
    objectDirectory: String,
    configure: StrataScene<T, C>.() -> Unit
) : Disposable {

    val assets = StrataAssets()

    val terrain = TerrainRegistry<T>(
        directory = terrainDirectory,
        assets = assets
    )

    val objects = ObjectRegistry(
        directory = objectDirectory,
        assets = assets
    )

    val sounds = SoundRegistry<C>(assets)

    val audio = StrataAudio(
        assets = assets,
        sounds = sounds
    )

    /**
     * Configures optional performance logging.
     */
    val performance = ScenePerformanceLogger()

    private var attachedView: IsoWorldView? = null
    private var attachedInput: StrataInput? = null

    private var disposed = false

    /**
     * Returns the attached world view.
     */
    val view: IsoWorldView
        get() = attachedView
            ?: error("No world view is attached to this scene.")

    /**
     * Provides access to input configuration, including UI processors.
     */
    val input: StrataInput
        get() = attachedInput
            ?: error("No world view is attached to this scene.")

    init {
        try {
            configure(this)

            assets.finishLoading()

            terrain.prepare()
            objects.prepare()
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
     * Creates, configures, and attaches an isometric world view.
     *
     * Terrain and object visuals are resolved through this scene's registries.
     */
    fun createView(
        world: World,
        terrainFor: (Tile) -> T,
        configure: IsoViewSettings.() -> Unit = {}
    ): IsoWorldView {
        checkActive()

        check(attachedView == null) {
            "A world view is already attached to this scene."
        }

        val settings = IsoViewSettings().apply(configure)

        val view = IsoWorldView(
            world = world,

            textureFor = { tile ->
                terrain[terrainFor(tile)]
            },

            objectVisualFor = objects::get,

            tileWidth = settings.tileWidth,
            tileHeight = settings.tileHeight,

            cameraSettings = settings.camera,

            bindings = settings.bindings,

            maxTerrainSpriteHeight =
                settings.maxTerrainSpriteHeight
                    ?: terrain.maxSpriteHeight(settings.tileWidth),

            cameraControls = settings.cameraControls
        )

        attachView(view)

        return view
    }

    /**
     * Attaches a world view and installs its input processor.
     *
     * A scene can own one world view.
     */
    fun attachView(view: IsoWorldView) {
        checkActive()

        check(attachedView == null) {
            "A world view is already attached to this scene."
        }

        val input = StrataInput(view.inputProcessor)

        try {
            input.install()
        } catch (failure: Throwable) {
            try {
                view.dispose()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }

            throw failure
        }

        attachedView = view
        attachedInput = input
    }

    /**
     * Updates the attached world view.
     */
    fun update(delta: Float) {
        checkActive()
        view.update(delta)
    }

    /**
     * Renders the world with optional rendering effects.
     */
    fun render(
        preview: PlacementPreview? = null,
        raisedTile: Pair<Int, Int>? = null,
        raiseOffsetY: Float = 0f
    ) {
        checkActive()

        view.render(
            raisedTile = raisedTile,
            raiseOffsetY = raiseOffsetY,
            preview = preview
        )

        performance.record(
            stats = view.renderStats,
            delta = Gdx.graphics.deltaTime
        )
    }

    /**
     * Resizes the attached world view if one exists.
     */
    fun resize(width: Int, height: Int) {
        checkActive()
        attachedView?.resize(width, height)
    }

    private fun checkActive() {
        check(!disposed) {
            "StrataScene has already been disposed."
        }
    }

    /**
     * Releases resources in reverse ownership order.
     */
    override fun dispose() {
        if (disposed) return

        disposed = true

        try {
            attachedInput?.uninstall()
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