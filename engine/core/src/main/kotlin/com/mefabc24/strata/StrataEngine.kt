package com.mefabc24.strata

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

class StrataEngine(
    private val game: StrataGame,
) : ApplicationAdapter() {

    override fun create() {
        Gdx.app.log("Strata", "Engine initialized")
        game.create()
    }

    override fun render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        game.update(Gdx.graphics.deltaTime)
        game.render()
    }

    override fun dispose() {
        game.dispose()
        Gdx.app.log("Strata", "Engine disposed")
    }

}