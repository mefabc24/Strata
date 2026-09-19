package com.mefabc24.strata.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.mefabc24.strata.StrataEngine
import com.mefabc24.strata.StrataGame
import com.badlogic.gdx.graphics.Color

object DesktopLauncher {
    fun launch(
        game: StrataGame,
        title: String = "Strata Engine",
        width: Int = 1280,
        height: Int = 720,
        backgroundColor: Color = Color(0.1f, 0.1f, 0.1f, 1f)
    ) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle(title)
            setWindowedMode(width, height)
            useVsync(true)
            setForegroundFPS(60)
        }

        Lwjgl3Application(
            StrataEngine(
                game = game,
                backgroundColor = backgroundColor
            ),
            config
        )
    }
}