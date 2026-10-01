package com.mefabc24.strata.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor

/**
 * Routes debug UI, game UI, debug tools, and game-world input in that order.
 */
class StrataInput(
    worldProcessor: InputProcessor? = null
) {
    private val debugUiProcessors = mutableListOf<InputProcessor>()
    private val screenUiProcessors = mutableListOf<InputProcessor>()
    private val uiProcessors = mutableListOf<InputProcessor>()
    private var debugWorldProcessor: InputProcessor? = null
    private var worldProcessor: InputProcessor? = null

    internal val processor: InputProcessor
        field = InputMultiplexer()

    init {
        if (worldProcessor != null) {
            setWorldProcessor(worldProcessor)
        }
    }

    /**
     * Adds a UI processor with priority over world input.
     *
     * The same processor cannot be registered more than once.
     */
    fun addUiProcessor(processor: InputProcessor) {
        check(uiProcessors.none { it === processor }) {
            "This UI input processor is already registered."
        }

        uiProcessors += processor
        rebuild()
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
        rebuild()
        return true
    }

    /**
     * Replaces the processors owned by the active screen stack.
     *
     * The list must already be ordered from the topmost screen down. It is
     * routed after the debug UI and before compatibility UI and world input.
     */
    internal fun setScreenUiProcessors(processors: List<InputProcessor>) {
        check(processors.distinctBy { System.identityHashCode(it) }.size == processors.size) {
            "A screen UI input processor was supplied more than once."
        }
        screenUiProcessors.clear()
        screenUiProcessors += processors
        rebuild()
    }

    /** Adds a debug UI processor at the highest input priority. */
    fun addDebugUiProcessor(processor: InputProcessor) {
        check(debugUiProcessors.none { it === processor }) {
            "This debug UI input processor is already registered."
        }
        debugUiProcessors += processor
        rebuild()
    }

    /** Removes a previously registered debug UI processor. */
    fun removeDebugUiProcessor(processor: InputProcessor): Boolean {
        val removed = debugUiProcessors.removeAll { it === processor }
        if (removed) rebuild()
        return removed
    }

    /** Installs the active debug world tool after UI and before game input. */
    fun setDebugWorldProcessor(processor: InputProcessor) {
        check(debugWorldProcessor == null) {
            "A debug world input processor is already registered."
        }
        debugWorldProcessor = processor
        rebuild()
    }

    fun removeDebugWorldProcessor(processor: InputProcessor): Boolean {
        if (debugWorldProcessor !== processor) return false
        debugWorldProcessor = null
        rebuild()
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
        rebuild()
    }

    /**
     * Removes [processor] when it is the currently registered world input.
     */
    fun removeWorldProcessor(processor: InputProcessor): Boolean {
        if (worldProcessor !== processor) return false

        worldProcessor = null
        rebuild()
        return true
    }

    /** Replaces the active world processor during a world switch. */
    internal fun replaceWorldProcessor(processor: InputProcessor?) {
        worldProcessor = processor
        rebuild()
    }

    private fun rebuild() {
        processor.clear()
        debugUiProcessors.forEach(processor::addProcessor)
        screenUiProcessors.forEach(processor::addProcessor)
        uiProcessors.forEach(processor::addProcessor)
        debugWorldProcessor?.let(processor::addProcessor)
        worldProcessor?.let(processor::addProcessor)
    }

    /** Installs this router as libGDX's active input processor. */
    fun install() {
        Gdx.input.inputProcessor = processor
    }

    /**
     * Clears libGDX's input processor only when this router is still active.
     */
    fun uninstall() {
        if (Gdx.input.inputProcessor === processor) {
            Gdx.input.inputProcessor = null
        }
    }
}
