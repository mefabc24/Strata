package com.mefabc24.strata.ui

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ScreenViewport

/**
 * Provides the root UI layer for a Strata scene.
 *
 * The supplied skin is owned by the caller and is not disposed by this class.
 */
class StrataUi(
    val skin: Skin
) : Disposable {

    val stage = Stage(
        ScreenViewport()
    )

    /**
     * Root layout container for game-specific UI.
     */
    val root = Table(skin).apply {
        setFillParent(true)
    }

    /**
     * Input processor used by the UI stage.
     */
    val inputProcessor: InputProcessor
        get() = stage

    init {
        stage.addActor(root)
    }

    /**
     * Updates UI actions and actors.
     */
    fun update(delta: Float) {
        stage.act(delta)
    }

    /**
     * Renders the UI.
     */
    fun render() {
        stage.draw()
    }

    /**
     * Updates the UI viewport.
     */
    fun resize(
        width: Int,
        height: Int
    ) {
        if (width <= 0 || height <= 0) return

        stage.viewport.update(
            width,
            height,
            true
        )
    }

    /**
     * Releases resources owned by the UI layer.
     *
     * The supplied skin remains owned by the caller.
     */
    override fun dispose() {
        stage.dispose()
    }
}