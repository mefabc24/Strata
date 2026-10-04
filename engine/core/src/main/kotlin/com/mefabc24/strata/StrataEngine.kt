package com.mefabc24.strata

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

class StrataEngine(
    private val game: StrataGame
) : ApplicationAdapter() {

    internal val backgroundColor =
        game.engineSettings.backgroundColorSnapshot()

    override fun create() {
        Gdx.app.log("Strata", "Engine initialized")
        game.create()
    }

    override fun resize(width: Int, height: Int) {
        game.resize(width, height)
    }

    override fun render() {
        Gdx.gl.glClearColor(
            backgroundColor.r,
            backgroundColor.g,
            backgroundColor.b,
            backgroundColor.a
        )
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        game.update(Gdx.graphics.deltaTime)
        game.render()
    }

    override fun dispose() {
        game.dispose()
        Gdx.app.log("Strata", "Engine disposed")
    }
}
