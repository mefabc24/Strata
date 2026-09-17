package com.mefabc24.strata.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.mefabc24.strata.StrataEngine
import com.mefabc24.strata.StrataGame

object DesktopLauncher {
    fun launch(
        game: StrataGame,
        title: String = "Strata Engine",
        width: Int = 1280,
        height: Int = 720
    ) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle(title)
            setWindowedMode(width, height)
            useVsync(true)
            setForegroundFPS(60)
        }

        Lwjgl3Application(StrataEngine(game), config)
    }
}