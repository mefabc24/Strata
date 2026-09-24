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
    private val uiProcessors = mutableSetOf<InputProcessor>()

    internal val processor: InputProcessor
        get() = multiplexer

    /**
     * Adds a UI processor with priority over world input.
     *
     * The same processor cannot be registered more than once.
     */
    fun addUiProcessor(processor: InputProcessor) {
        check(uiProcessors.add(processor)) {
            "This UI input processor is already registered."
        }

        multiplexer.addProcessor(0, processor)
    }

    /**
     * Removes a previously registered UI processor.
     *
     * Returns true when the processor was registered.
     */
    fun removeUiProcessor(processor: InputProcessor): Boolean {
        if (!uiProcessors.remove(processor)) return false

        multiplexer.removeProcessor(processor)
        return true
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
