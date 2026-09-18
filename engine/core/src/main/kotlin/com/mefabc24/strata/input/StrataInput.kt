package com.mefabc24.strata.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor

/**
 * Routes input to UI processors before the game world.
 */
class StrataInput(
    worldProcessor: InputProcessor
) {
    private val multiplexer = InputMultiplexer(worldProcessor)

    /**
     * Adds a UI processor with priority over world input.
     */
    fun addUiProcessor(processor: InputProcessor) {
        multiplexer.addProcessor(0, processor)
    }

    fun removeUiProcessor(processor: InputProcessor) {
        multiplexer.removeProcessor(processor)
    }

    fun install() {
        Gdx.input.inputProcessor = multiplexer
    }

    fun uninstall() {
        if (Gdx.input.inputProcessor === multiplexer) {
            Gdx.input.inputProcessor = null
        }
    }
}