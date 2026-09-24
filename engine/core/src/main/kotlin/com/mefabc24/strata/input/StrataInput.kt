package com.mefabc24.strata.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor

/**
 * Routes optional UI processors before an optional game-world processor.
 */
class StrataInput(
    worldProcessor: InputProcessor? = null
) {
    private val multiplexer = InputMultiplexer()
    private val uiProcessors = mutableListOf<InputProcessor>()
    private var worldProcessor: InputProcessor? = null

    init {
        if (worldProcessor != null) {
            setWorldProcessor(worldProcessor)
        }
    }

    internal val processor: InputProcessor
        get() = multiplexer

    /**
     * Adds a UI processor with priority over world input.
     *
     * The same processor cannot be registered more than once.
     */
    fun addUiProcessor(processor: InputProcessor) {
        check(uiProcessors.none { it === processor }) {
            "This UI input processor is already registered."
        }

        multiplexer.addProcessor(uiProcessors.size, processor)
        uiProcessors += processor
    }

    /**
     * Removes a previously registered UI processor.
     *
     * Returns true when the processor was registered.
     */
    fun removeUiProcessor(processor: InputProcessor): Boolean {
        val index = uiProcessors.indexOfFirst {
            it === processor
        }

        if (index < 0) return false

        uiProcessors.removeAt(index)
        multiplexer.removeProcessor(index)
        return true
    }

    /**
     * Adds the single world processor after all registered UI processors.
     */
    fun setWorldProcessor(processor: InputProcessor) {
        check(worldProcessor == null) {
            "A world input processor is already registered."
        }

        worldProcessor = processor
        multiplexer.addProcessor(processor)
    }

    /**
     * Removes [processor] when it is the currently registered world input.
     */
    fun removeWorldProcessor(processor: InputProcessor): Boolean {
        if (worldProcessor !== processor) return false

        multiplexer.removeProcessor(processor)
        worldProcessor = null
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
