package com.mefabc24.strata

interface StrataGame {
    /** Global engine settings captured before [create] is invoked. */
    val engineSettings: EngineSettings
        get() = EngineSettings()

    fun create()
    fun update(delta: Float)
    fun render()
    fun dispose()
    fun resize(width: Int, height: Int)
}
