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
import com.mefabc24.strata.world.TilePosition

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
     * Provides access to scene debugging facilities.
     */
    val debug = DebugSettings()

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
     * Provides access to runtime input routing, including UI processors.
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
        configure: SceneSettings<C>.() -> Unit = {}
    ): IsoWorldView {
        checkActive()

        check(attachedView == null) {
            "A world view is already attached to this scene."
        }

        val settings = SceneSettings(
            sceneAudio = audio,
            sceneDebug = debug
        ).apply(configure)

        val renderingSettings = settings.rendering.copy().apply {
            validate()

            if (maxTerrainSpriteHeight == null) {
                maxTerrainSpriteHeight = terrain.maxSpriteHeight(tileGeometry.width)
            }
        }

        val view = IsoWorldView(
            world = world,

            textureFor = { tile ->
                terrain[terrainFor(tile)]
            },

            objectVisualFor = objects::get,

            cameraSettings = settings.camera,
            controls = settings.controls,
            renderingSettings = renderingSettings
        )

        attachView(view)

        return view
    }

    /**
     * Attaches a world view and installs its input processor.
     *
     * A scene can own one world view.
     */
    private fun attachView(view: IsoWorldView) {
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
        raisedTile: TilePosition? = null,
        raiseOffsetY: Float = 0f
    ) {
        checkActive()

        view.render(
            raisedTile = raisedTile,
            raiseOffsetY = raiseOffsetY,
            preview = preview
        )

        debug.performance.record(
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