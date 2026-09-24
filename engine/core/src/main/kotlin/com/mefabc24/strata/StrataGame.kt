package com.mefabc24.strata

interface StrataGame {
    /**
     * Stable global settings for this game instance.
     *
     * Implementations must return the same settings object on every access.
     * [StrataEngine] captures its values when the engine is constructed.
     */
    val engineSettings: EngineSettings

    fun create()
    fun update(delta: Float)
    fun render()
    fun dispose()
    fun resize(width: Int, height: Int)
}
